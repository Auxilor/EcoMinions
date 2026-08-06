package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.minions.ui.TypeSelectMenu
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.util.StringUtils
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** `/minions` - opens the type browser, or lists subcommands from the console. */
class CommandMinions(
    plugin: EcoPlugin,
    private val typeSelect: TypeSelectMenu,
    subcommands: List<com.willfp.eco.core.command.impl.Subcommand>
) : PluginCommand(plugin, "minions", "ecominions.command", false) {

    init {
        subcommands.forEach { addSubcommand(it) }
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (sender is Player) {
            typeSelect.open(sender)
            return
        }
        sender.sendMessage(StringUtils.format(CONSOLE_USAGE))
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()

    private companion object {
        const val CONSOLE_USAGE = "&eEcoMinions &7- give, reload, remove, info, open"
    }
}
