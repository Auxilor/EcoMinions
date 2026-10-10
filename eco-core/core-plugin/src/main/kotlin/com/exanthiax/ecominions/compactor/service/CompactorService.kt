package com.exanthiax.ecominions.compactor.service

import com.exanthiax.ecominions.compactor.model.CompactorRecipe
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.willfp.eco.core.items.Items
import org.bukkit.inventory.ItemStack

/** Compresses bulk drops in a minion's storage into their block form. */
class CompactorService {
    /**
     * Applies every recipe until nothing more can be compacted, so nine iron ore
     * becoming a block can in turn feed a block-of-blocks recipe.
     *
     * @return Items that did not fit back into [storage].
     */
    fun compact(storage: MinionStorage, recipes: List<CompactorRecipe>): List<ItemStack> {
        val ordered = orderByChain(recipes)
        val overflow = mutableListOf<ItemStack>()
        var changed = true

        while (changed) {
            changed = false
            for (recipe in ordered) {
                while (countMatching(storage, recipe) >= recipe.inputAmount) {
                    if (!consume(storage, recipe)) {
                        break
                    }
                    storage.add(outputOf(recipe))?.let { overflow += it }
                    changed = true
                }
            }
        }

        return overflow
    }

    /** @return Whether [storage] holds a full batch of [recipe]'s input. */
    fun canCompact(storage: MinionStorage, recipe: CompactorRecipe): Boolean =
        countMatching(storage, recipe) >= recipe.inputAmount

    /** Runs a recipe after the one it's a `higher-level-of`, e.g. coal -> coal_block first. */
    private fun orderByChain(recipes: List<CompactorRecipe>): List<CompactorRecipe> {
        val byId = recipes.associateBy { it.id }
        val ordered = mutableListOf<CompactorRecipe>()
        val visited = mutableSetOf<String>()

        fun visit(recipe: CompactorRecipe) {
            if (!visited.add(recipe.id)) {
                return
            }
            recipe.higherLevelOf?.let { byId[it] }?.let(::visit)
            ordered += recipe
        }

        recipes.forEach(::visit)
        return ordered
    }

    private fun outputOf(recipe: CompactorRecipe): ItemStack {
        val item = Items.lookup(recipe.outputLookup).item.clone()
        item.amount = recipe.outputAmount
        return item
    }

    private fun countMatching(storage: MinionStorage, recipe: CompactorRecipe): Int {
        val input = Items.lookup(recipe.inputLookup)
        var total = 0
        for (slot in 0 until storage.size) {
            val item = storage.getItem(slot) ?: continue
            if (input.matches(item)) {
                total += item.amount
            }
        }
        return total
    }

    private fun consume(storage: MinionStorage, recipe: CompactorRecipe): Boolean {
        val input = Items.lookup(recipe.inputLookup)
        var remaining = recipe.inputAmount

        for (slot in 0 until storage.size) {
            val item = storage.getItem(slot) ?: continue
            if (!input.matches(item)) {
                continue
            }
            val taken = minOf(item.amount, remaining)
            item.amount -= taken
            if (item.amount <= 0) {
                storage.setItem(slot, null)
            }
            remaining -= taken
            if (remaining <= 0) {
                return true
            }
        }
        return false
    }
}
