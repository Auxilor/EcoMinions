package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.persistence.MinionDataKeys
import com.exanthiax.ecominions.minions.service.MinionService
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.items.Items
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe

/**
 * Registers the server recipes that produce minion items.
 *
 * The recipe result is a marker item; [MinionCraftListener] swaps it for the real
 * minion item, so recipe matching never depends on skull textures.
 */
class MinionRecipes(
    private val plugin: EcoPlugin,
    private val minions: MinionService,
    private val keys: MinionDataKeys
) {
    private val registered = mutableListOf<NamespacedKey>()

    fun registerAll() {
        for (type in minions.types()) {
            val crafting = type.crafting ?: continue
            val key = NamespacedKey(plugin, "recipe_${type.id}")
            val recipe = ShapedRecipe(key, markerFor(type.id) ?: continue)

            recipe.shape(*crafting.shape.toTypedArray())
            for ((symbol, lookup) in crafting.ingredients) {
                val ingredient = Items.lookup(lookup).item
                if (ingredient.type != Material.AIR) {
                    recipe.setIngredient(symbol, RecipeChoice.ExactChoice(ingredient))
                }
            }

            Bukkit.addRecipe(recipe)
            registered += key
        }
    }

    fun unregisterAll() {
        registered.forEach { Bukkit.removeRecipe(it) }
        registered.clear()
    }

    private fun markerFor(typeId: String): ItemStack? {
        val marker = ItemStack(Material.ARMOR_STAND)
        val meta = marker.itemMeta ?: return null
        meta.persistentDataContainer.set(keys.isMinion, MinionDataKeys.BYTE, MARKER)
        meta.persistentDataContainer.set(keys.type, MinionDataKeys.STRING, typeId)
        marker.itemMeta = meta
        return marker
    }

    private companion object {
        const val MARKER: Byte = 1
    }
}
