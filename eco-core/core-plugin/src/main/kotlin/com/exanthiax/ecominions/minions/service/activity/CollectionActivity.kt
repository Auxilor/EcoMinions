package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.event.MinionCollectItemEvent
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionCollectItem
import com.exanthiax.ecominions.minions.model.CollectionSettings
import com.exanthiax.ecominions.minions.service.ExperienceSink
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.WorkAreaEntities
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.Item
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

/** Picks up dropped items and XP orbs in its box. Earns nothing offline: nothing drops while nobody is there. */
class CollectionActivity(
    private val entities: WorkAreaEntities,
    private val dispatch: MinionTriggerDispatch,
    private val events: EventDispatcher,
    private val experience: ExperienceSink,
    private val isMinionItem: (ItemStack) -> Boolean,
    private val listening: (HandlerList) -> Boolean = { it.registeredListeners.isNotEmpty() }
) : MinionActivity {

    override fun act(minion: Minion, now: Long): ActivityResult {
        val settings = minion.state.definition.collection ?: return ActivityResult.BLOCKED
        val items = entities.find(minion, Item::class.java).count { collect(minion, it, settings) }
        val orbs = if (settings.collectExperience) absorbOrbs(minion) else 0
        if (items + orbs > 0) {
            return ActivityResult.WORKED
        }
        minion.blockedReason = NOTHING_TO_COLLECT
        return ActivityResult.BUSY
    }

    override fun hasWork(minion: Minion): Boolean = true

    override fun offlineCycle(minion: Minion): OfflineYield? = null

    private fun collect(minion: Minion, item: Item, settings: CollectionSettings): Boolean {
        if (!item.isValid || item.pickupDelay > 0) {
            return false
        }
        val stack = item.itemStack
        if (isMinionItem(stack) || (settings.targetItems.isNotEmpty() && settings.targetItems.none { it.matches(stack) })) {
            return false
        }
        if (listening(MinionCollectItemEvent.getHandlerList()) && !events.dispatch(MinionCollectItemEvent(minion, item))) {
            return false
        }

        val leftover = dispatch.storeAndDispatchCollect(minion, listOf(stack.clone())).sumOf { it.amount }
        val stored = stack.amount - leftover
        if (stored <= 0) {
            return false
        }
        dispatch.collect(minion, TriggerMinionCollectItem, stack, stored.toDouble())
        if (leftover > 0) {
            item.itemStack = stack.clone().apply { amount = leftover }
        } else {
            item.remove()
        }
        return true
    }

    private fun absorbOrbs(minion: Minion): Int {
        val orbs = entities.find(minion, ExperienceOrb::class.java).filter { it.isValid }
        experience.store(minion, orbs.sumOf { it.experience })
        orbs.forEach { it.remove() }
        return orbs.size
    }

    private companion object {
        const val NOTHING_TO_COLLECT = "nothing-to-collect"
    }
}
