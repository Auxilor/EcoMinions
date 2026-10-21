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

/**
 * The caps one owner places under. [PlacementLimits.UNLIMITED] means no cap.
 *
 * @property known Whether the owner's permissions have ever been read. Owner limits never hold back
 *           the minions of an owner who has not been seen online since, as their caps could be higher.
 */
data class OwnerCaps(
    val total: Int,
    val types: Map<MinionType, Int>,
    val minions: Map<String, Int>,
    val known: Boolean = true
) {
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
 * Permission caps can only be read while the owner is online, so the last ones seen are saved in the
 * owner's data. Only the permission numbers are saved; the config defaults are applied on every read.
 */
class MinionLimits(
    private val settings: SettingsHolder<MinionSettings>,
    private val placed: PlacedMinionRegistry,
    private val types: (String) -> MinionDefinition?,
    private val allTypes: () -> Collection<MinionDefinition>,
    private val islandIndex: IslandMinionIndex,
    private val islandLimits: IslandLimits,
    private val islandAt: (Location) -> MinionIsland?,
    private val onlinePlayer: (UUID) -> Player?,
    private val readCaps: (UUID) -> List<String>,
    private val writeCaps: (UUID, List<String>) -> Unit
) : Tickable {
    override val tickInterval = 100L

    private val knownPermissions = ConcurrentHashMap<UUID, PermissionCaps>()
    private val ownerRanks = ConcurrentHashMap<UUID, Map<UUID, LimitBreach>>()
    private val islandRanks = ConcurrentHashMap<String, Map<UUID, LimitBreach>>()
    private val minionIslands = ConcurrentHashMap<UUID, List<MinionIsland>>()

    override fun tick() = invalidateAll()

    fun capsFor(owner: UUID): OwnerCaps {
        val permissions = onlinePlayer(owner)?.let(::refreshPermissions)
            ?: knownPermissions[owner]
            ?: PermissionCaps.decode(readCaps(owner))?.also { knownPermissions[owner] = it }
        return capsFrom(permissions)
    }

    fun refreshCaps(player: Player) {
        refreshPermissions(player)
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
        if (!caps.known) {
            return emptyMap()
        }
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

    private fun refreshPermissions(player: Player): PermissionCaps {
        val permissions = PermissionCaps.of(player)
        if (knownPermissions.put(player.uniqueId, permissions) != permissions) {
            writeCaps(player.uniqueId, permissions.encode())
        }
        return permissions
    }

    private fun capsFrom(permissions: PermissionCaps?): OwnerCaps {
        if (permissions?.admin == true) {
            return OwnerCaps(PlacementLimits.UNLIMITED, emptyMap(), emptyMap())
        }
        val current = settings.value
        val total = if (current.limitsEnabled) {
            maxOf(current.defaultLimit, permissions?.total?.takeIf { current.permissionOverrides } ?: 0)
        } else {
            PlacementLimits.UNLIMITED
        }
        val typeCaps = if (current.typeLimitsEnabled) {
            current.typeLimits.mapValues { (type, cap) ->
                maxOf(cap, permissions?.types?.get(type)?.takeIf { current.typeLimitOverrides } ?: 0)
            }
        } else {
            emptyMap()
        }
        val minionCaps = allTypes().mapNotNull { definition ->
            definition.limit?.let { definition.id to maxOf(it, permissions?.minions?.get(definition.id) ?: 0) }
        }.toMap()
        return OwnerCaps(total, typeCaps, minionCaps, known = permissions != null)
    }

    /** The limit numbers an owner's permissions grant, without any config default applied. */
    private data class PermissionCaps(
        val admin: Boolean,
        val total: Int?,
        val types: Map<MinionType, Int>,
        val minions: Map<String, Int>
    ) {
        fun encode(): List<String> = listOf(SEEN) +
            listOfNotNull(ADMIN.takeIf { admin }, total?.let { "$TOTAL$it" }) +
            types.map { (type, cap) -> "$TYPE${type.id}:$cap" } +
            minions.map { (id, cap) -> "$MINION$id:$cap" }

        companion object {
            private const val SEEN = "seen"
            private const val ADMIN = "admin"
            private const val TOTAL = "total:"
            private const val TYPE = "type:"
            private const val MINION = "minion:"

            fun of(player: Player): PermissionCaps {
                var total: Int? = null
                val types = HashMap<MinionType, Int>()
                val minions = HashMap<String, Int>()
                for (info in player.effectivePermissions) {
                    if (!info.value) {
                        continue
                    }
                    val permission = info.permission
                    when {
                        permission.startsWith(LIMIT_PREFIX) ->
                            permission.removePrefix(LIMIT_PREFIX).toIntOrNull()?.let { total = maxOf(total ?: 0, it) }
                        permission.startsWith(TYPE_LIMIT_PREFIX) -> permission.removePrefix(TYPE_LIMIT_PREFIX).splitCap()
                            ?.let { (id, cap) -> MinionType.entries.firstOrNull { it.id == id }?.let { types.merge(it, cap, ::maxOf) } }
                        permission.startsWith(MINION_LIMIT_PREFIX) -> permission.removePrefix(MINION_LIMIT_PREFIX).splitCap()
                            ?.let { (id, cap) -> minions.merge(id, cap, ::maxOf) }
                    }
                }
                return PermissionCaps(player.hasPermission(ADMIN_PERMISSION), total, types, minions)
            }

            fun decode(entries: List<String>): PermissionCaps? {
                if (SEEN !in entries) {
                    return null
                }
                return PermissionCaps(
                    admin = ADMIN in entries,
                    total = entries.firstOrNull { it.startsWith(TOTAL) }?.removePrefix(TOTAL)?.toIntOrNull(),
                    types = entries.filter { it.startsWith(TYPE) }.mapNotNull { it.removePrefix(TYPE).splitCap(":") }
                        .mapNotNull { (id, cap) -> MinionType.entries.firstOrNull { it.id == id }?.let { it to cap } }.toMap(),
                    minions = entries.filter { it.startsWith(MINION) }.mapNotNull { it.removePrefix(MINION).splitCap(":") }.toMap()
                )
            }

            private fun String.splitCap(separator: String = "."): Pair<String, Int>? =
                substringAfterLast(separator).toIntOrNull()?.let { substringBeforeLast(separator) to it }
        }
    }

    private companion object {
        const val ADMIN_PERMISSION = "ecominions.admin"
        const val LIMIT_PREFIX = "ecominions.limit."
        const val TYPE_LIMIT_PREFIX = "ecominions.type-limit."
        const val MINION_LIMIT_PREFIX = "ecominions.minion-limit."
    }
}
