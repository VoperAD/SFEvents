package io.github.voperad.sfevents.events

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import io.github.voperad.sfevents.events.configs.BaseConfiguration
import io.github.voperad.sfevents.managers.EventManager
import io.github.voperad.sfevents.pluginInstance
import io.github.voperad.sfevents.secondsToTicks
import io.github.voperad.sfevents.util.ChatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.boss.BossBar
import kotlin.coroutines.CoroutineContext

abstract class BaseEvent<T : BaseConfiguration>(val config: T) {

    protected val jobs = mutableListOf<Job>()
    protected var isActive = false
    protected var bossBar: BossBar? = null

    open fun announce() {
        isActive = true
        var counter = 0
        jobs.add(pluginInstance.launch {
            while (counter < config.announcements) {
                config.announceMessages.forEach {
                    ChatUtils.broadcast(it,
                        "@name" to config.eventName,
                        "@time" to "${(config.announcements - counter) * config.announcementInterval}"
                    )
                }
                counter++
                delay(config.announcementInterval.secondsToTicks())
            }

            val allowedPlayerCount = Bukkit.getOnlinePlayers().filter { config.hasPermission(it) }.size
            if (allowedPlayerCount >= config.minPlayers) {
                initBossBar()
                start()
            }

            ChatUtils.broadcast(config.notEnoughPlayersMessages,
                "@name" to config.eventName,
                "@online" to allowedPlayerCount.toString(),
                "@required" to config.minPlayers.toString()
            )

            EventManager.currentEvent = null
        })
    }

    abstract fun start()

    abstract fun cancel()

    abstract fun finish(cancelled: Boolean = false)

    abstract fun finishNoWinner()

    protected fun initBossBar() {
        if (!config.bossBarEnabled) {
            return
        }

        bossBar = Bukkit.createBossBar(
            config.bossBarTitle.replace("@name", config.eventName),
            config.bossBarColor,
            config.bossBarStyle,
        ).apply {
            isVisible = true
            progress = 1.0
            for (p in Bukkit.getOnlinePlayers()) {
                if (config.hasPermission(p)) {
                    this.addPlayer(p)
                }
            }
        }
    }

    protected fun launch(
        context: CoroutineContext = pluginInstance.minecraftDispatcher,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit) {
        val job = pluginInstance.launch(context, start, block)
        jobs.add(job)
        job.invokeOnCompletion { jobs.remove(job) }
    }

}