package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionExtractEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Moves a minion's storage, or one stack of it, into a player's inventory, or
 * all of it onto the ground at their feet.
 */
class MinionCollectService(
    private val repository: MinionStateRepository,
    private val events: EventDispatcher,
    private val withdrawn: (Player, Minion, ItemStack) -> Unit = { _, _, _ -> }
) {
    /**
     * Hands [player] everything in [minion]'s storage that fits in their
     * inventory; the rest stays in storage. Reports each stack that arrived to `withdrawn`, after saving.
     *
     * @return How many items were moved, or null if the extract event was cancelled.
     */
    fun collect(player: Player, minion: Minion): Int? = hand(player, minion, everything(minion))

    /**
     * Hands [player] up to one stack similar to [sample]; what does not fit
     * in their inventory stays in storage. Reports what arrived to `withdrawn`, after saving.
     *
     * @return How many items were moved, or null if the extract event was cancelled.
     */
    fun withdraw(player: Player, minion: Minion, sample: ItemStack): Int? {
        val amount = minion.storage.countSimilar(sample).coerceAtMost(sample.maxStackSize)
        val stack = sample.clone().apply { this.amount = amount }
        return hand(player, minion, listOfNotNull(stack.takeIf { amount > 0 }))
    }

    /**
     * Drops everything in [minion]'s storage on the ground at [player]'s feet.
     * Items the world refuses to spawn stay in storage.
     *
     * Reports nothing to `withdrawn`: dropped items can be picked up by anyone, or by a collector minion.
     *
     * @return How many items were dropped, or null if the extract event was cancelled.
     */
    fun drop(player: Player, minion: Minion): Int? = extract(player, minion, everything(minion)) { item ->
        if (player.world.dropItemNaturally(player.location, item).isValid) 0 else item.amount
    }?.moved

    private fun hand(player: Player, minion: Minion, stored: List<ItemStack>): Int? {
        val handover = extract(player, minion, stored) { item ->
            player.inventory.addItem(item).values.sumOf { it.amount }
        } ?: return null
        handover.delivered.forEach { withdrawn(player, minion, it) }
        return handover.moved
    }

    private fun everything(minion: Minion): List<ItemStack> =
        (0 until minion.storage.size).mapNotNull { minion.storage.getItem(it)?.clone() }

    private fun extract(
        player: Player,
        minion: Minion,
        stored: List<ItemStack>,
        deliver: (ItemStack) -> Int
    ): StorageHandover? {
        val event = MinionExtractEvent(player, minion, stored.toMutableList())
        return when {
            stored.isEmpty() -> StorageHandover.NONE
            !events.dispatch(event) -> null
            else -> minion.handOver(event.items, deliver).also { if (it.taken > 0) repository.save(minion) }
        }
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
