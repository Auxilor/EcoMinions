package com.exanthiax.ecominions.compactor.service

import com.exanthiax.ecominions.compactor.model.CompactorRecipe
import com.exanthiax.ecominions.core.registry.ConfigDrivenRegistry
import com.willfp.eco.core.items.Items
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/** Compresses bulk drops in a storage inventory into their block form. */
class CompactorService(
    private val recipes: ConfigDrivenRegistry<CompactorRecipe>
) {
    /**
     * Applies every recipe until nothing more can be compacted, so nine iron ore
     * becoming a block can in turn feed a block-of-blocks recipe.
     *
     * @return Items that did not fit back into [inventory].
     */
    fun compact(inventory: Inventory): List<ItemStack> {
        val overflow = mutableListOf<ItemStack>()
        var changed = true

        while (changed) {
            changed = false
            for (recipe in recipes.all()) {
                while (countMatching(inventory, recipe) >= recipe.inputAmount) {
                    if (!consume(inventory, recipe)) {
                        break
                    }
                    overflow += inventory.addItem(outputOf(recipe)).values
                    changed = true
                }
            }
        }

        return overflow
    }

    private fun outputOf(recipe: CompactorRecipe): ItemStack {
        val item = Items.lookup(recipe.outputLookup).item.clone()
        item.amount = recipe.outputAmount
        return item
    }

    private fun countMatching(inventory: Inventory, recipe: CompactorRecipe): Int {
        val input = Items.lookup(recipe.inputLookup)
        var total = 0
        for (slot in 0 until inventory.size) {
            val item = inventory.getItem(slot) ?: continue
            if (input.matches(item)) {
                total += item.amount
            }
        }
        return total
    }

    private fun consume(inventory: Inventory, recipe: CompactorRecipe): Boolean {
        val input = Items.lookup(recipe.inputLookup)
        var remaining = recipe.inputAmount

        for (slot in 0 until inventory.size) {
            val item = inventory.getItem(slot) ?: continue
            if (!input.matches(item)) {
                continue
            }
            val taken = minOf(item.amount, remaining)
            item.amount -= taken
            if (item.amount <= 0) {
                inventory.setItem(slot, null)
            }
            remaining -= taken
            if (remaining <= 0) {
                return true
            }
        }
        return false
    }
}
