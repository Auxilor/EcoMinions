package com.exanthiax.ecominions.api.impl

import com.exanthiax.ecominions.api.event.MinionFuelEvent
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionFuelService
import com.exanthiax.ecominions.minions.service.MinionService
import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.ArmorStand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import com.exanthiax.ecominions.api.minion.Minion as ApiMinion

class FuelsManagerImplTest {

    private val coal = Fuel(id = "coal", displayName = "Coal", itemLookup = "coal", durationTicks = 20L, speedMultiplier = 2.0)

    private val minion = Minion(
        state = MinionState(definition = MinionDefinitionFixture.build()),
        entity = mockk<ArmorStand>(relaxed = true),
        storage = MinionStorage(1),
        stats = MinionStats(
            MinionDefinition.BaseStats(
                speedTicks = "40",
                efficiencyTicks = "10",
                radius = "2",
                storageSlots = "1",
                maxStorageSlots = "54",
                hardSlotCap = 54,
                regenDelayTicks = "60"
            ),
            level = 1
        )
    )

    private val fuels = mockk<FuelService>(relaxed = true) { every { getById("coal") } returns coal }
    private val minions = mockk<MinionService> {
        every { getByUuid(minion.uuid) } returns minion
        every { getByUuid(neq(minion.uuid)) } returns null
    }

    private fun manager(events: RecordingEventDispatcher) = FuelsManagerImpl(
        fuels,
        MinionFuelService(fuels, mockk(relaxed = true), mockk(relaxed = true), events),
        minions
    )

    @Test
    fun `one unit of fuel burns for its configured duration`() {
        val events = RecordingEventDispatcher()

        assertTrue(manager(events).applyFuel(minion, coal))

        assertEquals(1_000L, events.last<MinionFuelEvent>().durationMillis)
        assertNull(events.last<MinionFuelEvent>().player)
        assertEquals(1_000L, minion.burningFuelRemainingMillis)
    }

    @Test
    fun `a listener's duration is the one burnt`() {
        val events = RecordingEventDispatcher { (it as? MinionFuelEvent)?.durationMillis = 250L }

        manager(events).applyFuel(minion, coal, 500L)

        assertEquals(250L, minion.burningFuelRemainingMillis)
    }

    @Test
    fun `a cancelled fuelling burns nothing`() {
        val applied = manager(RecordingEventDispatcher(allow = false)).applyFuel(minion, coal)

        assertFalse(applied)
        assertNull(minion.burningFuelId)
    }

    @Test
    fun `clearing puts the burn out`() {
        val manager = manager(RecordingEventDispatcher())
        manager.applyFuel(minion, coal)

        manager.clearFuel(minion)

        assertNull(minion.burningFuelId)
        assertEquals(0L, minion.burningFuelRemainingMillis)
    }

    @Test
    fun `an unloaded minion is refused`() {
        val stranger = mockk<ApiMinion> { every { uuid } returns UUID.randomUUID() }

        assertThrows<IllegalArgumentException> { manager(RecordingEventDispatcher()).applyFuel(stranger, coal) }
    }
}
