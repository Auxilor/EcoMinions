package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionExtractEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
import org.bukkit.entity.Player

/** Moves a minion's storage into a player's inventory. */
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
    fun collect(player: Player, minion: Minion): Int? {
        val stored = (0 until minion.storage.size).mapNotNull { minion.storage.getItem(it)?.clone() }
        if (stored.isEmpty()) {
            return 0
        }

        val event = MinionExtractEvent(player, minion, stored.toMutableList())
        if (!events.dispatch(event)) {
            return null
        }

        var collected = 0
        for (item in event.items) {
            val removed = minion.storage.remove(item)
            if (removed <= 0) {
                continue
            }
            val leftover = player.inventory.addItem(item.clone().apply { amount = removed }).values.sumOf { it.amount }
            collected += removed - leftover
            if (leftover > 0) {
                minion.storage.add(item.clone().apply { amount = leftover })
            }
        }

        repository.save(minion)
        return collected
    }
}
