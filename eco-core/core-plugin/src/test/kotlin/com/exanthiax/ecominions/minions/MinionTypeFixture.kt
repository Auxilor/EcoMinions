package com.exanthiax.ecominions.minions

import com.exanthiax.ecominions.compactor.model.CompactorRecipe
import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.exanthiax.ecominions.minions.model.MinionArmor
import com.exanthiax.ecominions.minions.model.MinionType
import com.exanthiax.ecominions.minions.model.ParticleSettings
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.minions.model.SpawnItemSpec
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.willfp.libreforge.Holder
import org.bukkit.Material

/**
 * A minimal minion type for tests.
 *
 * Deliberately avoids Bukkit enums so the pure layers can be tested without a
 * server present.
 */
object MinionTypeFixture {

    fun build(
        id: String = "stone",
        allowedUpgradeIds: List<String> = listOf("speed", "capacity"),
        sellPrices: Map<Material, Double> = emptyMap(),
        baseStats: MinionType.BaseStats = MinionType.BaseStats(
            speedTicks = "40",
            efficiencyTicks = "10",
            radius = "2",
            storageSlots = "1",
            maxStorageSlots = 54,
            regenDelayTicks = "60"
        ),
        levelling: LevellingSpec = LevellingSpec.DISABLED,
        holder: Holder? = null,
        compactorUpgrade: Upgrade? = null,
        compactorRecipesByTier: Map<Int, List<CompactorRecipe>> = emptyMap()
    ) = MinionType(
        id = id,
        name = "Stone Minion",
        targetBlocks = emptySet(),
        allowedUpgradeIds = allowedUpgradeIds,
        woodFamily = null,
        baseStats = baseStats,
        spawnItem = SpawnItemSpec(
            mode = SpawnItemSpec.Mode.ITEM,
            texture = "",
            itemLookup = "armor_stand",
            displayName = "Stone Minion",
            lore = emptyList()
        ),
        crafting = null,
        armor = MinionArmor.EMPTY,
        particles = ParticleSettings(full = null, fullPlayerRadius = 16, idle = null, idlePlayerRadius = 16),
        placement = PlacementSettings.NONE,
        sellPrices = sellPrices,
        levelling = levelling,
        holder = holder,
        compactorUpgrade = compactorUpgrade,
        compactorRecipesByTier = compactorRecipesByTier
    )
}
