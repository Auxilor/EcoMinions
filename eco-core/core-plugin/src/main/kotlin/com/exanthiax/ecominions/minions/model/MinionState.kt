package com.exanthiax.ecominions.minions.model

import java.util.UUID

/** The persisted identity of a minion: what survives pickup, restart and reload. */
data class MinionState(
    val definition: MinionDefinition,
    private val storedLevel: Int = 1,
    val xp: Double = 0.0,
    val owner: UUID? = null,
    val upgrades: Map<String, Int> = emptyMap(),
    val inventoryBytes: ByteArray? = null,
    val uuid: UUID? = null,
    val shutdownTimestamp: Long? = null,
    /** The fuel burning when this state was taken, so it survives pickup and restart. */
    val fuel: StoredFuel? = null
) {
    /**
     * Clamped on read so that lowering `max-level` in config demotes minions
     * cleanly, rather than stranding them above the top of the curve.
     */
    val level: Int
        get() = if (definition.levelling.enabled) {
            storedLevel.coerceIn(1, definition.levelling.maxLevel)
        } else {
            storedLevel
        }

    val isAtMaxLevel: Boolean
        get() = definition.levelling.enabled && level >= definition.levelling.maxLevel

    fun upgradeLevel(upgradeId: String): Int = upgrades[upgradeId] ?: 0

    /**
     * @return A copy with [upgradeId] set to [tier].
     *
     * Level is recalculated only for a type that does not level: with levelling
     * on, level is XP-driven and upgrades must not move it.
     */
    fun withUpgrade(upgradeId: String, tier: Int): MinionState {
        val updated = upgrades + (upgradeId to tier)
        if (definition.levelling.enabled) {
            return copy(upgrades = updated)
        }
        return copy(upgrades = updated, storedLevel = levelFor(updated))
    }

    /** @return A copy at [level] with [xp] banked toward the next one. */
    fun withXp(xp: Double, level: Int): MinionState =
        copy(xp = xp, storedLevel = level)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MinionState
        return definition.id == other.definition.id &&
            storedLevel == other.storedLevel &&
            xp == other.xp &&
            owner == other.owner &&
            upgrades == other.upgrades &&
            uuid == other.uuid &&
            shutdownTimestamp == other.shutdownTimestamp &&
            fuel == other.fuel &&
            (inventoryBytes?.contentEquals(other.inventoryBytes) ?: (other.inventoryBytes == null))
    }

    override fun hashCode(): Int {
        var result = definition.id.hashCode()
        result = 31 * result + storedLevel
        result = 31 * result + xp.hashCode()
        result = 31 * result + (owner?.hashCode() ?: 0)
        result = 31 * result + upgrades.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + (shutdownTimestamp?.hashCode() ?: 0)
        result = 31 * result + (fuel?.hashCode() ?: 0)
        result = 31 * result + (inventoryBytes?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        /** A minion's level is the sum of its purchased tiers, starting from one. */
        fun levelFor(upgrades: Map<String, Int>): Int = upgrades.values.sum() + 1
    }
}
