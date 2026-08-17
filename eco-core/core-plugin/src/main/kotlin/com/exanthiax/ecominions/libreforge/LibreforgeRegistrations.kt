package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.compactor.model.CompactorRecipe
import com.exanthiax.ecominions.core.registry.ConfigDrivenRegistry
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.libreforge.effects.EffectMinionCapacity
import com.exanthiax.ecominions.libreforge.effects.EffectMinionEfficiency
import com.exanthiax.ecominions.libreforge.effects.EffectMinionRadius
import com.exanthiax.ecominions.libreforge.effects.EffectMinionSpeed
import com.exanthiax.ecominions.minions.model.MinionType
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.loader.configs.ConfigCategory

/**
 * Single owner of everything the plugin hands to libreforge.
 *
 * When effects, conditions or triggers are added they are registered here, not in
 * the plugin class.
 */
object LibreforgeRegistrations {

    fun configCategories(
        types: ConfigDrivenRegistry<MinionType>,
        upgrades: ConfigDrivenRegistry<Upgrade>,
        fuels: ConfigDrivenRegistry<Fuel>,
        compactorRecipes: ConfigDrivenRegistry<CompactorRecipe>
    ): List<ConfigCategory> = listOf(upgrades, fuels, compactorRecipes, types)

    /**
     * Every libreforge component this plugin owns.
     *
     * Registered from one place so the plugin class stays wiring-only, and so
     * there is a single answer to "what does EcoMinions add to libreforge?".
     */
    fun registerAll(upgrades: ConfigDrivenRegistry<Upgrade>) {
        UpgradePlaceholderProvider(upgrades::all).register()
        MinionHolders(upgrades::find).register()

        Effects.register(EffectMinionSpeed)
        Effects.register(EffectMinionEfficiency)
        Effects.register(EffectMinionRadius)
        Effects.register(EffectMinionCapacity)
    }
}
