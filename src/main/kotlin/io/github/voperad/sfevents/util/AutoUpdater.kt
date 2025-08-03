package io.github.voperad.sfevents.util

import com.github.shynixn.mccoroutine.bukkit.launch
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import io.github.voperad.sfevents.info
import io.github.voperad.sfevents.log
import io.github.voperad.sfevents.pluginInstance
import org.apache.maven.artifact.versioning.ComparableVersion
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.logging.Level

object AutoUpdater {

    private const val LATEST_RELEASE = "https://api.github.com/repos/VoperAD/SFEvents/releases/latest"
    private const val RELEASES = "https://github.com/VoperAD/SFEvents/releases"

    private lateinit var pluginFile: File
    private lateinit var updateFolder: File
    private lateinit var userAgent: String

    fun initialize(pluginFile: File) {
        this.pluginFile = pluginFile
        this.updateFolder = pluginInstance.server.updateFolderFile
        this.userAgent = " (${pluginInstance.name}/${pluginInstance.description.version}, ServerVersion/${pluginInstance.server.version}, BukkitVersion/${pluginInstance.server.bukkitVersion})"
        pluginInstance.launch {
            run()
        }
    }

    fun run() {
        val latestRelease = getLatestRelease() ?: run {
            log(Level.SEVERE, "Failed to check for updates")
            return
        }

        if (isLatestVersionNewer(latestRelease.cleanVersion)) {
            if (isAutoUpdateEnabled()) {
                info("There is a new version of SlimeFrame available: ${latestRelease.tagName}")
                info("Downloading...")
                downloadUpdate(latestRelease.downloadUrl)
            } else {
                info("There is a new version of SlimeFrame available: ${latestRelease.tagName}")
                info("Download it here: $RELEASES")
            }
        } else {
            info("You are running the latest version")
        }
    }

    private fun isAutoUpdateEnabled() = pluginInstance.config.getBoolean("auto-update", false)

    private fun getLatestRelease(): Release? {
        return try {
            val url = URL(LATEST_RELEASE)
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.addRequestProperty("User-Agent", userAgent)
            val inputStream = urlConnection.inputStream
            val inputStreamReader = InputStreamReader(inputStream)

            val jsonParser = JsonParser()
            val jsonElement: JsonElement = jsonParser.parse(inputStreamReader)
            Release(jsonElement)
        } catch (e: Exception) {
            log(Level.SEVERE, "Failed to check for updates: ${e.message}")
            null
        }
    }

    private fun isLatestVersionNewer(latest: String): Boolean {
        val latestVersion = ComparableVersion(latest)
        val currentVersion = ComparableVersion(pluginInstance.description.version)
        return latestVersion.compareTo(currentVersion) > 0
    }

    private fun downloadUpdate(downloadUrl: String) {
        var inputStream: BufferedInputStream?
        var fileOutputStream: FileOutputStream? = null

        try {
            val url = URL(downloadUrl)
            inputStream = BufferedInputStream(url.openStream())
            fileOutputStream = FileOutputStream(File(updateFolder, pluginFile.name))

            val buffer = ByteArray(4096)
            var count: Int
            while (inputStream.read(buffer, 0, 4096).also { count = it } != -1) {
                fileOutputStream.write(buffer, 0, count)
            }
        } catch (e: Exception) {
            log(Level.SEVERE, "Failed to download update: ${e.message}")
        } finally {
            try {
                fileOutputStream?.close()
            } catch (e: IOException) {
                log(Level.SEVERE, "Failed to close file output stream: ${e.message}")
            }
        }
    }

    private data class Release(
        val tagName: String,
        val jarName: String
    ) {
        constructor(element: JsonElement) : this(
            tagName = element.asJsonObject["tag_name"].asString,
            jarName = element.asJsonObject["assets"].asJsonArray[0].asJsonObject["name"].asString
        )

        val downloadUrl: String
            get() = "https://github.com/VoperAD/SlimeFrame/releases/download/$tagName/$jarName"

        val cleanVersion: String
            get() = tagName.replace("v", "")
    }
}
