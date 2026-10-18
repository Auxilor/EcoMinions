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

    override val description = "Gives XP to a minion."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only works on minions: in a minion type's effects, an upgrade's effects, or level-up-effects. Anywhere else it does nothing.",
        "Use it to feed levelling from something that is not an xp-gain-method, such as an upgrade."
    )

    override val parameters = setOf(TriggerParameter.PLAYER)

    override val arguments = arguments {
        require(
            "amount",
            "You must specify the amount of xp!",
            description = "The XP to give the minion.",
            type = ArgType.EXPRESSION,
            example = "10"
        )
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
