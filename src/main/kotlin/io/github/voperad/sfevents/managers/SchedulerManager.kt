package io.github.voperad.sfevents.managers

import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import io.github.voperad.sfevents.debug
import io.github.voperad.sfevents.info
import io.github.voperad.sfevents.log
import io.github.voperad.sfevents.pluginInstance
import io.github.voperad.sfevents.tasks.EventSchedulerTask
import java.util.logging.Level

object SchedulerManager {

    val cronParser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ))
    val events: MutableList<ScheduledEvent> = mutableListOf()

    fun loadEvents() {
        events.clear()
        EventSchedulerTask.stop()

        val scheduler = pluginInstance.config.getConfigurationSection("scheduler") ?: run {
            log(Level.WARNING, "Scheduler section not found in config.yml. Please check your configuration.")
            return
        }

        scheduler.getBoolean("enabled", false).takeIf { it } ?: run {
            info("Scheduler is not enabled in config.yml.")
            return
        }

        scheduler.getConfigurationSection("events")?.let { eventsSection ->
            eventsSection.getKeys(false).forEach { key ->
                debug("Processing event key: $key")
                val eventName = eventsSection.getString("$key.name") ?: return
                val eventFrequency = eventsSection.getString("$key.frequency") ?: return

                EventsFilesManager.findEventConfigByName(eventName) ?: run {
                    log(Level.WARNING, "Event configuration for '$eventName' not found. Please check your configuration.")
                    return@forEach
                }

                val frequency = try {
                    cronParser.parse(eventFrequency)
                } catch (e: Exception) {
                    log(Level.SEVERE, "Invalid cron expression '$eventFrequency' for event '$eventName'. Please check your configuration. Error: ${e.message}")
                    return@forEach
                }

                ScheduledEvent(
                    name = eventName,
                    frequency = frequency
                ).also { scheduledEvent ->
                    events.add(scheduledEvent)
                    info("Scheduled event '${scheduledEvent.name}' with frequency '${scheduledEvent.frequency.asString()}' loaded successfully.")
                }
            }

            EventSchedulerTask.start()
        } ?: run {
            log(Level.WARNING, "Events section not found in scheduler section.")
        }
    }

    data class ScheduledEvent(val name: String, val frequency: Cron) {}

}