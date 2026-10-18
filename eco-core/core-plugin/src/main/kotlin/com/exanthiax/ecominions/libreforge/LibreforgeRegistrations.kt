package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.compactor.model.CompactorRecipeDefinition
import com.exanthiax.ecominions.core.registry.ConfigDrivenRegistry
import com.exanthiax.ecominions.core.world.WorldSettings
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.levelling.service.MinionXpService
import com.exanthiax.ecominions.libreforge.conditions.ConditionMinionHasFuel
import com.exanthiax.ecominions.libreforge.conditions.ConditionMinionHasUpgrade
import com.exanthiax.ecominions.libreforge.conditions.ConditionMinionIsType
import com.exanthiax.ecominions.libreforge.conditions.ConditionMinionLevelAbove
import com.exanthiax.ecominions.libreforge.conditions.ConditionMinionLevelBelow
import com.exanthiax.ecominions.libreforge.conditions.ConditionMinionOwnerOnline
import com.exanthiax.ecominions.libreforge.conditions.ConditionMinionStorageAbovePercent
import com.exanthiax.ecominions.libreforge.effects.EffectGiveMinionXp
import com.exanthiax.ecominions.libreforge.effects.EffectMinionCapacity
import com.exanthiax.ecominions.libreforge.effects.EffectMinionCompact
import com.exanthiax.ecominions.libreforge.effects.EffectMinionEfficiency
import com.exanthiax.ecominions.libreforge.effects.EffectMinionHeight
import com.exanthiax.ecominions.libreforge.effects.EffectMinionRadius
import com.exanthiax.ecominions.libreforge.effects.EffectMinionSellStorage
import com.exanthiax.ecominions.libreforge.effects.EffectMinionSpeed
import com.exanthiax.ecominions.libreforge.mutators.MutatorMinionAsDispatcher
import com.exanthiax.ecominions.libreforge.mutators.MutatorMinionLocationToTargetBlock
import com.exanthiax.ecominions.libreforge.mutators.MutatorMinionOwnerAsDispatcher
import com.exanthiax.ecominions.libreforge.triggers.TriggerClickMinion
import com.exanthiax.ecominions.libreforge.triggers.TriggerGroupMinionStatic
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionCatchFish
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionCollect
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionCollectItem
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionFuelExpire
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionFuelStart
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionKillMob
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionLevelUp
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionMineBlock
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionPickup
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionPlace
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionRegenBlock
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionSell
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionStorageFull
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionTick
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionUpgrade
import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionCompactService
import com.exanthiax.ecominions.minions.service.SellService
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.mutators.Mutators
import com.willfp.libreforge.triggers.Triggers
import org.bukkit.entity.Entity

/**
 * Single owner of everything the plugin hands to libreforge.
 *
 * When effects, conditions or triggers are added they are registered here, not in
 * the plugin class.
 */
object LibreforgeRegistrations {

    /**
     * `compactorRecipes` must load before `types`: [MinionDefinitionLoader] resolves
     * each type's `compactor-recipes:` list during `load()`, so a type parsed
     * before the catalog is populated silently gets none of them. This ordering
     * has to survive `/minions reload` too, not just first boot - reload calls
     * `clear()` on every category and reloads them in this order.
     *
     * `blocks` goes first for the same reason: a type's `target-blocks:` are
     * resolved against the block definitions while the type is being parsed.
     */
    fun configCategories(
        blocks: ConfigDrivenRegistry<MinionBlock>,
        types: ConfigDrivenRegistry<MinionDefinition>,
        upgrades: ConfigDrivenRegistry<Upgrade>,
        fuels: ConfigDrivenRegistry<Fuel>,
        compactorRecipes: ConfigDrivenRegistry<CompactorRecipeDefinition>
    ): List<ConfigCategory> = listOf(blocks, upgrades, fuels, compactorRecipes, types)

    /**
     * Every libreforge component this plugin owns.
     *
     * Registered from one place so the plugin class stays wiring-only, and so
     * there is a single answer to "what does EcoMinions add to libreforge?".
     */
    fun registerAll(
        upgrades: ConfigDrivenRegistry<Upgrade>,
        worlds: WorldSettings,
        selling: SellService,
        compacting: MinionCompactService,
        xpService: MinionXpService,
        minionAt: (Entity) -> Minion?
    ) {
        UpgradePlaceholderProvider(upgrades::all).register()
        MinionHolders(upgrades::find).register(worlds)

        Effects.register(EffectMinionSpeed)
        Effects.register(EffectMinionEfficiency)
        Effects.register(EffectMinionRadius)
        Effects.register(EffectMinionHeight)
        Effects.register(EffectMinionCapacity)
        Effects.register(EffectMinionSellStorage(selling))
        Effects.register(EffectMinionCompact(compacting))
        Effects.register(EffectGiveMinionXp(xpService))

        Mutators.register(MutatorMinionOwnerAsDispatcher)
        Mutators.register(MutatorMinionLocationToTargetBlock)
        Mutators.register(MutatorMinionAsDispatcher(minionAt))

        Triggers.register(TriggerMinionMineBlock)
        Triggers.register(TriggerMinionStorageFull)
        Triggers.register(TriggerMinionRegenBlock)
        Triggers.register(TriggerMinionTick)
        Triggers.register(TriggerMinionCollectItem)
        Triggers.register(TriggerMinionKillMob)
        Triggers.register(TriggerMinionCatchFish)
        Triggers.register(TriggerGroupMinionStatic)
        Triggers.register(TriggerMinionPlace)
        Triggers.register(TriggerMinionPickup)
        Triggers.register(TriggerMinionUpgrade)
        Triggers.register(TriggerMinionFuelStart)
        Triggers.register(TriggerMinionFuelExpire)
        Triggers.register(TriggerMinionSell)
        Triggers.register(TriggerMinionLevelUp)
        Triggers.register(TriggerMinionCollect)
        Triggers.register(TriggerClickMinion)

        Conditions.register(ConditionMinionLevelAbove)
        Conditions.register(ConditionMinionLevelBelow)
        Conditions.register(ConditionMinionIsType)
        Conditions.register(ConditionMinionHasUpgrade)
        Conditions.register(ConditionMinionStorageAbovePercent)
        Conditions.register(ConditionMinionHasFuel)
        Conditions.register(ConditionMinionOwnerOnline)
    }
}
