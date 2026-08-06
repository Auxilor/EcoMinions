package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionService
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

/** `/minions give <player> <type> [level]`. */
class GiveSubcommand(
    plugin: EcoPlugin,
    private val minions: MinionService,
    private val itemFactory: MinionItemFactory,
    private val messages: Messages
) : Subcommand(plugin, "give", "ecominions.admin.give", false) {

    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.size < 2) {
            sender.sendMessage(StringUtils.format(USAGE))
            return
        }

        val target = Bukkit.getPlayer(args[0])
        if (target == null) {
            sender.sendMessage(messages.line("messages.no-such-player", TemplateContext.of("name" to args[0])))
            return
        }

        val type = minions.getType(args[1].lowercase())
        if (type == null) {
            sender.sendMessage(messages.line("messages.no-such-type", TemplateContext.of("id" to args[1])))
            return
        }

        val level = args.getOrNull(2)?.toIntOrNull() ?: 1
        target.inventory.addItem(itemFactory.build(MinionState(type = type, level = level)))

        sender.sendMessage(
            messages.line(
                "messages.give-success",
                TemplateContext.of(
                    "player" to target.name,
                    "type" to type.name,
                    "level" to level
                )
            )
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> = when (args.size) {
        1 -> Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[0], true) }
        2 -> minions.types().map { it.id }.filter { it.startsWith(args[1], true) }
        3 -> LEVEL_SUGGESTIONS.filter { it.startsWith(args[2]) }
        else -> emptyList()
    }

    private companion object {
        const val USAGE = "&cUsage: /minions give <player> <type> [level]"
        val LEVEL_SUGGESTIONS = listOf("1", "2", "3", "5", "10")
    }
}
