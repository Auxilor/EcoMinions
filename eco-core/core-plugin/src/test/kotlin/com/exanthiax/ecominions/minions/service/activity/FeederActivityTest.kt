package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.event.MinionFeedEvent
import com.exanthiax.ecominions.api.island.MinionIsland
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionFeed
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.FeederSettings
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionFuelService
import com.exanthiax.ecominions.minions.service.WorkAreaEntities
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class FeederActivityTest {

    private val stats = MinionStats(
        MinionDefinition.BaseStats(
            speedTicks = "20",
            efficiencyTicks = "5",
            radius = "2",
            storageSlots = "1",
            maxStorageSlots = "54",
            hardSlotCap = 54,
            regenDelayTicks = "60"
        ),
        level = 1
    )

    private val coal = Fuel("coal", "Coal", "coal", 1200L, 2.0)
    private val blazeRod = Fuel("blaze_rod", "Blaze Rod", "blaze_rod", 2400L, 3.0)
    private val owner = UUID.randomUUID()
    private val stands = mutableMapOf<ArmorStand, Minion>()
    private val inactive = mutableSetOf<Minion>()
    private val entities = object : WorkAreaEntities {
        @Suppress("UNCHECKED_CAST")
        override fun <T : Entity> find(minion: Minion, type: Class<T>): List<T> =
            if (type == ArmorStand::class.java) stands.keys.toList() as List<T> else emptyList()
    }
    private val fuels = mockk<FuelService> {
        every { matching(any()) } answers {
            when (firstArg<ItemStack>().type) {
                Material.COAL -> coal
                Material.BLAZE_ROD -> blazeRod
                else -> null
            }
        }
        every { getById("coal") } returns coal
        every { getById("blaze_rod") } returns blazeRod
        every { activeFuelOf(any()) } answers {
            coal.takeIf { firstArg<Minion>().burningFuelEndsAt > System.currentTimeMillis() }
        }
    }
    private val fuelService = mockk<MinionFuelService> {
        every { unitMillis(coal) } returns 60000L
        every { applyDirect(any(), coal, 60000L) } returns true
        every { unitMillis(blazeRod) } returns 120000L
        every { applyDirect(any(), blazeRod, 120000L) } returns true
    }

    private fun minion(
        type: MinionType = MinionType.FEEDER,
        minionOwner: UUID? = owner,
        settings: FeederSettings = FeederSettings(emptyList())
    ) = Minion(
        state = MinionState(
            definition = MinionDefinitionFixture.build(
                type = type,
                feeder = settings.takeIf { type == MinionType.FEEDER }
            ),
            owner = minionOwner
        ),
        entity = mockk<ArmorStand>(relaxed = true) {
            every { location } returns mockk<Location>(relaxed = true)
        },
        storage = MinionStorage(2) { a, b -> a.type == b.type },
        stats = stats
    )

    private fun target(minionOwner: UUID? = owner, type: MinionType = MinionType.MINER): Minion =
        minion(type, minionOwner).also { stands[mockk<ArmorStand>(relaxed = true)] = it }

    private fun feeder(coalCount: Int = 3, settings: FeederSettings = FeederSettings(emptyList())) =
        minion(settings = settings).also {
            if (coalCount > 0) {
                it.storage.add(ItemStack(Material.COAL, coalCount))
            }
        }

    private val dispatch = mockk<MinionTriggerDispatch>(relaxed = true)

    private fun activity(
        events: EventDispatcher = RecordingEventDispatcher(),
        islandAt: (Location) -> MinionIsland? = { null },
        islandPrivileges: () -> Boolean = { false },
        saved: (Minion) -> Unit = {},
        canWork: (Minion, Block) -> Boolean = { _, _ -> true }
    ) = FeederActivity(
        entities = entities,
        fuels = fuels,
        fuelService = fuelService,
        storageSaved = saved,
        minionOf = { entity -> stands[entity] },
        isActive = { it !in inactive },
        islandAt = islandAt,
        islandPrivileges = islandPrivileges,
        takesInput = { it.feeder != null || it.breeder != null },
        events = events,
        dispatch = dispatch,
        listening = { true },
        canWork = canWork
    )

    private fun island(id: String = "one", provider: String = "test") = mockk<MinionIsland> {
        every { this@mockk.id } returns id
        every { this@mockk.provider } returns provider
    }

    private fun coalIn(minion: Minion) = minion.storage.count { it.type == Material.COAL }

    @Test
    fun `feeds one unfuelled active neighbour from storage`() {
        val subject = feeder()
        val first = target()
        target()
        var saves = 0

        assertEquals(ActivityResult.WORKED, activity(saved = { saves++ }).act(subject, 0L))

        verify(exactly = 1) { fuelService.applyDirect(any(), coal, 60000L) }
        verify(exactly = 1) { fuelService.applyDirect(first, coal, 60000L) }
        assertEquals(2, coalIn(subject))
        assertEquals(1, saves)
    }

    @Test
    fun `skips a neighbour with a frozen remainder`() {
        val subject = feeder()
        target().burningFuelRemainingMillis = 5000L

        assertEquals(ActivityResult.BUSY, activity().act(subject, 0L))

        assertEquals("nothing-to-feed", subject.blockedReason)
        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `feeds a target whose live burn expired with a stale remainder`() {
        val subject = feeder()
        val stale = target()
        stale.burningFuelEndsAt = System.currentTimeMillis() - 1000L
        stale.burningFuelRemainingMillis = 5000L

        assertEquals(ActivityResult.WORKED, activity().act(subject, 0L))

        verify(exactly = 1) { fuelService.applyDirect(stale, coal, 60000L) }
    }

    @Test
    fun `skips a target with full storage`() {
        val subject = feeder()
        val full = target()
        full.storage.add(ItemStack(Material.STONE, 64))
        full.storage.add(ItemStack(Material.DIRT, 64))

        assertEquals(ActivityResult.BUSY, activity().act(subject, 0L))

        verify(exactly = 0) { fuelService.applyDirect(full, any(), any()) }
    }

    @Test
    fun `skips an inactive neighbour`() {
        val subject = feeder()
        inactive += target()

        assertEquals(ActivityResult.BUSY, activity().act(subject, 0L))

        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `skips other feeders`() {
        val subject = feeder()
        target(type = MinionType.FEEDER)

        assertEquals(ActivityResult.BUSY, activity().act(subject, 0L))
    }

    @Test
    fun `skips a neighbour of another owner off-island`() {
        val subject = feeder()
        target(minionOwner = UUID.randomUUID())

        assertEquals(ActivityResult.BUSY, activity(islandPrivileges = { true }).act(subject, 0L))
    }

    @Test
    fun `feeds a neighbour of another owner on the same island`() {
        val subject = feeder()
        target(minionOwner = UUID.randomUUID())
        val shared = island()

        val subjectActivity = activity(islandAt = { shared }, islandPrivileges = { true })

        assertEquals(ActivityResult.WORKED, subjectActivity.act(subject, 0L))
    }

    @Test
    fun `puts the fuel back when the event is cancelled`() {
        val subject = feeder()
        target()
        every { fuelService.applyDirect(any(), any(), any()) } returns false
        var saves = 0

        assertEquals(ActivityResult.BUSY, activity(saved = { saves++ }).act(subject, 0L))

        assertEquals(3, coalIn(subject))
        assertEquals("nothing-to-feed", subject.blockedReason)
        assertEquals(1, saves)
    }

    @Test
    fun `a cancelled feed event takes nothing and applies nothing`() {
        val subject = feeder()
        target()
        var saves = 0

        val result = activity(RecordingEventDispatcher(allow = false), saved = { saves++ }).act(subject, 0L)

        assertEquals(ActivityResult.BUSY, result)
        assertEquals(3, coalIn(subject))
        assertEquals(0, saves)
        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
        verify(exactly = 0) { dispatch.triggerAt(any(), any(), any(), any()) }
    }

    @Test
    fun `moves on to the next neighbour when the feed event for the first is cancelled`() {
        val subject = feeder()
        val first = target()
        val second = target()
        val events = RecordingEventDispatcher(edit = { event ->
            if (event is MinionFeedEvent && event.target === first) event.isCancelled = true
        })

        assertEquals(ActivityResult.WORKED, activity(events).act(subject, 0L))

        verify(exactly = 0) { fuelService.applyDirect(first, any(), any()) }
        verify(exactly = 1) { fuelService.applyDirect(second, coal, 60000L) }
        assertEquals(2, coalIn(subject))
    }

    @Test
    fun `the feed event carries the feeder target fuel and duration`() {
        val subject = feeder()
        val hungry = target()
        val events = RecordingEventDispatcher()

        activity(events).act(subject, 0L)

        val event = events.last<MinionFeedEvent>()
        assertSame(subject, event.minion)
        assertSame(hungry, event.target)
        assertEquals("coal", event.fuel.id)
        assertEquals(60000L, event.durationMillis)
    }

    @Test
    fun `a listener that zeroes the duration cancels the feed`() {
        val subject = feeder()
        target()
        val events = RecordingEventDispatcher(edit = { event ->
            if (event is MinionFeedEvent) event.durationMillis = 0L
        })

        assertEquals(ActivityResult.BUSY, activity(events).act(subject, 0L))

        assertEquals(3, coalIn(subject))
        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
        verify(exactly = 0) { dispatch.triggerAt(any(), any(), any(), any()) }
    }

    @Test
    fun `applies the duration a listener changed`() {
        val subject = feeder()
        val hungry = target()
        every { fuelService.applyDirect(hungry, coal, 5000L) } returns true
        val events = RecordingEventDispatcher(edit = { event ->
            if (event is MinionFeedEvent) event.durationMillis = 5000L
        })

        assertEquals(ActivityResult.WORKED, activity(events).act(subject, 0L))

        verify(exactly = 1) { fuelService.applyDirect(hungry, coal, 5000L) }
    }

    @Test
    fun `does not dispatch the feed event when nobody listens`() {
        val subject = feeder()
        target()
        val events = RecordingEventDispatcher()
        val silent = FeederActivity(
            entities = entities,
            fuels = fuels,
            fuelService = fuelService,
            storageSaved = {},
            minionOf = { entity -> stands[entity] },
            isActive = { true },
            islandAt = { null },
            islandPrivileges = { false },
            takesInput = { false },
            events = events,
            dispatch = dispatch,
            listening = { false }
        )

        assertEquals(ActivityResult.WORKED, silent.act(subject, 0L))

        assertEquals(emptyList<Any>(), events.events)
    }

    @Test
    fun `fires the feed trigger once at the target`() {
        val subject = feeder()
        val hungry = target()
        target()

        activity().act(subject, 0L)

        val spot = hungry.location
        verify(exactly = 1) { dispatch.triggerAt(subject, TriggerMinionFeed, spot, 1.0) }
        verify(exactly = 1) { dispatch.triggerAt(any(), any(), any(), any()) }
    }

    @Test
    fun `fires no trigger when the fuel event cancels the burn`() {
        val subject = feeder()
        target()
        every { fuelService.applyDirect(any(), any(), any()) } returns false

        activity().act(subject, 0L)

        verify(exactly = 0) { dispatch.triggerAt(any(), any(), any(), any()) }
    }

    @Test
    fun `skips another owner on the same island when privileges are off`() {
        val subject = feeder()
        target(minionOwner = UUID.randomUUID())
        val shared = island()

        val subjectActivity = activity(islandAt = { shared }, islandPrivileges = { false })

        assertEquals(ActivityResult.BUSY, subjectActivity.act(subject, 0L))

        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `skips another owner on an island of a different provider`() {
        val subject = feeder()
        val other = target(minionOwner = UUID.randomUUID())
        val islands = mapOf(subject.location to island(provider = "a"), other.location to island(provider = "b"))

        val subjectActivity = activity(islandAt = { islands[it] }, islandPrivileges = { true })

        assertEquals(ActivityResult.BUSY, subjectActivity.act(subject, 0L))

        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `skips another owner on an island with a different id`() {
        val subject = feeder()
        val other = target(minionOwner = UUID.randomUUID())
        val islands = mapOf(subject.location to island(id = "one"), other.location to island(id = "two"))

        val subjectActivity = activity(islandAt = { islands[it] }, islandPrivileges = { true })

        assertEquals(ActivityResult.BUSY, subjectActivity.act(subject, 0L))

        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `skips a neighbour the owner cannot work at`() {
        val subject = feeder()
        target()

        assertEquals(ActivityResult.BUSY, activity(canWork = { _, _ -> false }).act(subject, 0L))

        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `a fuel filter rejects a stored fuel that is not listed`() {
        val subject = feeder(coalCount = 0, settings = FeederSettings(listOf("coal")))
        subject.storage.add(ItemStack(Material.BLAZE_ROD, 2))
        target()

        assertEquals(ActivityResult.BLOCKED, activity().act(subject, 0L))

        assertEquals("no-fuel-stored", subject.blockedReason)
        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `prefers the fuel the neighbour last burned`() {
        val subject = feeder()
        subject.storage.add(ItemStack(Material.BLAZE_ROD, 2))
        val hungry = target()
        hungry.burningFuelId = "blaze_rod"

        assertEquals(ActivityResult.WORKED, activity().act(subject, 0L))

        verify(exactly = 1) { fuelService.applyDirect(hungry, blazeRod, 120000L) }
        verify(exactly = 0) { fuelService.applyDirect(any(), coal, any()) }
    }

    @Test
    fun `feeds the next neighbour when the first is cancelled`() {
        val subject = feeder()
        val first = target()
        val second = target()
        every { fuelService.applyDirect(first, coal, 60000L) } returns false

        assertEquals(ActivityResult.WORKED, activity().act(subject, 0L))

        verify(exactly = 1) { fuelService.applyDirect(second, coal, 60000L) }
        assertEquals(2, coalIn(subject))
    }

    @Test
    fun `second feeder sees the live burn`() {
        val first = feeder()
        val second = feeder()
        val shared = target()
        val subject = activity()

        assertEquals(ActivityResult.WORKED, subject.act(first, 0L))
        shared.burningFuelEndsAt = System.currentTimeMillis() + 60000L

        assertEquals(ActivityResult.BUSY, subject.act(second, 0L))
    }

    @Test
    fun `blocked with an empty store`() {
        val subject = feeder(coalCount = 0)
        target()

        assertEquals(ActivityResult.BLOCKED, activity().act(subject, 0L))

        assertEquals("no-fuel-stored", subject.blockedReason)
    }

    @Test
    fun `ignores stored items that are no longer fuel`() {
        val subject = feeder(coalCount = 0)
        subject.storage.add(ItemStack(Material.STONE, 4))
        target()

        assertEquals(ActivityResult.BLOCKED, activity().act(subject, 0L))

        assertEquals("no-fuel-stored", subject.blockedReason)
        assertEquals(4, subject.storage.count { it.type == Material.STONE })
    }

    @Test
    fun `offline cycle earns nothing`() {
        assertNull(activity().offlineCycle(feeder()))
    }
}
