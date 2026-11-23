package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.core.scheduling.Regions
import org.bukkit.command.CommandSender
import wiki.ecohub.core.plugin.EcoPlugin
import wiki.ecohub.core.stdlib.toNiceString
import wiki.ecohub.ui.text.StringUtils

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
) : EcoMinionsSubcommand("reload", "ecominions.command.reload", false) {

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
