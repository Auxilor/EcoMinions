package com.exanthiax.ecominions.api.upgrade

import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.entity.Player

/** Configured upgrades, and the tiers minions own. */
interface UpgradesManager {
    fun all(): Collection<Upgrade>

    fun getById(id: String): Upgrade?

    /** @return Upgrades [minion]'s type is allowed to buy. */
    fun allowedFor(minion: Minion): List<Upgrade>

    /** @return The tier [minion] owns, or 0 if it has never bought this upgrade. */
    fun tierOf(minion: Minion, upgrade: Upgrade): Int

    /** @return The stat value [tier] grants, or null if that tier is not configured. */
    fun effectValue(upgrade: Upgrade, tier: Int): Double?

    /**
     * Sets [minion]'s tier of [upgrade] without charging anything. 0 removes it.
     * Fires `MinionUpgradeEvent` with no player.
     *
     * @return Whether the tier changed. False if [tier] is outside 0 to the
     *         upgrade's highest tier, or the event was cancelled.
     */
    fun setTier(minion: Minion, upgrade: Upgrade, tier: Int): Boolean

    /**
     * Buys [tier] of [upgrade] for [minion], charging [player] as the upgrade
     * menu does. Only the next tier up can be bought. When the tier has more
     * than one cost, the first one [player] can afford is used.
     * Fires `MinionUpgradeEvent`.
     */
    fun purchase(player: Player, minion: Minion, upgrade: Upgrade, tier: Int): PurchaseResult

    /** Sets every upgrade [minion] owns back to 0. Fires `MinionUpgradeEvent` once per upgrade. */
    fun resetUpgrades(minion: Minion)
}
