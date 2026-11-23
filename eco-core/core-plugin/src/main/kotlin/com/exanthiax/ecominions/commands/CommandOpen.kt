package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.minions.ui.TypeSelectMenu
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** `/minions open` - the type browser. */
class CommandOpen(
    private val typeSelect: TypeSelectMenu
) : EcoMinionsSubcommand("open", "ecominions.command.open", false) {

    override fun onExecute(sender: CommandSender, args: List<String>) {
        (sender as? Player)?.let { typeSelect.open(it) }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}
