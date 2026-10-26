package com.exanthiax.ecominions.minions

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.compactor.model.CompactorRecipeDefinition
import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.exanthiax.ecominions.minions.model.CollectionSettings
import com.exanthiax.ecominions.minions.model.FishingSettings
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionEquipment
import com.exanthiax.ecominions.minions.model.MinionModel
import com.exanthiax.ecominions.minions.model.ParticleSettings
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.minions.model.PriceList
import com.exanthiax.ecominions.minions.model.SlayerSettings
import com.exanthiax.ecominions.minions.model.SpawnItemSpec
import com.exanthiax.ecominions.minions.model.TargetSet
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
        targetBlocks: TargetSet = TargetSet.EMPTY,
        allowedUpgradeIds: List<String> = listOf("speed", "capacity"),
        prices: PriceList = PriceList.EMPTY,
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
        compactorRecipes: List<CompactorRecipeDefinition> = emptyList(),
        collection: CollectionSettings? = null,
        slayer: SlayerSettings? = null,
        fishing: FishingSettings? = null,
        bypassAntigrief: Boolean = false,
        limit: Int? = null,
        name: String = "Stone Minion",
        model: MinionModel? = null
    ) = MinionDefinition(
        id = id,
        name = name,
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
        prices = prices,
        levelling = levelling,
        holder = holder,
        compactorRecipes = compactorRecipes,
        collection = collection,
        slayer = slayer,
        fishing = fishing,
        bypassAntigrief = bypassAntigrief,
        limit = limit,
        model = model
    )
}
