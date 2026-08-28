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

object ConditionMinionLevelBelow : Condition<NoCompileData>("minion_level_below") {
    override val description = "Passes when the minion's level is at or below the given level."
    override val categories = setOf("minion")

    override val arguments = arguments {
        require(
            "level",
            "You must specify the level!",
            description = "The maximum minion level.",
            type = ArgType.EXPRESSION
        )
    }

    override fun isMet(
        dispatcher: Dispatcher<*>,
        config: Config,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ): Boolean {
        val minion = dispatcher.get<Minion>() ?: return false
        return minion.level <= config.getIntFromExpression("level", null as TriggerData?)
    }
}
