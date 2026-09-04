package com.exanthiax.ecominions.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.toNiceString
import org.bukkit.command.CommandSender

class CommandReload(
    private val plugin: EcoPlugin,
    private val minionTypeCount: () -> Int
) : Subcommand(plugin, "reload", "ecominions.command.reload", false) {

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(
            plugin.langYml.getMessage("reloaded", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%time%", plugin.reloadWithTime().toNiceString())
                .replace("%count%", minionTypeCount().toString())
        )
    }
}
