package io.github.voperad.sfevents.events.configs

import io.github.voperad.sfevents.configs.ConfigHandler
import io.github.voperad.sfevents.configs.ConfigPath
import kotlinx.serialization.Transient
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import java.io.File
import kotlin.properties.Delegates

abstract class BaseConfiguration {

    @Transient
    lateinit var file: File

    @Transient
    lateinit var fileConfiguration: FileConfiguration

    @ConfigPath("name")
    lateinit var eventName: String

    lateinit var permission: String

    lateinit var eventType: String

    @ConfigPath("messages.announce")
    lateinit var announceMessages: List<String>

    @ConfigPath("messages.start")
    lateinit var startMessages: List<String>

    @ConfigPath("messages.finish")
    lateinit var finishMessages: List<String>

    @ConfigPath("messages.finish-no-winners")
    lateinit var finishNoWinnersMessages: List<String>
    
    @ConfigPath("messages.cancelled")
    lateinit var cancelledMessages: List<String>

    @ConfigPath("messages.not-enough-players")
    lateinit var notEnoughPlayersMessages: List<String>

    @ConfigPath("boss-bar.enabled")
    var bossBarEnabled by Delegates.notNull<Boolean>()

    @ConfigPath("boss-bar.title")
    lateinit var bossBarTitle: String

    @ConfigPath("boss-bar.color", BossBarHandler::class)
    lateinit var bossBarColor: BarColor

    @ConfigPath("boss-bar.style", BossBarHandler::class)
    lateinit var bossBarStyle: BarStyle

    var duration by Delegates.notNull<Int>()

    var minPlayers by Delegates.notNull<Int>()

    var announcements by Delegates.notNull<Int>()

    var announcementInterval by Delegates.notNull<Int>()

    fun hasPermission(player: Player) = permission.isBlank() || player.hasPermission(permission)

    object BossBarHandler : ConfigHandler {
        override fun process(path: String, config: FileConfiguration): Any? {
            if (path.endsWith("color")) {
                return BarColor.valueOf(config.getString(path)?.uppercase() ?: "WHITE")
            }

            if (path.endsWith("style")) {
                return BarStyle.valueOf(config.getString(path)?.uppercase() ?: "SOLID")
            }

            throw IllegalArgumentException("Unknown boss bar property: $path")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return (other as? BaseConfiguration)?.let {
            this.file.name.equals(it.file.name, ignoreCase = true)
        } == true
    }

}