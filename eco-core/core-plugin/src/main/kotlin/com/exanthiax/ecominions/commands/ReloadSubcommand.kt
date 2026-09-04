package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.toNiceString
import org.bukkit.command.CommandSender

/** `/minions reload`. */
class ReloadSubcommand(
    private val plugin: EcoPlugin,
    private val messages: Messages,
    private val minionTypeCount: () -> Int
) : Subcommand(plugin, "reload", "ecominions.admin.reload", false) {

    override fun onExecute(sender: CommandSender, args: List<String>) {
        val time = plugin.reloadWithTime().toNiceString()
        sender.sendMessage(
            messages.line(
                "messages.reloaded",
                TemplateContext.of(
                    "time" to time,
                    "count" to minionTypeCount()
                )
            )
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}
