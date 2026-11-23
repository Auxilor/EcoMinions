package com.exanthiax.ecominions.libreforge.conditions

import com.exanthiax.ecominions.minions.service.Minion
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.config.arguments
import wiki.ecohub.config.section.getIntFromExpression
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.condition.Condition
import wiki.ecohub.rules.holder.Dispatcher
import wiki.ecohub.rules.holder.ProvidedHolder
import wiki.ecohub.rules.holder.get
import wiki.ecohub.rules.trigger.TriggerData

object ConditionMinionHasUpgrade : Condition<NoCompileData>("minion_has_upgrade") {
    override val description = "Passes when the minion owns the given upgrade at or above a tier."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only works on minions. Anywhere else it never passes.",
        "A compactor recipe counts as the upgrade compactor_<recipe id>."
    )

    override val arguments = arguments {
        require(
            "upgrade",
            "You must specify the upgrade!",
            description = "The id of the upgrade to check.",
            type = ArgType.STRING
        )
        optional(
            "tier",
            description = "The minimum tier required. Defaults to 1.",
            type = ArgType.EXPRESSION,
            default = "1"
        )
    }

    override fun isMet(
        dispatcher: Dispatcher<*>,
        config: Config,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ): Boolean {
        val minion = dispatcher.get<Minion>() ?: return false
        val required = if (config.has("tier")) config.getIntFromExpression("tier", null as TriggerData?) else 1

        return minion.state.upgradeLevel(config.getString("upgrade").lowercase()) >= required
    }
}
