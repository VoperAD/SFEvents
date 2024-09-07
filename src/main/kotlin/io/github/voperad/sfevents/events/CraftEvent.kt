package io.github.voperad.sfevents.events

import io.github.thebusybiscuit.slimefun4.api.events.MultiBlockCraftEvent
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.voperad.sfevents.*
import io.github.voperad.sfevents.events.configs.CraftEventConfiguration
import io.github.voperad.sfevents.events.configs.ItemToCraft
import io.github.voperad.sfevents.managers.EventManager
import io.github.voperad.sfevents.util.ChatUtils
import kotlinx.coroutines.delay
import me.clip.placeholderapi.PlaceholderAPI
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.logging.Level
import kotlin.math.roundToInt
import kotlin.random.Random

private typealias CEC = CraftEventConfiguration

class CraftEvent(config: CEC): BaseEvent<CEC>(config), Listener {

    private val playersProgress = mutableMapOf<UUID, MutableMap<String, Int>>()
    private var itemsToCraftInfo: List<String>
    private var itemsToCraft = config.itemsToCraft

    init {
        if (config.randomOrder) {
            itemsToCraft = itemsToCraft.shuffled()
        }

        if (config.randomizationEnabled) {
            setupRandomization()
        }

        itemsToCraftInfo = setupItemsToCraftInfo()
    }

    override fun start() {
        Bukkit.getPluginManager().registerEvents(this, pluginInstance)
        ChatUtils.broadcast(config.startMessages,
            "@name" to config.eventName,
            "@time" to config.duration.toString()
        )

        ChatUtils.broadcast(itemsToCraftInfo)

        launch {
            var remainingTime = config.duration.secondsToTicks()
            val initialTime = System.nanoTime()
            while (remainingTime > 0 && isActive) {
                remainingTime -= 1.secondsToTicks()
                bossBar?.progress = remainingTime.toDouble() / config.duration.secondsToTicks()
                debug("Remaining time: $remainingTime")
                delay(1.secondsToTicks())
            }
            val finishTime = System.nanoTime()
            debug("Event duration: ${(finishTime - initialTime) / 1_000_000_000.0} seconds")
            finishNoWinner()
        }
    }

    override fun cancel() {
        finish(true)
        ChatUtils.broadcast(config.cancelledMessages, "@name" to config.eventName)
    }

    override fun finish(cancelled: Boolean) {
        if (!isActive) {
            debug("Trying to finish event ${config.eventName} but it is not active")
            return
        }

        isActive = false
        jobs.forEach { it.cancel() }
        HandlerList.unregisterAll(this)
        EventManager.currentEvent = null
        bossBar?.removeAll()
    }

    override fun finishNoWinner() {
        finish()
        ChatUtils.broadcast(config.finishNoWinnersMessages, "@name" to config.eventName)
    }

    private fun setWinner(winner: Player) {
        finish()

        ChatUtils.broadcast(config.finishMessages,
            "@name" to config.eventName,
            "@winner" to winner.displayName
        )

        config.commandsOnWinner.forEach {
            val command = PlaceholderAPI.setPlaceholders(winner, it)
            debug("Executing: $command")
            val executed = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
            if (!executed) {
                log(Level.WARNING, "Could not execute command $command")
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        bossBar?.let {
            if (config.hasPermission(e.player)) {
                it.addPlayer(e.player)
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onSfItemCraft(e: MultiBlockCraftEvent) {
        val p = e.player

        isTargetItem(e.output)?.let {
            if (config.hasPermission(p)) {
                addProgress(p, it)
                if (isWinner(p)) {
                    setWinner(p)
                }
            }
        }

        playerInfo()
    }

    private fun setupRandomization() {
        val itemsSize = itemsToCraft.size
        val itemsToPick = config.itemsToPick

        config.itemsToPick = if (itemsToPick == 0) Random.nextInt(1, itemsSize) else minOf(itemsToPick, itemsSize)

        val shuffled = itemsToCraft.shuffled()
        itemsToCraft = shuffled.subList(0, config.itemsToPick)
    }

    private fun setupItemsToCraftInfo(): List<String> {
        val result = mutableListOf<String>()
        val header = if (config.craftInOrder) config.itemsToCraftHeaderInOrder else config.itemsToCraftHeader

        result.addAll(header.map { it -> it.replace("@name", config.eventName) })

        itemsToCraft.forEachIndexed { index, item ->
            result.addAll(
                config.itemLineFormat.map { line ->
                    line.replace("@index", (index + 1).toString())
                        .replace("@name", config.eventName)
                        .replace("@amount", item.amount.toString())
                        .replace("@item", SlimefunItem.getById(item.id)?.itemName ?: "ITEM NOT FOUND")
                }
            )
        }

        return result
    }

    // TODO: remove it later, this is only for debugging
    private fun playerInfo() {
        playersProgress.forEach { (uuid, map) ->
            info("Player ${Bukkit.getPlayer(uuid)?.name}")
            map.forEach {
                info("${it.key}: ${it.value}")
            }
            info("\n")
        }
    }

    private fun isTargetItem(item: ItemStack): Pair<SlimefunItem, ItemToCraft>? {
        return SlimefunItem.getByItem(item)?.let { sfItem ->
            itemsToCraft.firstOrNull { it.id == sfItem.id }?.let { itemToCraft ->
                sfItem to itemToCraft
            }
        }
    }

    private fun addProgress(player: Player, itemPair: Pair<SlimefunItem, ItemToCraft>) {
        initPlayerProgress(player)
        val itemToCraft = itemPair.second

        if (config.craftInOrder && !canProgress(player, itemToCraft.id)) {
            return
        }

        val progress = playersProgress[player.uniqueId] ?: return
        if (progress[itemToCraft.id]!! >= itemToCraft.amount) {
            player.sendMessage("${ChatColor.AQUA}You have already crafted ${itemToCraft.amount} ${itemPair.first.itemName}")
            return
        }

        progress.computeIfPresent(itemToCraft.id) { _, amount -> amount + 1 }
    }

    private fun initPlayerProgress(player: Player) {
        playersProgress.getOrPut(player.uniqueId) {
            itemsToCraft.associate { it.id to 0 }.toMutableMap()
        }
    }

    private fun canProgress(player: Player, id: String): Boolean {
        val progress = playersProgress[player.uniqueId] ?: return false
        val items = itemsToCraft
        val targetIndex = items.indexOfFirst { it.id == id }

        items.forEachIndexed { i, item ->
            val itemProgress = progress[item.id] ?: return false
            if (targetIndex > i && (itemProgress < item.amount)) {
                return false
            }
        }

        return true
    }

    private fun isWinner(player: Player): Boolean {
        val progress = playersProgress[player.uniqueId] ?: return false

        itemsToCraft.forEach { item ->
            val itemProgress = progress[item.id] ?: return false
            if (itemProgress < item.amount) {
                return false
            }
        }

        return true
    }

    fun getPlayersProgress(): List<Pair<OfflinePlayer, Double>> {
        val totalItems = itemsToCraft.sumOf { it.amount }

        if (totalItems == 0) {
            log(Level.WARNING, "Total items to craft is 0")
            return emptyList()
        }

        return playersProgress.map { (uuid, progressMap) ->
            val player = Bukkit.getOfflinePlayer(uuid)
            val totalProgress = progressMap.values.sum()
            var percentage = (totalProgress * 100.0) / totalItems
            percentage = (percentage * 100.0).roundToInt() / 100.0
            player to percentage
        }.sortedByDescending { it.second }
    }

}


