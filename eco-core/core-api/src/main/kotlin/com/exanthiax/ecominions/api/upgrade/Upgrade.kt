package com.exanthiax.ecominions.api.upgrade

/** A purchasable minion upgrade, e.g. `speed` or `capacity`. */
interface Upgrade {
    /** Config file name, without extension. */
    val id: String

    /** Formatted display name. */
    val name: String

    /** Which stat this upgrade modifies. */
    val effect: String

    /** Highest tier defined in config. */
    val maxTier: Int
}
