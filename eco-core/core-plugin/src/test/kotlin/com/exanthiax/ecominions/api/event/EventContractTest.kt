package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.menu.MinionMenuType
import com.exanthiax.ecominions.api.minion.DestroyCause
import com.exanthiax.ecominions.api.minion.Minion
import io.mockk.mockk
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
    }

    @Test
    fun `cancellable events start uncancelled and remember cancelling`() {
        val event = MinionDestroyEvent(minion, DestroyCause.COMMAND)
        assertFalse(event.isCancelled)

        event.isCancelled = true

        assertTrue(event.isCancelled)
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
            MinionKillEvent(minion, mockk(relaxed = true), mutableListOf(), 0)
        )

        assertTrue(events.all { it.minion === minion })
    }

    @Test
    fun `reporting events are not cancellable`() {
        val reporting = listOf(MinionCloseEvent::class.java, MinionStorageFullEvent::class.java, MinionLevelUpEvent::class.java)

        assertTrue(reporting.none { Cancellable::class.java.isAssignableFrom(it) })
    }
}
