package io.github.voperad.sfevents.tasks

import com.cronutils.model.time.ExecutionTime
import io.github.voperad.sfevents.*
import io.github.voperad.sfevents.managers.EventManager
import io.github.voperad.sfevents.managers.SchedulerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import java.util.logging.Level

object EventSchedulerTask {

    val jobs = mutableListOf<Job>()
    val eventsExecutions = mutableMapOf<String, ZonedDateTime>()

    fun start() {
        for (scheduledEvent in SchedulerManager.events) {
            eventsExecutions[scheduledEvent.name] = ExecutionTime.forCron(scheduledEvent.frequency).nextExecution(ZonedDateTime.now()).get()
        }

        jobs.add(pluginInstance.launchAsync {
            debug("Launching EventSchedulerTask...")
            while (true) {
                execute()
                delay(1000L)
            }
        })
    }

    fun stop() {
        debug("Stopping EventSchedulerTask...")
        jobs.forEach { it.cancel() }
    }

    private fun execute() {
        debug("Executing EventSchedulerTask...")
        for (scheduledEvent in SchedulerManager.events) {
            debug("Checking scheduled event '${scheduledEvent.name}'")
            val now = ZonedDateTime.now()
            eventsExecutions[scheduledEvent.name]?.let { execution ->
                debug("Next execution for '${scheduledEvent.name}' is at ${execution.toString()}")
                if (now.isAfter(execution)) {
                    debug("Executing scheduled event '${scheduledEvent.name}' at ${now}.")
                    EventManager.currentEvent?.let {
                        log(Level.WARNING, "Event '${it.config.eventName}' is already running. Skipping execution for '${scheduledEvent.name}'.")
                    } ?: run {
                        EventManager.startEvent(scheduledEvent.name)
                        info("Starting scheduled event '${scheduledEvent.name}' at ${now}.")
                    }
                    eventsExecutions.remove(scheduledEvent.name)
                }
            } ?: run {
                debug("Storing next execution for '${scheduledEvent.name}'")
                ExecutionTime.forCron(scheduledEvent.frequency).nextExecution(now)?.let { nextExecution ->
                    eventsExecutions[scheduledEvent.name] = nextExecution.get()
                }
            }
        }
    }

}