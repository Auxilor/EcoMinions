package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.levelling.service.MinionXpService
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.get
import com.willfp.libreforge.getDoubleFromExpression
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

/** Grants XP to a minion, letting a fuel or an upgrade tier feed progression. */
class EffectGiveMinionXp(
    private val xp: MinionXpService
) : Effect<NoCompileData>("give_minion_xp") {

    override val description = "Grants XP to a minion."
    override val categories = setOf("minion")

    override val parameters = setOf(TriggerParameter.PLAYER)

    override val arguments = arguments {
        require("amount", "You must specify the amount of xp!", type = ArgType.EXPRESSION)
    }

    override fun onTrigger(
        config: Config,
        data: TriggerData,
        compileData: NoCompileData
    ): Boolean {
        val minion = data.dispatcher.get<Minion>() ?: return false
        val amount = config.getDoubleFromExpression("amount", data)

        if (amount <= 0.0) {
            return false
        }

        xp.grant(minion, amount)
        return true
    }
}
