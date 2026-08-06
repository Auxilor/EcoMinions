package com.exanthiax.ecominions.api.upgrade

import com.exanthiax.ecominions.api.minion.Minion

/** Queries over configured upgrades and what minions have bought. */
interface UpgradesManager {
    fun all(): Collection<Upgrade>

    fun getById(id: String): Upgrade?

    /** @return Upgrades [minion]'s type is allowed to buy. */
    fun allowedFor(minion: Minion): List<Upgrade>

    /** @return The tier [minion] owns, or 0 if it has never bought this upgrade. */
    fun tierOf(minion: Minion, upgrade: Upgrade): Int

    /** @return The stat value [tier] grants, or null if that tier is not configured. */
    fun effectValue(upgrade: Upgrade, tier: Int): Double?
}
