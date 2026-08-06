package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.minions.ui.TypeSelectMenu
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** `/minions open` - the type browser. */
class OpenSubcommand(
    plugin: EcoPlugin,
    private val typeSelect: TypeSelectMenu
) : Subcommand(plugin, "open", "ecominions.command", false) {

    override fun onExecute(sender: CommandSender, args: List<String>) {
        (sender as? Player)?.let { typeSelect.open(it) }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}
