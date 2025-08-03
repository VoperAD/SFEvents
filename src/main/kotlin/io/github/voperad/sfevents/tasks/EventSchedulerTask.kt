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
    val eventsNextExecution = mutableMapOf<String, ZonedDateTime>()

    fun start() {
        for (event in SchedulerManager.events) {
            eventsNextExecution[event.name] = ExecutionTime.forCron(event.frequency).nextExecution(ZonedDateTime.now()).get()
        }

        jobs.add(pluginInstance.launchAsync {
            debug("Launching EventSchedulerTask...")
            while (true) {
                execute()
                delay(500L)
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
            eventsNextExecution.putIfAbsent(
                scheduledEvent.name,
                ExecutionTime.forCron(scheduledEvent.frequency).nextExecution(ZonedDateTime.now()).get()
            )

            debug("Checking scheduled event '${scheduledEvent.name}'")
            val now = ZonedDateTime.now().withSecond(0).withNano(0)

            val execution = eventsNextExecution[scheduledEvent.name]
                ?.withSecond(0)
                ?.withNano(0)

            debug("Next execution for '${scheduledEvent.name}' is at $execution")

            if (now.isEqual(execution)) {
                debug("Executing scheduled event '${scheduledEvent.name}' at $now.")
                EventManager.currentEvent?.let {
                    log(
                        Level.WARNING,
                        "Event '${it.config.eventName}' is already running. Skipping execution for '${scheduledEvent.name}'."
                    )
                } ?: run {
                    EventManager.startEvent(scheduledEvent.name)
                    eventsNextExecution.remove(scheduledEvent.name)
                    info("Starting scheduled event '${scheduledEvent.name}' at $now.")
                }
            }
        }
    }

}