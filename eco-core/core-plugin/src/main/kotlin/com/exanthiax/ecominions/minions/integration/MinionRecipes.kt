package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionService
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.recipe.Recipes

/**
 * Registers the recipes that produce minion items.
 *
 * eco's recipe system outputs the real minion item, so nothing needs to swap a
 * marker for it afterwards, and permissions are handled by eco itself.
 */
class MinionRecipes(
    private val plugin: EcoPlugin,
    private val minions: MinionService,
    private val itemFactory: MinionItemFactory
) {
    fun registerAll() {
        for (type in minions.types()) {
            val crafting = type.crafting ?: continue
            val output = itemFactory.build(MinionState(type = type))
                .apply { amount = crafting.giveAmount }

            Recipes.createAndRegisterRecipe(
                plugin,
                type.id,
                output,
                crafting.recipe,
                crafting.permission,
                crafting.shapeless
            )
        }
    }
}
