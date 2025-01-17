package io.github.voperad.sfevents.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import io.github.voperad.sfevents.managers.EventManager
import io.github.voperad.sfevents.managers.EventType
import io.github.voperad.sfevents.managers.EventsFilesManager
import net.md_5.bungee.api.ChatColor
import org.bukkit.command.CommandSender

@CommandAlias("sfevents|sfev|sfe")
object SFEventsCommands: BaseCommand() {

    @HelpCommand
    fun onHelp(help: CommandHelp) {
        help.showHelp()
    }

    @Subcommand("reload")
    @CommandPermission("sfevents.admin.reload")
    @Description("Reloads all events files")
    fun reload(sender: CommandSender) {
        EventsFilesManager.loadEventsConfigurations()
    }

    @Subcommand("event create")
    @CommandCompletion("@eventtype @nothing")
    @CommandPermission("sfevents.admin.createevent")
    @Description("Creates a new event given an event type")
    fun createEvent(sender: CommandSender, eventType: EventType, @Single @Conditions("unique-file-name") fileName: String) {
        EventsFilesManager.createEvent(fileName, eventType)?.let {
            sender.sendMessage("${ChatColor.GREEN}Event \"$fileName\" created!")
        } ?: sender.sendMessage("${ChatColor.RED}Failed to create the event!")
    }

    @Subcommand("event start")
    @CommandCompletion("@events")
    @CommandPermission("sfevents.admin.startevent")
    @Conditions("not-happening")
    @Description("Starts the given event")
    fun startEvent(sender: CommandSender, @Single @Conditions("valid-event") name: String) {
        sender.sendMessage("${ChatColor.GREEN}Event $name started!")
        EventManager.startEvent(name)
    }

    @Subcommand("event cancel")
    @CommandPermission("sfevents.admin.cancelevent")
    @Conditions("happening")
    @Description("If there is an event running, cancels it")
    fun stopEvent(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GREEN}Cancelling event ${EventManager.currentEvent?.config?.file?.nameWithoutExtension}...")
        EventManager.cancelEvent()
    }

    @Subcommand("configurations")
    fun listConfigurations(sender: CommandSender) {
        EventsFilesManager.configurations.forEach {
            sender.sendMessage("${ChatColor.GREEN}${it.file.nameWithoutExtension}")
        }
    }

}