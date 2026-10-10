package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.compactor.model.CompactorRecipeDefinition
import com.exanthiax.ecominions.levelling.model.LevellingSpec
import com.exanthiax.ecominions.api.minion.MinionType
import com.willfp.libreforge.Holder
import org.bukkit.Material
import com.exanthiax.ecominions.api.minion.MinionDefinition as ApiMinionDefinition
import kotlin.math.roundToInt

/**
 * A configured minion. Immutable, fully validated by the time it exists:
 * everything questionable was rejected by the loader.
 */
class MinionDefinition(
    override val id: String,
    override val name: String,
    val targetBlocks: TargetSet,
    override val allowedUpgradeIds: List<String>,
    override val type: MinionType,
    val baseStats: BaseStats,
    val spawnItem: SpawnItemSpec,
    val crafting: CraftingSpec?,
    val equipment: MinionEquipment,
    val particles: ParticleSettings,
    val placement: PlacementSettings,
    val prices: PriceList,
    /** Parsed from the type's `levelling:` block. Disabled unless configured. */
    val levelling: LevellingSpec = LevellingSpec.DISABLED,
    /** Compiled from the type's `effects:` block. Always active, no purchase needed. */
    val holder: Holder? = null,
    /**
     * Resolved from this type's `compactor-recipes:` list of catalog ids. Authored
     * order is the order the compactor menu upgrades through - each recipe is
     * still bought and gated independently, so this only controls display order.
     */
    val compactorRecipes: List<CompactorRecipeDefinition> = emptyList()
) : ApiMinionDefinition {

    /** Preview value shown before a minion's actual level is known, e.g. in the type-select menu. */
    override val baseSpeedTicks: Int
        get() = baseStats.valueOf(MinionStat.SPEED_TICKS, PREVIEW_LEVEL)

    override val baseStorageSlots: Int
        get() = baseStats.valueOf(MinionStat.INVENTORY_SLOTS, PREVIEW_LEVEL)

    override val targetBlockIds: Set<String>
        get() = targetBlocks.ids

    override val isLevellable: Boolean
        get() = levelling.enabled

    override val maxLevel: Int
        get() = levelling.maxLevel

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
        /**
         * The work box's vertical span in blocks, from the type's floor upward.
         * The loader fills an unset value from [defaultHeight], so the fallback
         * here only applies to stats built without a type.
         */
        val height: String = FALLBACK_HEIGHT,
        /** The first layer of the work box, relative to the minion. A whole number, never levelled. */
        val floorOffset: Int = FALLBACK_FLOOR_OFFSET,
        val storageSlots: String,
        val maxStorageSlots: String,
        val regenDelayTicks: String,
        /** `minion.hard-slot-cap` at load time - [maxStorageSlots] is clamped to it on every read. */
        val hardSlotCap: Int,
        /** Used only for [maxStorageSlots], which rounds rather than truncates - it is a slot count, not a rate. */
        val evaluateRounded: (String, Int) -> Int = { expression, _ -> expression.trim().toDouble().roundToInt() },
        val evaluate: (String, Int) -> Int = { expression, _ -> expression.trim().toInt() }
    ) {
        fun valueOf(stat: MinionStat, level: Int): Int = when (stat) {
            MinionStat.SPEED_TICKS -> evaluate(speedTicks, level).coerceAtLeast(1)
            MinionStat.EFFICIENCY_TICKS -> evaluate(efficiencyTicks, level).coerceAtLeast(0)
            MinionStat.RADIUS -> evaluate(radius, level).coerceAtLeast(1)
            MinionStat.HEIGHT -> evaluate(height, level).coerceAtLeast(1)
            MinionStat.INVENTORY_SLOTS -> evaluate(storageSlots, level).coerceIn(1, maxStorageSlotsAt(level))
        }

        fun regenDelayAt(level: Int): Int = evaluate(regenDelayTicks, level).coerceAtLeast(1)

        fun maxStorageSlotsAt(level: Int): Int = evaluateRounded(maxStorageSlots, level).coerceIn(1, hardSlotCap)
    }

    override fun toString(): String = "MinionDefinition(id=$id)"

    companion object {
        private const val PREVIEW_LEVEL = 1

        /** For stats built without a type; the three-block box every type once had. */
        const val FALLBACK_HEIGHT = "3"

        const val FALLBACK_FLOOR_OFFSET = -1

        /**
         * The `base-stats.height` a type gets when its config sets none.
         *
         * Sized to where each type's blocks sit above its floor: ore fills the ring
         * below the minion, a crop stands at the minion's own level, and a tree needs
         * room for its trunk - see [TreeShape.MAX_REACHABLE_TRUNK].
         */
        fun defaultHeight(type: MinionType): String = when (type) {
            MinionType.MINER -> "1"
            MinionType.FARMER -> "1"
            MinionType.WOODCUTTER -> "5"
        }

        /** Where a type's work box starts when `placement.floor-offset` is not set. */
        fun defaultFloorOffset(type: MinionType): Int = when (type) {
            MinionType.MINER -> -1
            MinionType.FARMER -> 0
            MinionType.WOODCUTTER -> 0
        }
    }
}
