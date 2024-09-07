package io.github.voperad.sfevents.managers

import io.github.voperad.sfevents.configs.ConfigUtils
import io.github.voperad.sfevents.events.configs.BaseConfiguration
import io.github.voperad.sfevents.info
import io.github.voperad.sfevents.log
import io.github.voperad.sfevents.pluginInstance
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.util.logging.Level

object EventsFilesManager {

    val configurations = mutableSetOf<BaseConfiguration>()
    private val eventsFolder = File(pluginInstance.dataFolder, "events")

    fun loadEventsConfigurations() {
        configurations.clear()

        if (!createEventsFolder()) {
            return
        }

        eventsFolder.listFiles()?.filter { it.extension == "yml" || it.extension == "yaml"}?.forEach {
            info("Loading event configuration: ${it.name}")
            val yamlConfiguration = YamlConfiguration.loadConfiguration(it)

            ConfigUtils.instantiateConfiguration(yamlConfiguration, it)?.apply {
                file = it
                fileConfiguration = yamlConfiguration
                configurations.add(this)
            }
        }
    }

    fun createEvent(fileName: String, type: EventType): BaseConfiguration? {
        val eventFile = File(eventsFolder, "${fileName}.yml")

        if (!createEventsFolder() || !createEventFile(eventFile, type)) {
            return null
        }

        val fileConfig = YamlConfiguration.loadConfiguration(eventFile)
        return ConfigUtils.instantiateConfiguration(fileConfig, eventFile)?.apply {
            file = eventFile
            fileConfiguration = fileConfig
            configurations.add(this)
        }
    }

    fun findEventConfigByName(name: String): BaseConfiguration? {
        return configurations.find { it.file.nameWithoutExtension.equals(name, ignoreCase = true) } ?: run {
            log(Level.WARNING, "Event $name not found!")
            null
        }
    }

    private fun createEventFile(file: File, type: EventType): Boolean {
        val defaultFile = type.name.lowercase().replace("_", "-")
        val inputStream = this::class.java.getResourceAsStream("/default-events/${defaultFile}.yml")
            ?: run {
                log(Level.SEVERE, "Failed to load default event file $defaultFile")
                return false
            }

        val content = inputStream.bufferedReader().use { it.readText() }

        try {
            if (file.createNewFile()) {
                file.writeText(content)
                return true
            }

            log(Level.WARNING, "Event ${file.name} already exists")
            return false
        } catch (e: IOException) {
            log(Level.SEVERE, "Failed to create event file ${file.name}")
            return false
        }
    }

    private fun createEventsFolder(): Boolean {
        if (!eventsFolder.exists() && !eventsFolder.mkdirs()) {
            log(Level.SEVERE, "Failed to create events folder")
            return false
        }

        return true
    }

}