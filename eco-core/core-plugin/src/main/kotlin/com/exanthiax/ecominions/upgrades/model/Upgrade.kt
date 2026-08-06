package com.exanthiax.ecominions.upgrades.model

import com.exanthiax.ecominions.api.upgrade.Upgrade as ApiUpgrade

/** A purchasable upgrade and its tier ladder. */
class Upgrade(
    override val id: String,
    override val name: String,
    override val effect: String,
    /** eco item lookup string for the menu icon. */
    val iconLookup: String,
    val tiers: Map<Int, UpgradeTier>
) : ApiUpgrade {

    override val maxTier: Int = tiers.keys.maxOrNull() ?: 0

    fun tier(level: Int): UpgradeTier? = tiers[level]

    /** @return The tier above [currentTier], or null if the ladder is exhausted. */
    fun nextTier(currentTier: Int): UpgradeTier? = tiers[currentTier + 1]

    /** Tiers in ascending order, for display. */
    fun orderedTiers(): List<UpgradeTier> = tiers.entries.sortedBy { it.key }.map { it.value }
}

/** One rung of an upgrade ladder. */
class UpgradeTier(
    val level: Int,
    /** How much the stat moves, interpreted by the stat resolver. */
    val value: Double,
    /** Admin-authored summary of the change, e.g. `-25 ticks`. */
    val statDelta: String,
    val costs: List<TierCost>
)
