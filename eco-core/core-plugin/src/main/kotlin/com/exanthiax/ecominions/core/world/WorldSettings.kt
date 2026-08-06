package com.exanthiax.ecominions.core.world

import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.World

/** Where minions are allowed to exist. Shared by placement and mining. */
class WorldSettings(
    private val config: Config
) {
    fun isDisabled(world: World): Boolean = world.name in config.getStrings("worlds.disabled")
}
