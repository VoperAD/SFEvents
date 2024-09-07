package io.github.voperad.sfevents.managers

import co.aikar.commands.ConditionFailedException
import co.aikar.commands.PaperCommandManager
import io.github.voperad.sfevents.commands.SFEventsCommands
import io.github.voperad.sfevents.pluginInstance
import net.md_5.bungee.api.ChatColor

object CommandManager : PaperCommandManager(pluginInstance) {

    fun setup() {
        enableUnstableAPI("help")

        registerCompletions()
        registerReplacements()
        registerConditions()
        registerCommand(SFEventsCommands)
    }

    private fun registerCompletions() {
        commandCompletions.registerAsyncCompletion("eventtype") {
            EventType.entries.map {
                it.name.lowercase().replace("_", "-")
            }
        }

        commandCompletions.registerAsyncCompletion("events") {
            EventsFilesManager.configurations.map { it.file.nameWithoutExtension }
        }
    }

    private fun registerConditions() {
        commandConditions.addCondition("not-happening") {
            EventManager.currentEvent?.let {
                throw ConditionFailedException("${ChatColor.RED}An event is already happening!")
            }
        }

        commandConditions.addCondition("happening") {
            EventManager.currentEvent ?: throw ConditionFailedException("${ChatColor.RED}No event is happening!")
        }

        commandConditions.addCondition(String::class.java, "valid-event") { _, _, event ->
            if (EventsFilesManager.configurations.none { it.file.nameWithoutExtension.equals(event, true) }) {
                throw ConditionFailedException("${ChatColor.RED}Event $event does not exist!")
            }
        }

        commandConditions.addCondition(String::class.java, "unique-file-name") { _, _, fileName ->
            EventsFilesManager.configurations
                .firstOrNull { it.file.nameWithoutExtension == fileName }
                ?.run { throw ConditionFailedException("${ChatColor.RED}There is already an event named \"${fileName}\"!") }
        }
    }

    private fun registerReplacements() {

    }

}