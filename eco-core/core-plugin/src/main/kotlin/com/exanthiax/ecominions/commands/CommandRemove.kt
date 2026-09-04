package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.service.MinionService
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

/** `/minions remove <player>` - deletes every minion a player owns. */
class CommandRemove(
    plugin: EcoPlugin,
    private val minions: MinionService,
    private val messages: Messages
) : Subcommand(plugin, "remove", "ecominions.command.remove", false) {

    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(StringUtils.format(USAGE))
            return
        }

        val target = Bukkit.getOfflinePlayer(args[0])
        val owned = minions.ownedBy(target).toList()
        for (minion in owned) {
            minion.entity.remove()
            minions.unregister(minion)
        }

        sender.sendMessage(
            messages.line(
                "messages.remove-success",
                TemplateContext.of(
                    "player" to (target.name ?: args[0]),
                    "count" to owned.size
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
