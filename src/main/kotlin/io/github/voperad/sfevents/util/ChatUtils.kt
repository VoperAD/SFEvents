package io.github.voperad.sfevents.util

import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit

object ChatUtils {

    fun broadcast(message: String, vararg replacements: Pair<String, String>) {
        var finalMessage = message
        for (replacement in replacements) {
            finalMessage = finalMessage.replace(replacement.first, replacement.second)
        }
        finalMessage = ChatColor.translateAlternateColorCodes('&', finalMessage)
        Bukkit.broadcastMessage(finalMessage)
    }

    fun broadcast(messages: Collection<String>, vararg replacements: Pair<String, String>) {
        messages.forEach { broadcast(it, *replacements) }
    }

}