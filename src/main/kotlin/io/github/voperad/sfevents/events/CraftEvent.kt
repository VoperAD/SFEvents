package io.github.voperad.sfevents.events

import io.github.thebusybiscuit.slimefun4.api.events.MultiBlockCraftEvent
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem
import io.github.voperad.sfevents.debug
import io.github.voperad.sfevents.events.configs.CraftEventConfiguration
import io.github.voperad.sfevents.events.configs.ItemToCraft
import io.github.voperad.sfevents.log
import io.github.voperad.sfevents.managers.EventManager
import io.github.voperad.sfevents.pluginInstance
import io.github.voperad.sfevents.secondsToTicks
import io.github.voperad.sfevents.util.ChatUtils
import kotlinx.coroutines.delay
import me.clip.placeholderapi.PlaceholderAPI
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
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

class CraftEvent(config: CEC): BaseEvent<CEC>(config), Listener, Progressable, Top {

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
        jobs.toList().forEach { it.cancel() }
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

    private fun isTargetItem(item: ItemStack): TargetItemCraftedInfo? {
        return SlimefunItem.getByItem(item)?.let { sfItem ->
            itemsToCraft.firstOrNull { it.id == sfItem.id }?.let { itemToCraft ->
                TargetItemCraftedInfo(sfItem, itemToCraft, item.amount)
            }
        }
    }

    private fun addProgress(player: Player, targetItemCraftedInfo: TargetItemCraftedInfo) {
        initPlayerProgress(player)
        val itemToCraft = targetItemCraftedInfo.itemToCraft

        if (config.craftInOrder && !canProgress(player, itemToCraft.id)) {
            return
        }

        val progress = playersProgress[player.uniqueId] ?: return
        val itemProgress: () -> Int = { progress[itemToCraft.id] ?: 0 }

        if (itemProgress() >= itemToCraft.amount) {
            player.sendMessage("${ChatColor.AQUA}You have already crafted ${itemProgress()} ${targetItemCraftedInfo.slimefunItem.itemName}")
            return
        }

        progress.computeIfPresent(itemToCraft.id) { _, amount ->
            val sum = amount + targetItemCraftedInfo.amount
            debug("addProgress(${player.name}) -> ${itemToCraft.id}: [$amount -> $sum]")
            if (sum >= itemToCraft.amount) itemToCraft.amount else sum
        }

        debug("Current progress for ${player.name}: ${itemProgress()}")
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

    override fun sendPlayerProgress(playerName: Player) {
        playersProgress[playerName.uniqueId]?.let { progressMap ->
            val totalItems = itemsToCraft.sumOf { it.amount }
            val totalProgress = progressMap.values.sum()
            var percentage = (totalProgress * 100.0) / totalItems
            percentage = (percentage * 100.0).roundToInt() / 100.0

            playerName.sendMessage("${ChatColor.GREEN}Your progress: $percentage%")
            itemsToCraft.forEach { item ->
                val itemProgress = progressMap[item.id] ?: 0

                val sfItem = SlimefunItem.getById(item.id) ?: run {
                    playerName.sendMessage("${ChatColor.RED}Item ${item.id} not found!")
                    return@forEach
                }

                playerName.sendMessage("${sfItem.itemName}: ${ChatColor.AQUA}$itemProgress/${item.amount}")
            }

        } ?: playerName.sendMessage("${ChatColor.RED}You have no progress in this event.")
    }

    override fun sendEventTop(commandSender: CommandSender) {
        getPlayersProgress().forEachIndexed { index, (player, percentage) ->
            player.name?.let { name ->
                commandSender.sendMessage(
                    "${ChatColor.GOLD}${index + 1}. ${ChatColor.YELLOW}$name: ${ChatColor.AQUA}$percentage%"
                )
            }
        }
    }

    data class TargetItemCraftedInfo(
        val slimefunItem: SlimefunItem,
        val itemToCraft: ItemToCraft,
        val amount: Int
    )

}


