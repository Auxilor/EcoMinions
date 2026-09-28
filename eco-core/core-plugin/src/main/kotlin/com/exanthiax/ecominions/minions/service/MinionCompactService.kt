package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionCompactEvent
import com.exanthiax.ecominions.compactor.service.CompactorService
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.willfp.libreforge.EmptyProvidedHolder

/** Runs a minion's owned compactor recipes over its storage. */
class MinionCompactService(
    private val compactor: CompactorService,
    private val events: EventDispatcher
) {
    /**
     * Only recipes whose upgrade the minion owns and whose conditions it meets
     * run. What no longer fits is dropped where the minion stands.
     *
     * @return Whether any recipe ran.
     */
    fun compact(minion: Minion): Boolean {
        val dispatcher = minion.toDispatcher()
        val recipes = minion.state.definition.compactorRecipes
            .filter { minion.state.upgradeLevel(it.upgrade.id) > 0 }
            .filter { it.recipe.conditions.isEmpty() || it.recipe.conditions.areMet(dispatcher, EmptyProvidedHolder) }
            .map { it.recipe }
        if (recipes.isEmpty() || !events.dispatch(MinionCompactEvent(minion))) {
            return false
        }

        for (item in compactor.compact(minion.storage, recipes)) {
            minion.entity.world.dropItemNaturally(minion.entity.location, item)
        }
        return true
    }
}
