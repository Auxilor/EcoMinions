package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.compactor.service.CompactorService
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.get
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

/** Runs a minion's active compactor tier's recipes over its storage. */
class EffectMinionCompact(
    private val compactor: CompactorService
) : Effect<NoCompileData>("minion_compact") {
    override val description = "Compacts a minion's storage using its active compactor tier's recipes."
    override val categories = setOf("minion", "inventory")

    override val parameters = setOf(TriggerParameter.LOCATION)

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val minion = data.dispatcher.get<Minion>() ?: return false
        val upgrade = minion.state.type.compactorUpgrade ?: return false
        val active = minion.state.activeTierOf(upgrade.id)
        val recipes = (1..active).flatMap { level -> minion.state.type.compactorRecipesByTier[level].orEmpty() }
        if (recipes.isEmpty()) {
            return false
        }

        val overflow = compactor.compact(minion.storage, recipes)
        for (item in overflow) {
            minion.entity.world.dropItemNaturally(minion.entity.location, item)
        }

        return true
    }
}
