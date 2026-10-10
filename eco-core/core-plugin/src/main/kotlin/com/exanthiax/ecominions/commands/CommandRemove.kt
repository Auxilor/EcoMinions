package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.api.minion.DestroyCause
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.scheduling.Regions
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.PlacementService
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.util.concurrent.atomic.AtomicInteger

/**
 * `/minions remove <player>` - deletes every minion a player owns.
 *
 * Each minion is removed on the region that owns its stand, and the count is
 * reported once the last of them is done.
 */
class CommandRemove(
    plugin: EcoPlugin,
    private val minions: MinionService,
    private val placement: PlacementService,
    private val messages: Messages,
    private val regions: Regions
) : Subcommand(plugin, "remove", "ecominions.command.remove", false) {

    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(StringUtils.format(USAGE))
            return
        }

        val target = Bukkit.getOfflinePlayer(args[0])
        val name = target.name ?: args[0]
        val owned = minions.ownedMinions(target.uniqueId)
        minions.forgetPlaced(target.uniqueId)
        if (owned.isEmpty()) {
            report(sender, name, 0)
            return
        }

        val removed = AtomicInteger()
        val pending = AtomicInteger(owned.size)
        val finish = {
            if (pending.decrementAndGet() == 0) {
                report(sender, name, removed.get())
            }
        }

        for (minion in owned) {
            regions.on(minion.entity, onRetired = finish) {
                if (placement.destroy(minion, DestroyCause.COMMAND, false)) {
                    removed.incrementAndGet()
                }
                finish()
            }
        }
    }

    private fun report(sender: CommandSender, name: String, count: Int) {
        sender.sendMessage(
            messages.line(
                "messages.remove-success",
                TemplateContext.of(
                    "player" to name,
                    "count" to count
                )
            )
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> =
        if (args.size == 1) {
            Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[0], true) }
        } else {
            emptyList()
        }

    private companion object {
        const val USAGE = "&cUsage: /minions remove <player>"
    }
}
