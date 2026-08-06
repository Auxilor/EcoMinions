package com.exanthiax.ecominions.minions.model

import org.bukkit.Material
import com.exanthiax.ecominions.api.minion.MinionType as ApiMinionType

/**
 * A configured kind of minion. Immutable, fully validated by the time it exists:
 * everything questionable was rejected by the loader.
 */
class MinionType(
    override val id: String,
    override val name: String,
    override val targetBlocks: Set<Material>,
    override val allowedUpgradeIds: List<String>,
    val woodFamily: WoodFamily?,
    val baseStats: BaseStats,
    val spawnItem: SpawnItemSpec,
    val crafting: CraftingSpec?,
    val armor: MinionArmor,
    val particles: ParticleSettings,
    val placement: PlacementSettings,
    val sellPrices: Map<Material, Double>
) : ApiMinionType {

    override val baseSpeedTicks: Int
        get() = baseStats.speedTicks

    override val baseInventoryRows: Int
        get() = baseStats.inventoryRows

    override val isTreeType: Boolean
        get() = woodFamily != null

    /** @return The configured price of [material], or null if it is not sellable. */
    fun priceOf(material: Material): Double? = sellPrices[material]

    data class BaseStats(
        val speedTicks: Int,
        val efficiencyTicks: Int,
        val radius: Int,
        val inventoryRows: Int,
        val regenDelayTicks: Int
    )
}
