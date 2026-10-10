package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.integration.MinionPlaceholders
import com.exanthiax.ecominions.minions.integration.targetedArmorStand
import com.exanthiax.ecominions.minions.service.MinionService
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** `/minions info` - reports on the minion the sender is looking at. */
class CommandInfo(
    plugin: EcoPlugin,
    private val minions: MinionService,
    private val messages: Messages
) : Subcommand(plugin, "info", "ecominions.command.info", true) {

    override fun onExecute(sender: CommandSender, args: List<String>) {
        val player = sender as? Player ?: return
        val minion = player.targetedArmorStand(TARGET_RANGE)?.let { minions.getByEntity(it) }

        if (minion == null) {
            player.sendMessage(messages.line("messages.info-no-target"))
            return
        }

        val context = TemplateContext.of(MinionPlaceholders.MINION to minion)
        player.sendMessage(messages.line("messages.info-header", context))
        for (line in messages.lines("gui.main.info.lore", context)) {
            player.sendMessage(line)
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()

    private companion object {
        const val TARGET_RANGE = 8.0
    }
}
