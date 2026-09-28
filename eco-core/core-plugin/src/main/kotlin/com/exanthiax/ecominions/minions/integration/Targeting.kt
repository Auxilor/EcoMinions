package com.exanthiax.ecominions.minions.integration

import org.bukkit.FluidCollisionMode
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player

/** The armor stand in the player's line of sight, stopping at blocks. Spigot-safe. */
fun Player.targetedArmorStand(range: Double): ArmorStand? {
    val eye = eyeLocation
    val hit = world.rayTrace(
        eye,
        eye.direction,
        range,
        FluidCollisionMode.NEVER,
        true,
        0.0
    ) { it != this } ?: return null
    return hit.hitEntity as? ArmorStand
}
