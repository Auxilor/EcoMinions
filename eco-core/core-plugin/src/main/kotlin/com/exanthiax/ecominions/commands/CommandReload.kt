package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.core.scheduling.Regions
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.toNiceString
import org.bukkit.command.CommandSender

/**
 * `/minions reload`.
 *
 * A reload rebuilds registries, recipes and tasks shared by the whole server, so
 * it runs on the global region, which a player's command does not under Folia.
 */
class CommandReload(
    private val plugin: EcoPlugin,
    private val regions: Regions,
    private val minionTypeCount: () -> Int
) : Subcommand(plugin, "reload", "ecominions.command.reload", false) {

    override fun onExecute(sender: CommandSender, args: List<String>) {
        regions.global {
            sender.sendMessage(
                plugin.langYml.getMessage("reloaded", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                    .replace("%time%", plugin.reloadWithTime().toNiceString())
                    .replace("%count%", minionTypeCount().toString())
            )
        }
    }
}
