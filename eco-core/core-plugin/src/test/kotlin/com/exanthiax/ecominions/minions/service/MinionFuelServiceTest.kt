package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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
        repository = mockk<MinionStateRepository>(relaxed = true),
        triggers = mockk<MinionTriggerDispatch>(relaxed = true),
        events = RecordingEventDispatcher()
    )

    /** A service whose fuels are keyed off material, so stacks can stand in. */
    private fun serviceWith(vararg fuels: Pair<Material, Fuel>): MinionFuelService {
        val byMaterial = fuels.toMap()
        val lookup = mockk<FuelService>(relaxed = true)
        every { lookup.matching(any()) } answers { byMaterial[firstArg<ItemStack>().type] }
        return MinionFuelService(
            fuels = lookup,
            repository = mockk(relaxed = true),
            triggers = mockk(relaxed = true),
            events = RecordingEventDispatcher()
        )
    }

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

    @Test
    fun `a deposit of many items is worth many times the burn`() {
        val subject = minion()

        service().apply(subject, fuel("coal"), count = 5, now = 1_000L)

        assertEquals(5_000L, subject.burningFuelRemainingMillis)
        assertEquals(6_000L, subject.burningFuelEndsAt)
    }

    @Test
    fun `taking one fuel leaves the rest of the stack`() {
        val service = serviceWith(Material.COAL to fuel("coal"))
        val contents = arrayOf<ItemStack?>(ItemStack(Material.COAL, 8))

        val taken = service.take(contents, minion(), limit = 1)

        assertEquals(1, taken?.count)
        assertEquals(7, contents[0]?.amount)
    }

    @Test
    fun `an unlimited deposit drains every stack of the one fuel`() {
        val service = serviceWith(Material.COAL to fuel("coal"))
        val contents = arrayOf<ItemStack?>(
            ItemStack(Material.COAL, 8),
            ItemStack(Material.DIRT, 3),
            ItemStack(Material.COAL, 4)
        )

        val taken = service.take(contents, minion(), limit = Int.MAX_VALUE)

        assertEquals(12, taken?.count)
        assertNull(contents[0])
        assertNull(contents[2])
        assertEquals(3, contents[1]?.amount)
    }

    @Test
    fun `a limited deposit stops once it has enough`() {
        val service = serviceWith(Material.COAL to fuel("coal"))
        val contents = arrayOf<ItemStack?>(ItemStack(Material.COAL, 4), ItemStack(Material.COAL, 4))

        val taken = service.take(contents, minion(), limit = 6)

        assertEquals(6, taken?.count)
        assertNull(contents[0])
        assertEquals(2, contents[1]?.amount)
    }

    @Test
    fun `a deposit prefers the fuel already burning over the first one found`() {
        val service = serviceWith(
            Material.BLAZE_ROD to fuel("blaze_rod"),
            Material.COAL to fuel("coal")
        )
        val subject = minion()
        subject.burningFuelId = "coal"
        val contents = arrayOf<ItemStack?>(ItemStack(Material.BLAZE_ROD, 2), ItemStack(Material.COAL, 2))

        val taken = service.take(contents, subject, limit = Int.MAX_VALUE)

        assertEquals("coal", taken?.fuel?.id)
        assertEquals(2, taken?.count)
        assertEquals(2, contents[0]?.amount)
    }

    @Test
    fun `an inventory with no fuel takes nothing`() {
        val service = serviceWith(Material.COAL to fuel("coal"))
        val contents = arrayOf<ItemStack?>(ItemStack(Material.DIRT, 3), null)

        assertNull(service.take(contents, minion(), limit = Int.MAX_VALUE))
    }
}
