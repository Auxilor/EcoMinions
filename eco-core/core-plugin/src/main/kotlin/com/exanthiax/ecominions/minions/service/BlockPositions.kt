package com.exanthiax.ecominions.minions.service

import org.bukkit.Location
import org.bukkit.World

/** Packs a block position into one long, in the same layout as Minecraft's own block positions. */
object BlockPositions {
    fun pack(location: Location): Long =
        ((location.blockX.toLong() and XZ_MASK) shl X_SHIFT) or
            ((location.blockZ.toLong() and XZ_MASK) shl Z_SHIFT) or
            (location.blockY.toLong() and Y_MASK)

    fun unpack(world: World?, packed: Long): Location =
        Location(
            world,
            (packed shr X_SHIFT).toDouble(),
            (packed shl Y_SIGN_SHIFT shr Y_SIGN_SHIFT).toDouble(),
            (packed shl Z_SIGN_SHIFT shr X_SHIFT).toDouble()
        )

    private const val XZ_MASK = 0x3FFFFFFL
    private const val Y_MASK = 0xFFFL
    private const val X_SHIFT = 38
    private const val Z_SHIFT = 12
    private const val Y_SIGN_SHIFT = 52
    private const val Z_SIGN_SHIFT = 26
}
