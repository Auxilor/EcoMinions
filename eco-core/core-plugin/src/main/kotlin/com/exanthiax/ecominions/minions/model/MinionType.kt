package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.willfp.libreforge.Holder
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
    val sellPrices: Map<Material, Double>,
    /** Parsed from the type's `levelling:` block. Disabled unless configured. */
    val levelling: LevellingSpec = LevellingSpec.DISABLED,
    /** Compiled from the type's `effects:` block. Always active, no purchase needed. */
    val holder: Holder? = null
) : ApiMinionType {

    /** Preview value shown before a minion's actual level is known, e.g. in the type-select menu. */
    override val baseSpeedTicks: Int
        get() = baseStats.valueOf(MinionStat.SPEED_TICKS, PREVIEW_LEVEL)

    override val baseInventoryRows: Int
        get() = baseStats.valueOf(MinionStat.INVENTORY_ROWS, PREVIEW_LEVEL)

    override val isTreeType: Boolean
        get() = woodFamily != null

    override val isLevellable: Boolean
        get() = levelling.enabled

    override val maxLevel: Int
        get() = levelling.maxLevel

    /** @return The configured price of [material], or null if it is not sellable. */
    fun priceOf(material: Material): Double? = sellPrices[material]

    /**
     * Base stats as authored, which may be expressions in `%level%`.
     *
     * The evaluator is a constructor parameter so the model layer never reaches
     * for eco's expression engine, and so a test can supply plain arithmetic.
     */
    data class BaseStats(
        val speedTicks: String,
        val efficiencyTicks: String,
        val radius: String,
        val inventoryRows: String,
        val regenDelayTicks: String,
        val evaluate: (String, Int) -> Int = { expression, _ -> expression.trim().toInt() }
    ) {
        fun valueOf(stat: MinionStat, level: Int): Int = when (stat) {
            MinionStat.SPEED_TICKS -> evaluate(speedTicks, level).coerceAtLeast(1)
            MinionStat.EFFICIENCY_TICKS -> evaluate(efficiencyTicks, level).coerceAtLeast(0)
            MinionStat.RADIUS -> evaluate(radius, level).coerceAtLeast(1)
            MinionStat.INVENTORY_ROWS -> evaluate(inventoryRows, level)
        }

        fun regenDelayAt(level: Int): Int = evaluate(regenDelayTicks, level).coerceAtLeast(1)
    }

    private companion object {
        const val PREVIEW_LEVEL = 1
    }
}
