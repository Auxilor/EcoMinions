package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.compactor.model.CompactorRecipe
import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.exanthiax.ecominions.upgrades.model.Upgrade
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
    val holder: Holder? = null,
    /**
     * Resolved from this type's `compactor-tiers:` list, where position (1-based)
     * becomes the tier's level; null if the type declares no compactor ladder.
     */
    val compactorUpgrade: Upgrade? = null,
    /** This type's compactor ladder recipes, keyed by tier level. */
    val compactorRecipesByTier: Map<Int, List<CompactorRecipe>> = emptyMap()
) : ApiMinionType {

    /** Preview value shown before a minion's actual level is known, e.g. in the type-select menu. */
    override val baseSpeedTicks: Int
        get() = baseStats.valueOf(MinionStat.SPEED_TICKS, PREVIEW_LEVEL)

    override val baseStorageSlots: Int
        get() = baseStats.valueOf(MinionStat.INVENTORY_SLOTS, PREVIEW_LEVEL)

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
        val storageSlots: String,
        val maxStorageSlots: Int,
        val regenDelayTicks: String,
        val evaluate: (String, Int) -> Int = { expression, _ -> expression.trim().toInt() }
    ) {
        fun valueOf(stat: MinionStat, level: Int): Int = when (stat) {
            MinionStat.SPEED_TICKS -> evaluate(speedTicks, level).coerceAtLeast(1)
            MinionStat.EFFICIENCY_TICKS -> evaluate(efficiencyTicks, level).coerceAtLeast(0)
            MinionStat.RADIUS -> evaluate(radius, level).coerceAtLeast(1)
            MinionStat.INVENTORY_SLOTS -> evaluate(storageSlots, level).coerceAtLeast(1)
        }

        fun regenDelayAt(level: Int): Int = evaluate(regenDelayTicks, level).coerceAtLeast(1)
    }

    private companion object {
        const val PREVIEW_LEVEL = 1
    }
}
