package io.github.voperad.sfevents.events

import org.bukkit.entity.Player

fun interface Progressable {

    fun sendPlayerProgress(playerName: Player)

}