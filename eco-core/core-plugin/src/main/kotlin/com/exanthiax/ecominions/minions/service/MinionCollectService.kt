package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionExtractEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** Moves a minion's storage into a player's inventory, or onto the ground at their feet. */
class MinionCollectService(
    private val repository: MinionStateRepository,
    private val events: EventDispatcher
) {
    /**
     * Hands [player] everything in [minion]'s storage that fits in their
     * inventory; the rest stays in storage.
     *
     * @return How many items were moved, or null if the extract event was cancelled.
     */
    fun collect(player: Player, minion: Minion): Int? = extract(player, minion) { item ->
        player.inventory.addItem(item).values.sumOf { it.amount }
    }

    /**
     * Drops everything in [minion]'s storage on the ground at [player]'s feet.
     * Items the world refuses to spawn stay in storage.
     *
     * @return How many items were dropped, or null if the extract event was cancelled.
     */
    fun drop(player: Player, minion: Minion): Int? = extract(player, minion) { item ->
        if (player.world.dropItemNaturally(player.location, item).isValid) 0 else item.amount
    }

    private fun extract(player: Player, minion: Minion, deliver: (ItemStack) -> Int): Int? {
        val stored = (0 until minion.storage.size).mapNotNull { minion.storage.getItem(it)?.clone() }
        if (stored.isEmpty()) {
            return 0
        }

        val event = MinionExtractEvent(player, minion, stored.toMutableList())
        return if (events.dispatch(event)) move(minion, event.items, deliver) else null
    }

    private fun move(minion: Minion, items: List<ItemStack>, deliver: (ItemStack) -> Int): Int {
        var moved = 0
        for (item in items) {
            val removed = minion.storage.remove(item)
            if (removed <= 0) {
                continue
            }
            val undelivered = deliver(item.clone().apply { amount = removed })
            moved += removed - undelivered
            if (undelivered > 0) {
                minion.storage.add(item.clone().apply { amount = undelivered })
            }
        }

        repository.save(minion)
        return moved
    }

    /**
     * Gives [player] the XP [minion] has stored, and empties the store.
     *
     * @return How much was given.
     */
    fun claimExperience(player: Player, minion: Minion): Int {
        val amount = minion.state.storedExperience
        if (amount <= 0) {
            return 0
        }
        minion.state = minion.state.withStoredExperience(0)
        player.giveExp(amount)
        repository.save(minion)
        return amount
    }
}
