package com.exanthiax.ecominions.api.ext

import com.exanthiax.ecominions.api.EcoMinionsApi
import com.exanthiax.ecominions.api.minion.Minion
import com.exanthiax.ecominions.api.minion.MinionDefinition
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

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

/** @return How many minion items were given. */
fun Player.giveMinion(type: MinionDefinition, level: Int = 1, amount: Int = 1): Int =
    EcoMinionsApi.get().minions().giveItem(this, type, level, amount)

/** @return How many minion items were taken. */
fun Player.takeMinions(type: MinionDefinition, amount: Int): Int =
    EcoMinionsApi.get().minions().takeItem(this, type, amount)
