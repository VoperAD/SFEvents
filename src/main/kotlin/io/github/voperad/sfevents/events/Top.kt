package io.github.voperad.sfevents.events

import org.bukkit.command.CommandSender

fun interface Top {

    fun sendEventTop(commandSender: CommandSender)

}