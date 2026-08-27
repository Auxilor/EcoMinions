package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import io.mockk.mockk
import org.bukkit.entity.ArmorStand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinionFuelServiceTest {

    private val stats = MinionStats(
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

    /** Twenty ticks of burn, so one item is worth a round second. */
    private fun fuel(id: String) = Fuel(
        id = id,
        displayName = id,
        itemLookup = id,
        durationTicks = 20L,
        speedMultiplier = 2.0
    )

    private fun minion(shutdownTimestamp: Long? = null) = Minion(
        state = MinionState(
            definition = MinionDefinitionFixture.build(),
            shutdownTimestamp = shutdownTimestamp
        ),
        entity = mockk<ArmorStand>(relaxed = true),
        storage = MinionStorage(stats.storageSize),
        stats = stats
    )

    private fun service() = MinionFuelService(
        fuels = mockk<FuelService>(relaxed = true),
        repository = mockk<MinionStateRepository>(relaxed = true)
    )

    @Test
    fun `the first fuel starts the burn`() {
        val subject = minion()

        service().apply(subject, fuel("coal"), now = 1_000L)

        assertEquals("coal", subject.burningFuelId)
        assertEquals(1_000L, subject.burningFuelRemainingMillis)
        assertEquals(2_000L, subject.burningFuelEndsAt)
    }

    @Test
    fun `more of the same fuel adds to the time left`() {
        val subject = minion()
        val service = service()

        service.apply(subject, fuel("coal"), now = 1_000L)
        service.apply(subject, fuel("coal"), now = 1_400L)

        // 600ms was left of the first second when the second item went in.
        assertEquals(1_600L, subject.burningFuelRemainingMillis)
        assertEquals(3_000L, subject.burningFuelEndsAt)
    }

    @Test
    fun `a burn that already expired does not carry time forward`() {
        val subject = minion()
        val service = service()

        service.apply(subject, fuel("coal"), now = 1_000L)
        service.apply(subject, fuel("coal"), now = 9_000L)

        assertEquals(1_000L, subject.burningFuelRemainingMillis)
        assertEquals(10_000L, subject.burningFuelEndsAt)
    }

    @Test
    fun `a different fuel replaces the burn instead of stacking`() {
        val subject = minion()
        val service = service()

        service.apply(subject, fuel("coal"), now = 1_000L)
        service.apply(subject, fuel("blaze_rod"), now = 1_400L)

        assertEquals("blaze_rod", subject.burningFuelId)
        assertEquals(1_000L, subject.burningFuelRemainingMillis)
        assertEquals(2_400L, subject.burningFuelEndsAt)
    }

    @Test
    fun `fuelling an idle minion stays frozen and stacks on the remainder`() {
        val subject = minion(shutdownTimestamp = 500L)
        subject.burningFuelId = "coal"
        subject.burningFuelRemainingMillis = 700L

        service().apply(subject, fuel("coal"), now = 1_000L)

        assertEquals(1_700L, subject.burningFuelRemainingMillis)
        assertEquals(0L, subject.burningFuelEndsAt)
    }
}
