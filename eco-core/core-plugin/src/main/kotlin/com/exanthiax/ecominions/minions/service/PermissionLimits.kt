package com.exanthiax.ecominions.minions.service

import org.bukkit.entity.Player

/**
 * Permission overrides raise a limit; the most generous one wins.
 *
 * @param prefix Permission prefix whose suffix is the limit, for example `ecominions.limit.`.
 * @param default The limit with no matching permission.
 */
fun highestPermissionLimit(player: Player, prefix: String, default: Int): Int {
    var best = default
    for (info in player.effectivePermissions) {
        if (!info.value || !info.permission.startsWith(prefix)) {
            continue
        }
        val limit = info.permission.removePrefix(prefix).toIntOrNull() ?: continue
        if (limit > best) {
            best = limit
        }
    }
    return best
}
