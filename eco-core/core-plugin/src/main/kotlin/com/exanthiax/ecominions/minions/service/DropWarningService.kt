package com.exanthiax.ecominions.minions.service

import org.bukkit.Material
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * Warns admins once an hour per minion and material when drops are discarded for
 * having no configured sell price, rather than flooding the console every tick.
 */
class DropWarningService(
    private val logger: Logger
) {
    private val lastWarning = ConcurrentHashMap<Pair<UUID, Material>, Long>()

    fun warnUnsellable(minion: Minion, material: Material) {
        val key = minion.uuid to material
        val now = System.currentTimeMillis()
        val last = lastWarning[key]
        if (last != null && now - last < WINDOW_MILLIS) {
            return
        }
        lastWarning[key] = now
        logger.warning(
            "Minion ${minion.uuid} (${minion.state.type.id}) produced ${material.name} " +
                "but no sell-price is configured for it. Drop discarded."
        )
    }

    private companion object {
        const val WINDOW_MILLIS = 3_600_000L
    }
}
