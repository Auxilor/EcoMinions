package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.api.island.MinionIsland
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.service.highestPermissionLimit
import org.bukkit.entity.Player
import java.util.UUID

/**
 * How many minions an island may hold.
 *
 * The limit comes from the island owner's permissions, so it is only known while the owner is
 * online. Each answer is cached on the [IslandMinionIndex] and used while the owner is away, and
 * an island never seen with its owner online gets the default rather than no limit.
 */
class IslandLimits(
    private val settings: SettingsHolder<MinionSettings>,
    private val index: IslandMinionIndex,
    private val onlinePlayer: (UUID) -> Player?
) {
    fun limitFor(island: MinionIsland): Int {
        val current = settings.value
        if (!current.islandLimitsEnabled) {
            return Int.MAX_VALUE
        }
        val owner = island.owner?.let(onlinePlayer) ?: return index.cachedLimit(island) ?: current.defaultIslandLimit
        val limit = if (current.islandPermissionOverrides) {
            highestPermissionLimit(owner, LIMIT_PREFIX, current.defaultIslandLimit)
        } else {
            current.defaultIslandLimit
        }
        index.cacheLimit(island, limit)
        return limit
    }

    fun isFull(island: MinionIsland): Boolean = index.count(island) >= limitFor(island)

    private companion object {
        const val LIMIT_PREFIX = "ecominions.island-limit."
    }
}
