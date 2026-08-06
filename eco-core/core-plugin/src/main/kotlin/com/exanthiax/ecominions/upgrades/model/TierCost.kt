package com.exanthiax.ecominions.upgrades.model

import org.bukkit.entity.Player

/**
 * One way of paying for an upgrade tier.
 *
 * A port, not an implementation: the concrete cost is eco's `ConfiguredPrice`,
 * wired in `upgrades/integration`. Keeping the interface here is what lets the
 * purchase flow be tested without a price system.
 */
interface TierCost {
    fun canAfford(player: Player): Boolean

    fun pay(player: Player)

    /** Formatted for display, e.g. `$1,000` or `32x Stone`. */
    fun display(player: Player): String
}
