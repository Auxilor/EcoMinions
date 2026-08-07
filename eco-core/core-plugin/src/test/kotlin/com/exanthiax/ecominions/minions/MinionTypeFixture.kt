package com.exanthiax.ecominions.minions

import com.exanthiax.ecominions.minions.model.MinionArmor
import com.exanthiax.ecominions.minions.model.MinionType
import com.exanthiax.ecominions.minions.model.ParticleSettings
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.minions.model.SpawnItemSpec

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
        baseStats: MinionType.BaseStats = MinionType.BaseStats(
            speedTicks = 40,
            efficiencyTicks = 10,
            radius = 2,
            inventoryRows = 1,
            regenDelayTicks = 60
        )
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
        sellPrices = emptyMap()
    )
}
