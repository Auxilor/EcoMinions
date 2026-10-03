package com.exanthiax.ecominions.core.world

import com.willfp.libreforge.loader.LibreforgePlugin
import org.bukkit.World

/** Where minions are allowed to exist. Shared by placement, mining and effects. */
class WorldSettings(
    private val plugin: LibreforgePlugin
) {
    fun isDisabled(world: World): Boolean = plugin.isDisabledIn(world)
}
