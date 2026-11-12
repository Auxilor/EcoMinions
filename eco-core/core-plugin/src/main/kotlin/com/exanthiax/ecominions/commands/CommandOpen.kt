package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.minions.ui.TypeSelectMenu
import com.willfp.eco.core.command.impl.Subcommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import wiki.ecohub.core.plugin.EcoPlugin

/** `/minions open` - the type browser. */
class CommandOpen(
    plugin: EcoPlugin,
    private val typeSelect: TypeSelectMenu
) : Subcommand(plugin, "open", "ecominions.command.open", false) {

    override fun onExecute(sender: CommandSender, args: List<String>) {
        (sender as? Player)?.let { typeSelect.open(it) }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}
