package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.compactor.service.CompactorService
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.EmptyProvidedHolder
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.get
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter

/** Runs every compactor recipe a minion owns and currently meets the conditions for. */
class EffectMinionCompact(
    private val compactor: CompactorService
) : Effect<NoCompileData>("minion_compact") {
    override val description = "Compacts a minion's storage using its purchased compactor recipes."
    override val categories = setOf("minion", "inventory")

    override val parameters = setOf(TriggerParameter.LOCATION)

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val minion = data.dispatcher.get<Minion>() ?: return false
        val dispatcher = minion.toDispatcher()

        val recipes = minion.state.type.compactorRecipes
            .filter { minion.state.upgradeLevel(it.upgrade.id) > 0 }
            .filter { it.recipe.conditions.isEmpty() || it.recipe.conditions.areMet(dispatcher, EmptyProvidedHolder) }
            .map { it.recipe }
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
