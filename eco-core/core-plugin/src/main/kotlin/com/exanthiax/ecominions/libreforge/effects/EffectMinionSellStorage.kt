package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.libreforge.ownerPlayer
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionSell
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.SellService
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.get
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

/**
 * Sells everything in a minion's storage.
 *
 * Bespoke rather than stock because a minion's storage is a private inventory
 * that no stock effect can reach - stock inventory effects all target a player.
 */
class EffectMinionSellStorage(
    private val selling: SellService
) : Effect<NoCompileData>("minion_sell_storage") {
    override val description = "Sells the entire contents of a minion's storage."
    override val categories = setOf("minion", "economy")

    override val parameters = setOf(TriggerParameter.LOCATION)

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val minion = data.dispatcher.get<Minion>() ?: return false

        val earned = selling.sellStorage(minion)
        if (earned <= 0.0) {
            return false
        }

        TriggerMinionSell.dispatch(
            minion.toDispatcher(),
            TriggerData(
                dispatcher = minion.toDispatcher(),
                player = minion.ownerPlayer(),
                location = minion.location,
                value = earned
            )
        )

        return true
    }
}
