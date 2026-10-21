package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.island.MinionIsland
import com.exanthiax.ecominions.api.minion.LimitBreach
import com.exanthiax.ecominions.api.minion.LimitScope
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.lifecycle.Tickable
import com.exanthiax.ecominions.islands.IslandLimits
import com.exanthiax.ecominions.islands.IslandMinionIndex
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.OverLimitMode
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** The caps one owner places under. [PlacementLimits.UNLIMITED] means no cap. */
data class OwnerCaps(val total: Int, val types: Map<MinionType, Int>, val minions: Map<String, Int>) {
    fun typeCap(type: MinionType): Int = types[type] ?: PlacementLimits.UNLIMITED

    fun minionCap(id: String): Int = minions[id] ?: PlacementLimits.UNLIMITED
}

/**
 * Every placement limit: per player, per island, per type and per minion.
 *
 * In `inactive` mode each owner's minions are ranked in placement order, from the owner's
 * saved data so nothing needs loading. A minion over a cap does not use up room under any
 * other owner cap. Islands are ranked on their own, as island entries carry no owner.
 *
 * Permission caps can only be read while the owner is online, so the last ones seen are kept.
 */
class MinionLimits(
    private val settings: SettingsHolder<MinionSettings>,
    private val placementLimits: PlacementLimits,
    private val placed: PlacedMinionRegistry,
    private val types: (String) -> MinionDefinition?,
    private val allTypes: () -> Collection<MinionDefinition>,
    private val islandIndex: IslandMinionIndex,
    private val islandLimits: IslandLimits,
    private val islandAt: (Location) -> MinionIsland?,
    private val onlinePlayer: (UUID) -> Player?
) : Tickable {
    override val tickInterval = 100L

    private val knownCaps = ConcurrentHashMap<UUID, OwnerCaps>()
    private val ownerRanks = ConcurrentHashMap<UUID, Map<UUID, LimitBreach>>()
    private val islandRanks = ConcurrentHashMap<String, Map<UUID, LimitBreach>>()
    private val minionIslands = ConcurrentHashMap<UUID, List<MinionIsland>>()

    override fun tick() = invalidateAll()

    fun capsFor(owner: UUID): OwnerCaps =
        onlinePlayer(owner)?.let { player -> capsOf(player).also { knownCaps[owner] = it } }
            ?: knownCaps[owner]
            ?: defaultCaps()

    fun refreshCaps(player: Player) {
        knownCaps[player.uniqueId] = capsOf(player)
    }

    fun countOf(owner: UUID, scope: LimitScope, definition: MinionDefinition): Int = when (scope) {
        LimitScope.PLAYER -> placed.count(owner)
        LimitScope.ISLAND -> 0
        LimitScope.TYPE -> countOfType(owner, definition.type)
        LimitScope.MINION -> placed.count(owner, definition.id)
    }

    fun countOfType(owner: UUID, type: MinionType): Int = placed.entries(owner).count { types(it.typeId)?.type == type }

    /**
     * The first limit placing one more [definition] for [owner] would pass, in the order player, island, type, minion.
     *
     * @param island Looked up only when island limits are on.
     */
    fun checkPlacement(placer: Player, owner: UUID, definition: MinionDefinition, island: () -> MinionIsland?): LimitBreach? {
        val caps = capsFor(owner)
        val placedCount = countOf(owner, LimitScope.PLAYER, definition)
        if (placedCount >= caps.total) {
            return LimitBreach(LimitScope.PLAYER, placedCount + 1, caps.total)
        }
        if (settings.value.islandLimitsEnabled && !placer.hasPermission(ADMIN_PERMISSION)) {
            island()?.takeIf { islandLimits.isFull(it) }?.let {
                return LimitBreach(LimitScope.ISLAND, islandIndex.count(it) + 1, islandLimits.limitFor(it))
            }
        }
        val typeCount = countOf(owner, LimitScope.TYPE, definition)
        if (typeCount >= caps.typeCap(definition.type)) {
            return LimitBreach(LimitScope.TYPE, typeCount + 1, caps.typeCap(definition.type))
        }
        val minionCount = countOf(owner, LimitScope.MINION, definition)
        if (minionCount >= caps.minionCap(definition.id)) {
            return LimitBreach(LimitScope.MINION, minionCount + 1, caps.minionCap(definition.id))
        }
        return null
    }

    /** The limit holding [minion] back, or null when none does or the mode is `prevent`. */
    fun breachOf(minion: Minion): LimitBreach? {
        if (settings.value.overLimit != OverLimitMode.INACTIVE) {
            return null
        }
        val owner = minion.state.owner ?: return null
        val ownerBreach = ownerRanks.computeIfAbsent(owner, ::rankOwner)[minion.uuid]
        if (ownerBreach?.scope == LimitScope.PLAYER) {
            return ownerBreach
        }
        return islandBreachOf(minion) ?: ownerBreach
    }

    fun inactiveCount(owner: UUID): Int {
        if (settings.value.overLimit != OverLimitMode.INACTIVE) {
            return 0
        }
        return ownerRanks.computeIfAbsent(owner, ::rankOwner).size
    }

    /** Whether any limit beyond the per-player and per-island caps, or `inactive` mode, is in use. */
    fun inUse(): Boolean {
        val current = settings.value
        return current.overLimit == OverLimitMode.INACTIVE || current.typeLimitsEnabled || allTypes().any { it.limit != null }
    }

    fun invalidate(owner: UUID) {
        ownerRanks.remove(owner)
        islandRanks.clear()
    }

    fun invalidateAll() {
        ownerRanks.clear()
        islandRanks.clear()
        minionIslands.clear()
    }

    private fun islandBreachOf(minion: Minion): LimitBreach? {
        if (!settings.value.islandLimitsEnabled) {
            return null
        }
        val island = minionIslands.computeIfAbsent(minion.uuid) { listOfNotNull(islandAt(minion.entity.location)) }
            .firstOrNull() ?: return null
        return islandRanks.computeIfAbsent("${island.provider}:${island.id}") { rankIsland(island) }[minion.uuid]
    }

    private fun rankIsland(island: MinionIsland): Map<UUID, LimitBreach> {
        val minions = islandIndex.minionsOn(island)
        val cap = islandLimits.limitFor(island)
        return minions.drop(cap).associateWith { LimitBreach(LimitScope.ISLAND, minions.size, cap) }
    }

    private fun rankOwner(owner: UUID): Map<UUID, LimitBreach> {
        val caps = capsFor(owner)
        val entries = placed.entries(owner)
        val typeCounts = entries.groupingBy { types(it.typeId)?.type }.eachCount()
        val minionCounts = entries.groupingBy { it.typeId }.eachCount()
        var allowedTotal = 0
        val allowedTypes = HashMap<MinionType, Int>()
        val allowedMinions = HashMap<String, Int>()
        val breaches = HashMap<UUID, LimitBreach>()

        for (entry in entries) {
            val definition = types(entry.typeId)
            val breach = when {
                allowedTotal >= caps.total -> LimitBreach(LimitScope.PLAYER, entries.size, caps.total)
                definition == null -> null
                (allowedTypes[definition.type] ?: 0) >= caps.typeCap(definition.type) ->
                    LimitBreach(LimitScope.TYPE, typeCounts.getValue(definition.type), caps.typeCap(definition.type))
                (allowedMinions[definition.id] ?: 0) >= caps.minionCap(definition.id) ->
                    LimitBreach(LimitScope.MINION, minionCounts.getValue(definition.id), caps.minionCap(definition.id))
                else -> null
            }
            if (breach != null) {
                breaches[entry.minion] = breach
                continue
            }
            allowedTotal++
            if (definition != null) {
                allowedTypes.merge(definition.type, 1, Int::plus)
                allowedMinions.merge(definition.id, 1, Int::plus)
            }
        }
        return breaches
    }

    private fun capsOf(player: Player): OwnerCaps {
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return OwnerCaps(PlacementLimits.UNLIMITED, emptyMap(), emptyMap())
        }
        val current = settings.value
        val typeCaps = if (current.typeLimitsEnabled) {
            current.typeLimits.mapValues { (type, cap) ->
                if (current.typeLimitOverrides) highestPermissionLimit(player, "$TYPE_LIMIT_PREFIX${type.id}.", cap) else cap
            }
        } else {
            emptyMap()
        }
        val minionCaps = allTypes().mapNotNull { definition ->
            definition.limit?.let { definition.id to highestPermissionLimit(player, "$MINION_LIMIT_PREFIX${definition.id}.", it) }
        }.toMap()
        return OwnerCaps(placementLimits.limitFor(player), typeCaps, minionCaps)
    }

    private fun defaultCaps(): OwnerCaps {
        val current = settings.value
        return OwnerCaps(
            total = if (current.limitsEnabled) current.defaultLimit else PlacementLimits.UNLIMITED,
            types = if (current.typeLimitsEnabled) current.typeLimits else emptyMap(),
            minions = allTypes().mapNotNull { definition -> definition.limit?.let { definition.id to it } }.toMap()
        )
    }

    private companion object {
        const val ADMIN_PERMISSION = "ecominions.admin"
        const val TYPE_LIMIT_PREFIX = "ecominions.type-limit."
        const val MINION_LIMIT_PREFIX = "ecominions.minion-limit."
    }
}
