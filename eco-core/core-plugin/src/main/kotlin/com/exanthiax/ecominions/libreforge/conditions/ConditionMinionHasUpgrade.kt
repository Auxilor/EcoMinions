package com.exanthiax.ecominions.libreforge.conditions

import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.Dispatcher
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.arguments
import com.willfp.libreforge.conditions.Condition
import com.willfp.libreforge.get
import com.willfp.libreforge.getIntFromExpression
import com.willfp.libreforge.triggers.TriggerData

object ConditionMinionHasUpgrade : Condition<NoCompileData>("minion_has_upgrade") {
    override val description = "Passes when the minion owns, or is running, the given upgrade at or above a tier."
    override val categories = setOf("minion")

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
        optional(
            "mode",
            description = "'unlocked' (owns at/above tier) or 'active' (the currently " +
                "selected tier, for a selectable upgrade, is at/above tier). Defaults to 'unlocked'.",
            type = ArgType.STRING,
            default = "unlocked"
        )
    }

    override fun isMet(
        dispatcher: Dispatcher<*>,
        config: Config,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ): Boolean {
        val minion = dispatcher.get<Minion>() ?: return false
        val id = config.getString("upgrade").lowercase()
        val required = if (config.has("tier")) config.getIntFromExpression("tier", null as TriggerData?) else 1
        val mode = config.getStringOrNull("mode")?.lowercase() ?: "unlocked"

        val actual = if (mode == "active") minion.state.activeTierOf(id) else minion.state.upgradeLevel(id)
        return actual >= required
    }
}
