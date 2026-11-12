package com.exanthiax.ecominions.core.world

import org.bukkit.World
import wiki.ecohub.core.plugin.EcoPlugin

/** Where minions are allowed to exist. Shared by placement, mining and effects. */
class WorldSettings(
    private val plugin: LibreforgePlugin
) {
    fun isDisabled(world: World): Boolean = plugin.isDisabledIn(world)
}
