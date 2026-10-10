package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionCompactEvent
import com.exanthiax.ecominions.compactor.model.CompactorRecipe
import com.exanthiax.ecominions.compactor.service.CompactorService
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.libreforge.toDispatcher
import com.willfp.eco.core.items.Items
import com.willfp.libreforge.EmptyProvidedHolder
import org.bukkit.inventory.ItemStack

/** Runs a minion's owned compactor recipes over its storage. */
class MinionCompactService(
    private val compactor: CompactorService,
    private val events: EventDispatcher
) {
    /**
     * Only recipes whose upgrade the minion owns and whose conditions it meets
     * run, and only when storage holds a full batch for one of them. What no
     * longer fits is dropped where the minion stands.
     *
     * @return Whether any recipe ran.
     */
    fun compact(minion: Minion): Boolean {
        val recipes = activeRecipes(minion)
        val ready = recipes.filter { compactor.canCompact(minion.storage, it) }
        if (ready.isEmpty() || !events.dispatch(MinionCompactEvent(minion, ready.map { it.id }))) {
            return false
        }

        for (item in compactor.compact(minion.storage, recipes)) {
            minion.entity.world.dropItemNaturally(minion.entity.location, item)
        }
        return true
    }

    /** @return Whether [item] is the input of a recipe [minion] can run now. */
    fun isInput(minion: Minion, item: ItemStack): Boolean =
        activeRecipes(minion).any { Items.lookup(it.inputLookup).matches(item) }

    private fun activeRecipes(minion: Minion): List<CompactorRecipe> {
        val owned = minion.state.definition.compactorRecipes
            .filter { minion.state.upgradeLevel(it.upgrade.id) > 0 }
        if (owned.isEmpty()) {
            return emptyList()
        }
        val dispatcher = minion.toDispatcher()
        return owned
            .filter { it.recipe.conditions.isEmpty() || it.recipe.conditions.areMet(dispatcher, EmptyProvidedHolder) }
            .map { it.recipe }
    }
}
