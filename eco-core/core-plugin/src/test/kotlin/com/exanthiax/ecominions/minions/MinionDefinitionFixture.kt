package com.exanthiax.ecominions.minions

import com.exanthiax.ecominions.compactor.model.CompactorRecipeDefinition
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.exanthiax.ecominions.minions.model.MinionEquipment
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.ParticleSettings
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.minions.model.SpawnItemSpec
import com.willfp.libreforge.Holder
import org.bukkit.Material

/**
 * A minimal minion definition for tests.
 *
 * Deliberately avoids Bukkit enums so the pure layers can be tested without a
 * server present.
 */
object MinionDefinitionFixture {

    fun build(
        id: String = "stone",
        type: MinionType = MinionType.MINER,
        targetBlocks: Set<Material> = emptySet(),
        allowedUpgradeIds: List<String> = listOf("speed", "capacity"),
        sellPrices: Map<Material, Double> = emptyMap(),
        baseStats: MinionDefinition.BaseStats = MinionDefinition.BaseStats(
            speedTicks = "40",
            efficiencyTicks = "10",
            radius = "2",
            storageSlots = "1",
            maxStorageSlots = "54",
            regenDelayTicks = "60",
            hardSlotCap = 54
        ),
        placement: PlacementSettings = PlacementSettings.NONE,
        levelling: LevellingSpec = LevellingSpec.DISABLED,
        holder: Holder? = null,
        compactorRecipes: List<CompactorRecipeDefinition> = emptyList()
    ) = MinionDefinition(
        id = id,
        name = "Stone Minion",
        targetBlocks = targetBlocks,
        allowedUpgradeIds = allowedUpgradeIds,
        type = type,
        baseStats = baseStats,
        spawnItem = SpawnItemSpec(
            itemLookup = "armor_stand",
            displayName = "Stone Minion",
            lore = emptyList()
        ),
        crafting = null,
        equipment = MinionEquipment.EMPTY,
        particles = ParticleSettings(
            full = null,
            fullPlayerRadius = 16,
            idle = null,
            idlePlayerRadius = 16,
            blocked = null,
            blockedPlayerRadius = 16
        ),
        placement = placement,
        sellPrices = sellPrices,
        levelling = levelling,
        holder = holder,
        compactorRecipes = compactorRecipes
    )
}
