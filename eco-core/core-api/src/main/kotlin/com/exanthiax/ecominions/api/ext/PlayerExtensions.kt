package com.exanthiax.ecominions.api.ext

import com.exanthiax.ecominions.api.EcoMinionsApi
import com.exanthiax.ecominions.api.minion.Minion
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity

/** @return The minions this player owns that are currently loaded. */
fun OfflinePlayer.minions(): Collection<Minion> =
    EcoMinionsApi.get().minions().ownedBy(this)

/** @return How many loaded minions this player owns. */
fun OfflinePlayer.minionCount(): Int =
    EcoMinionsApi.get().minions().countOwnedBy(this)

/** @return How many minions this player may place, or [Int.MAX_VALUE] if unlimited. */
fun OfflinePlayer.minionLimit(): Int =
    EcoMinionsApi.get().minions().placementLimit(this)

/** @return The minion backed by this entity, or null if it is not a minion. */
fun Entity.asMinion(): Minion? =
    EcoMinionsApi.getOrNull()?.minions()?.getByEntity(this)
