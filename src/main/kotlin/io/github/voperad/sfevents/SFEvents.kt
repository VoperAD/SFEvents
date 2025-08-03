package io.github.voperad.sfevents

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import io.github.seggan.sf4k.AbstractAddon
import io.github.voperad.sfevents.managers.CommandManager
import io.github.voperad.sfevents.managers.EventsFilesManager
import io.github.voperad.sfevents.managers.SchedulerManager
import io.github.voperad.sfevents.tasks.EventSchedulerTask
import io.github.voperad.sfevents.util.AutoUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import org.bstats.bukkit.Metrics
import org.bstats.charts.SimplePie
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level
import kotlin.coroutines.CoroutineContext

class SFEvents : AbstractAddon() {

    override suspend fun onEnableAsync() {
        instance = this

        config.options().copyDefaults(true)
        saveConfig()

        EventsFilesManager.loadEventsConfigurations()
        CommandManager.setup()
        SchedulerManager.loadEvents()
        AutoUpdater.initialize(file)

        startMetrics()
    }

    override suspend fun onDisableAsync() {
        EventSchedulerTask.stop()
    }

    override fun getJavaPlugin(): JavaPlugin = this

    override fun getBugTrackerURL(): String = "https://github.com/VoperAD/SFEvents/issues"

    private fun startMetrics() {
        val metrics = Metrics(this, 23263)

        metrics.addCustomChart(SimplePie("auto_update") {
            config.getBoolean("settings.auto-update", false).toString()
        })

        metrics.addCustomChart(SimplePie("events_count") {
            EventsFilesManager.configurations.size.toString()
        })
    }

}

fun log(level: Level, message: String) = pluginInstance.logger.log(level, message)

fun info(message: String) = log(Level.INFO, message)

fun debug(message: String) {
    if (pluginInstance.config.getBoolean("settings.debug", false)) {
        info("[DEBUG] $message")
    }
}

private var instance: SFEvents? = null

val pluginInstance: SFEvents
    get() = checkNotNull(instance) { "Plugin is not enabled" }

fun JavaPlugin.launchAsync(
    context: CoroutineContext = asyncDispatcher,
    block: suspend CoroutineScope.() -> Unit
): Job = pluginInstance.launch {
    withContext(context, block)
}

fun Int.secondsToTicks(): Long = this * 20.ticks
