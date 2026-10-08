package com.exanthiax.ecominions.api.impl

import com.exanthiax.ecominions.api.event.MinionOwnerChangeEvent
import com.exanthiax.ecominions.api.minion.DestroyCause
import com.exanthiax.ecominions.api.minion.PickUpResult
import com.exanthiax.ecominions.api.minion.PlaceResult
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionActivityService
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.PlacementService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class MinionsManagerImplTest {

    private val type = MinionDefinitionFixture.build()
    private val uuid = UUID.randomUUID()
    private val minion = mockk<Minion>(relaxed = true) {
        every { uuid } returns this@MinionsManagerImplTest.uuid
        every { state } returns MinionState(definition = this@MinionsManagerImplTest.type)
    }
    private val minions = mockk<MinionService>(relaxed = true) {
        every { getByUuid(uuid) } returns minion
        every { getType(type.id) } returns type
    }
    private val placement = mockk<PlacementService>(relaxed = true)
    private val itemFactory = mockk<MinionItemFactory>(relaxed = true)
    private val activity = mockk<MinionActivityService> { every { isActive(minion) } returns true }

    private fun manager(events: RecordingEventDispatcher = RecordingEventDispatcher()) = MinionsManagerImpl(
        minions, placement, itemFactory, mockk(relaxed = true), mockk(relaxed = true),
        mockk(relaxed = true), mockk(relaxed = true), activity, mockk(relaxed = true), mockk(relaxed = true), events,
        mockk(relaxed = true)
    ) { id -> mockk<OfflinePlayer> { every { uniqueId } returns id } }

    @Test
    fun `placing an item that carries no minion is refused`() {
        every { itemFactory.read(any()) } returns null

        val result = manager().place(mockk<ItemStack>(), mockk<Location>(), null)

        assertEquals(PlaceResult.Status.INVALID_ITEM, result.status)
        verify(exactly = 0) { placement.placeUnattended(any(), any(), any()) }
    }

    @Test
    fun `every internal place result maps to a public status`() {
        val player = mockk<Player>(relaxed = true)
        val location = mockk<Location>()
        val cases = mapOf(
            PlacementService.PlaceResult.Placed(minion) to PlaceResult.Status.PLACED,
            PlacementService.PlaceResult.WorldDisabled to PlaceResult.Status.WORLD_DISABLED,
            PlacementService.PlaceResult.LimitReached(3) to PlaceResult.Status.LIMIT_REACHED,
            PlacementService.PlaceResult.Cancelled to PlaceResult.Status.CANCELLED
        )

        for ((internal, expected) in cases) {
            every { placement.place(player, any(), location) } returns internal
            assertEquals(expected, manager().place(player, type, 1, location).status)
        }
    }

    @Test
    fun `a limit reached carries the limit`() {
        val player = mockk<Player>(relaxed = true)
        every { placement.place(any(), any(), any()) } returns PlacementService.PlaceResult.LimitReached(3)

        assertEquals(3, manager().place(player, type, 1, mockk()).limit)
    }

    @Test
    fun `a cancelled pickup gives nothing`() {
        val player = mockk<Player>(relaxed = true)
        every { placement.pickUp(player, minion) } returns PlacementService.PickUpResult.Cancelled

        val result = manager().pickUp(player, minion)

        assertEquals(PickUpResult.Status.CANCELLED, result.status)
        assertNull(result.item)
        verify(exactly = 0) { player.inventory.addItem(*anyVararg()) }
    }

    @Test
    fun `destroying without a drop delegates with the cause`() {
        every { placement.destroy(minion, DestroyCause.API, false) } returns true

        assertTrue(manager().destroy(minion, DestroyCause.API))
    }

    @Test
    fun `a cancelled owner change leaves the owner alone`() {
        val changed = manager(RecordingEventDispatcher(allow = false)).setOwner(minion, null)

        assertFalse(changed)
        verify(exactly = 0) { minions.changeOwner(any(), any()) }
    }

    @Test
    fun `a listener's new owner is the one applied`() {
        val redirected = UUID.randomUUID()
        val events = RecordingEventDispatcher { event ->
            (event as? MinionOwnerChangeEvent)?.newOwner = mockk { every { uniqueId } returns redirected }
        }

        manager(events).setOwner(minion, null)

        verify { minions.changeOwner(minion, redirected) }
    }

    @Test
    fun `activity comes from the owner rule`() {
        assertTrue(manager().isActive(minion))
    }
}
