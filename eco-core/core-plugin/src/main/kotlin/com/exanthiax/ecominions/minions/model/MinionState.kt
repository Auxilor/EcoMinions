package com.exanthiax.ecominions.minions.model

import java.util.UUID

/** The persisted identity of a minion: what survives pickup, restart and reload. */
data class MinionState(
    val type: MinionType,
    val level: Int = 1,
    val owner: UUID? = null,
    val upgrades: Map<String, Int> = emptyMap(),
    val inventoryBytes: ByteArray? = null,
    val uuid: UUID? = null,
    val shutdownTimestamp: Long? = null
) {
    fun upgradeLevel(upgradeId: String): Int = upgrades[upgradeId] ?: 0

    /** @return A copy with [upgradeId] set to [tier], and the level recalculated. */
    fun withUpgrade(upgradeId: String, tier: Int): MinionState {
        val updated = upgrades + (upgradeId to tier)
        return copy(upgrades = updated, level = levelFor(updated))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MinionState
        return type.id == other.type.id &&
            level == other.level &&
            owner == other.owner &&
            upgrades == other.upgrades &&
            uuid == other.uuid &&
            shutdownTimestamp == other.shutdownTimestamp &&
            (inventoryBytes?.contentEquals(other.inventoryBytes) ?: (other.inventoryBytes == null))
    }

    override fun hashCode(): Int {
        var result = type.id.hashCode()
        result = 31 * result + level
        result = 31 * result + (owner?.hashCode() ?: 0)
        result = 31 * result + upgrades.hashCode()
        result = 31 * result + (uuid?.hashCode() ?: 0)
        result = 31 * result + (shutdownTimestamp?.hashCode() ?: 0)
        result = 31 * result + (inventoryBytes?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        /** A minion's level is the sum of its purchased tiers, starting from one. */
        fun levelFor(upgrades: Map<String, Int>): Int = upgrades.values.sum() + 1
    }
}
