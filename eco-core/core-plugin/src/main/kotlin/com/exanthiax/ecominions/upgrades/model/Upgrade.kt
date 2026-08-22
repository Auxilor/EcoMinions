package com.exanthiax.ecominions.upgrades.model

import com.exanthiax.ecominions.api.upgrade.Upgrade as ApiUpgrade
import com.willfp.libreforge.Holder
import com.willfp.libreforge.conditions.ConditionList

/** A purchasable upgrade and its tier ladder. */
class Upgrade(
    override val id: String,
    override val name: String,
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

    /**
     * @return The effect holder for the tier a minion actually owns, or null if
     *   that tier is unowned, unconfigured, or declares no effects.
     */
    fun holderFor(tier: Int): Holder? = tiers[tier]?.holder
}

/** One rung of an upgrade ladder. */
class UpgradeTier(
    val level: Int,
    /** Whatever `%tier_value%` resolves to for this rung. */
    val value: Double,
    /** Admin-authored summary of the change, e.g. `-25 ticks`. */
    val statDelta: String,
    val costs: List<TierCost>,
    /** Compiled from the upgrade's `effects:` block with this tier's value bound. */
    val holder: Holder?,
    /** Gates whether this tier can be bought at all, checked before affordability. */
    val buyConditions: ConditionList = ConditionList(emptyList())
)
