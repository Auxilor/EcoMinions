package com.exanthiax.ecominions.commands

import com.exanthiax.ecominions.core.text.Messages
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import org.bukkit.command.CommandSender

/** `/minions reload`. */
class ReloadSubcommand(
    private val plugin: EcoPlugin,
    private val messages: Messages
) : Subcommand(plugin, "reload", "ecominions.admin.reload", false) {

    override fun onExecute(sender: CommandSender, args: List<String>) {
        plugin.reload()
        sender.sendMessage(messages.line("messages.reload-success"))
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}
