package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.minions.ui.TypeSelectMenu
import org.bukkit.entity.Player
import wiki.ecohub.core.command.EcoCommand
import wiki.ecohub.core.command.command
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.ui.text.StringUtils

private const val CONSOLE_USAGE = "&eEcoMinions &7- give, reload, remove, info, open"

/** `/minions` - opens the type browser, or lists subcommands from the console. */
internal fun EcoPlugin.minionsCommand(
    typeSelect: TypeSelectMenu,
    vararg subcommands: EcoMinionsSubcommand
): EcoCommand = command(name = "minions", permission = "ecominions.command.minions") {
    mount(*subcommands)

    executes {
        val player = sender as? Player

        if (player != null) {
            typeSelect.open(player)
        } else {
            sender.sendMessage(StringUtils.format(CONSOLE_USAGE))
        }
    }
}
