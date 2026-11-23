package com.exanthiax.ecominions.core.world

import org.bukkit.block.Block

/** Whether this block stops movement, read from its material: Spigot has no per-block check. */
val Block.isSolidBlock: Boolean
    get() = type.isSolid
