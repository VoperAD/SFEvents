package io.github.voperad.sfevents.managers

import io.github.voperad.sfevents.debug
import io.github.voperad.sfevents.events.BaseEvent
import io.github.voperad.sfevents.events.CraftEvent
import io.github.voperad.sfevents.events.configs.BaseConfiguration
import io.github.voperad.sfevents.events.configs.CraftEventConfiguration
import kotlin.properties.Delegates

object EventManager {

    var currentEvent: BaseEvent<out BaseConfiguration>? by Delegates.observable(null) { prop, old, new ->
        debug("Property '${prop.name}' changed from '${old?.config?.eventName}' to '${new?.config?.eventName}'")
    }

    fun startEvent(eventName: String) {
        val baseConfiguration = EventsFilesManager.findEventConfigByName(eventName) ?: return

        val event = when (EventType.valueOf(baseConfiguration.eventType)) {
            EventType.CRAFT_EVENT -> CraftEvent(baseConfiguration as CraftEventConfiguration)
        }

        currentEvent = event
        event.announce()
    }

    fun cancelEvent() = currentEvent?.cancel()

}

enum class EventType(val className: String) {
    CRAFT_EVENT("CraftEventConfiguration"),
}