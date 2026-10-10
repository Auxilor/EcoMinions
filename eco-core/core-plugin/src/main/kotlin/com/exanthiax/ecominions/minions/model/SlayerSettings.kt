package com.exanthiax.ecominions.minions.model

import com.willfp.eco.core.entities.TestableEntity

/** What a slayer kills. [maxMobs] may be a `%level%` expression. */
data class SlayerSettings(
    val targets: List<TestableEntity>,
    val maxMobs: String,
    val drops: MinionDropList,
    /**
     * Whether kills are made as the owner, so vanilla player-kill loot drops and
     * the owner's own `kill` effects fire. Off by default.
     */
    val creditOwner: Boolean = false
)
