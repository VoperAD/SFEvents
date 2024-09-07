package io.github.voperad.sfevents.events.configs

import io.github.voperad.sfevents.configs.ConfigHandler
import io.github.voperad.sfevents.configs.ConfigPath
import org.bukkit.configuration.file.FileConfiguration
import kotlin.properties.Delegates

typealias ItemToCraft = CraftEventConfiguration.ItemToCraft

class CraftEventConfiguration : BaseConfiguration() {

    @ConfigPath("commands-on-winner")
    lateinit var commandsOnWinner: List<String>

    @ConfigPath("sf-items-to-craft", ItemsConfigHandler::class)
    lateinit var itemsToCraft: List<ItemToCraft>

    var craftInOrder by Delegates.notNull<Boolean>()

    var randomOrder by Delegates.notNull<Boolean>()

    @ConfigPath("randomization.enabled")
    var randomizationEnabled by Delegates.notNull<Boolean>()

    @ConfigPath("randomization.items-to-pick")
    var itemsToPick by Delegates.notNull<Int>()

    @ConfigPath("messages.items-to-craft.header")
    lateinit var itemsToCraftHeader: List<String>

    @ConfigPath("messages.items-to-craft.header-in-order")
    lateinit var itemsToCraftHeaderInOrder: List<String>

    @ConfigPath("messages.items-to-craft.item-line-format")
    lateinit var itemLineFormat: List<String>

    data class ItemToCraft(val id: String, val amount: Int)

    object ItemsConfigHandler : ConfigHandler {
        override fun process(path: String, config: FileConfiguration): Any? {
            val section = config.getConfigurationSection(path) ?: return null
            val result = mutableListOf<ItemToCraft?>()

            section.getKeys(false).mapTo(result) {
                val id = section.getString("$it.id") ?: return@mapTo null
                val amount = section.getInt("$it.amount")
                ItemToCraft(id, amount)
            }

            return result.filterNotNull()
        }
    }

}