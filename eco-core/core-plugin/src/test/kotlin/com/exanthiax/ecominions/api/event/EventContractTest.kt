package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.menu.MinionMenuType
import com.exanthiax.ecominions.api.minion.DestroyCause
import com.exanthiax.ecominions.api.minion.Minion
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventContractTest {

    private val minion = mockk<Minion>(relaxed = true)
    private val player = mockk<Player>(relaxed = true)

    @Test
    fun `handler lists are shared per event type`() {
        assertSame(MinionDestroyEvent.getHandlerList(), MinionDestroyEvent(minion, DestroyCause.API).handlers)
        assertSame(MinionOpenEvent.getHandlerList(), MinionOpenEvent(player, minion, MinionMenuType.MAIN).handlers)
        assertSame(MinionStorageFullEvent.getHandlerList(), MinionStorageFullEvent(minion).handlers)
        val feed = MinionFeedEvent(minion, minion, mockk(relaxed = true), 1L)
        assertSame(MinionFeedEvent.getHandlerList(), feed.handlers)
        assertSame(
            MinionBreedEvent.getHandlerList(),
            MinionBreedEvent(minion, mockk(relaxed = true), mockk(relaxed = true), ItemStack(Material.WHEAT)).handlers
        )
        assertSame(
            MinionDepositEvent.getHandlerList(),
            MinionDepositEvent(player, minion, ItemStack(Material.COAL)).handlers
        )
    }

    @Test
    fun `cancellable events start uncancelled and remember cancelling`() {
        val event = MinionDestroyEvent(minion, DestroyCause.COMMAND)
        assertFalse(event.isCancelled)

        event.isCancelled = true

        assertTrue(event.isCancelled)
    }

    @Test
    fun `input events cancel`() {
        val events: List<Cancellable> = listOf(
            MinionFeedEvent(minion, minion, mockk(relaxed = true), 1000L),
            MinionBreedEvent(minion, mockk(relaxed = true), mockk(relaxed = true), ItemStack(Material.WHEAT)),
            MinionDepositEvent(player, minion, ItemStack(Material.COAL))
        )

        assertTrue(events.none { it.isCancelled })
        events.forEach { it.isCancelled = true }
        assertTrue(events.all { it.isCancelled })
    }

    @Test
    fun `feed duration is writable`() {
        val feed = MinionFeedEvent(minion, minion, mockk(relaxed = true), 1000L)
        feed.durationMillis = 250L
        assertEquals(250L, feed.durationMillis)

        assertSame(player, MinionDepositEvent(player, minion, ItemStack(Material.COAL)).player)
    }

    @Test
    fun `mutable fields are writable by listeners`() {
        val sell = MinionSellEvent(minion, 10.0, emptyList())
        sell.total = 25.0
        assertEquals(25.0, sell.total)

        val extract = MinionExtractEvent(player, minion, mutableListOf<ItemStack>())
        val replacement = mutableListOf<ItemStack>()
        extract.items = replacement
        assertSame(replacement, extract.items)
    }

    @Test
    fun `minion events expose their minion through the marker`() {
        val events: List<MinionEvent> = listOf(
            MinionDestroyEvent(minion, DestroyCause.API),
            MinionStorageFullEvent(minion),
            MinionLevelUpEvent(minion, 1, 2),
            MinionCollectItemEvent(minion, mockk(relaxed = true)),
            MinionFeedEvent(minion, minion, mockk(relaxed = true), 1000L),
            MinionBreedEvent(minion, mockk(relaxed = true), mockk(relaxed = true), ItemStack(Material.WHEAT)),
            MinionDepositEvent(player, minion, ItemStack(Material.COAL)),
            MinionKillEvent(minion, mockk(relaxed = true), mutableListOf(), 0),
            MinionCatchEvent(minion, mockk(relaxed = true), mutableListOf(), 0)
        )

        assertTrue(events.all { it.minion === minion })
    }

    @Test
    fun `reporting events are not cancellable`() {
        val reporting = listOf(MinionCloseEvent::class.java, MinionStorageFullEvent::class.java, MinionLevelUpEvent::class.java)

        assertTrue(reporting.none { Cancellable::class.java.isAssignableFrom(it) })
    }
}
