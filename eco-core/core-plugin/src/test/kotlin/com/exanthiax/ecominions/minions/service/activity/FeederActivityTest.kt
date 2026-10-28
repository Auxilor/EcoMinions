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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

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

    private val coal = Fuel(
        id = "coal",
        displayName = "Coal",
        itemLookup = "coal",
        durationTicks = 1_200L,
        speedMultiplier = 2.0
    )
    private val blazeRod = Fuel(
        id = "blaze_rod",
        displayName = "Blaze Rod",
        itemLookup = "blaze_rod",
        durationTicks = 2_400L,
        speedMultiplier = 3.0
    )
    private val owner = UUID.randomUUID()
    private val stands = CopyOnWriteArrayList<Pair<ArmorStand, Minion>>()
    private val inactive = ConcurrentHashMap.newKeySet<Minion>()
    private val entities = object : WorkAreaEntities {
        @Suppress("UNCHECKED_CAST")
        override fun <T : Entity> find(minion: Minion, type: Class<T>): List<T> =
            if (type == ArmorStand::class.java) stands.map { (stand, _) -> stand } as List<T> else emptyList()
    }
    private val fuels = mockk<FuelService> {
        every { matching(any()) } answers {
            mapOf(Material.COAL to coal, Material.BLAZE_ROD to blazeRod)[firstArg<ItemStack>().type]
        }
        every { getById("coal") } returns coal
        every { getById("blaze_rod") } returns blazeRod
        every { activeFuelOf(any()) } answers {
            coal.takeIf { firstArg<Minion>().burningFuelEndsAt > System.currentTimeMillis() }
        }
    }
    private val fuelService = mockk<MinionFuelService> {
        every { unitMillis(coal) } returns 60_000L
        every { applyDirect(any(), coal, 60_000L) } returns true
        every { unitMillis(blazeRod) } returns 120_000L
        every { applyDirect(any(), blazeRod, 120_000L) } returns true
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
        minion(type, minionOwner).also { stands += mockk<ArmorStand>(relaxed = true) to it }

    private fun feeder(coalCount: Int = 3, settings: FeederSettings = FeederSettings(emptyList())) =
        minion(settings = settings).also { feeder ->
            if (coalCount > 0) {
                feeder.storage.add(ItemStack(Material.COAL, coalCount))
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
        targets = FeederTargets(
            entities = entities,
            fuels = fuels,
            minionOf = { entity -> stands.firstOrNull { (stand, _) -> stand == entity }?.second },
            isActive = { minion -> minion !in inactive },
            islandAt = { location -> if (islandPrivileges()) islandAt(location) else null },
            takesInput = { definition -> definition.feeder != null || definition.breeder != null }
        ),
        fuels = fuels,
        fuelService = fuelService,
        storageSaved = saved,
        signals = ActivitySignals(events, dispatch) { true },
        canWork = canWork
    )

    private fun island(islandId: String = "one", islandProvider: String = "test") = mockk<MinionIsland> {
        every { id } returns islandId
        every { provider } returns islandProvider
    }

    private fun coalIn(minion: Minion) = minion.storage.count { it.type == Material.COAL }

    @Test
    fun `feeds one unfuelled neighbour`() {
        val subject = feeder()
        val first = target()
        target()
        var saves = 0

        assertEquals(ActivityResult.WORKED, activity(saved = { saves++ }).act(subject, 0L))

        verify(exactly = 1) { fuelService.applyDirect(any(), coal, 60_000L) }
        verify(exactly = 1) { fuelService.applyDirect(first, coal, 60_000L) }
        assertEquals(2, coalIn(subject))
        assertEquals(1, saves)
    }

    @Test
    fun `skips a frozen remainder`() {
        val subject = feeder()
        target().burningFuelRemainingMillis = 5_000L

        assertEquals(ActivityResult.BUSY, activity().act(subject, 0L))

        assertEquals("nothing-to-feed", subject.blockedReason)
        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `feeds after a burn expired`() {
        val subject = feeder()
        val stale = target()
        stale.burningFuelEndsAt = System.currentTimeMillis() - 1_000L
        stale.burningFuelRemainingMillis = 5_000L

        assertEquals(ActivityResult.WORKED, activity().act(subject, 0L))

        verify(exactly = 1) { fuelService.applyDirect(stale, coal, 60_000L) }
    }

    @Test
    fun `skips a full target`() {
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
    fun `skips another owner off-island`() {
        val subject = feeder()
        target(minionOwner = UUID.randomUUID())

        assertEquals(ActivityResult.BUSY, activity(islandPrivileges = { true }).act(subject, 0L))
    }

    @Test
    fun `feeds another owner on island`() {
        val subject = feeder()
        target(minionOwner = UUID.randomUUID())
        val shared = island()

        val subjectActivity = activity(islandAt = { shared }, islandPrivileges = { true })

        assertEquals(ActivityResult.WORKED, subjectActivity.act(subject, 0L))
    }

    @Test
    fun `cancelled burn returns fuel`() {
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
    fun `cancelled feed takes nothing`() {
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
    fun `cancelled feed moves on`() {
        val subject = feeder()
        val first = target()
        val second = target()
        val events = RecordingEventDispatcher(edit = { event ->
            if (event is MinionFeedEvent && event.target === first) event.isCancelled = true
        })

        assertEquals(ActivityResult.WORKED, activity(events).act(subject, 0L))

        verify(exactly = 0) { fuelService.applyDirect(first, any(), any()) }
        verify(exactly = 1) { fuelService.applyDirect(second, coal, 60_000L) }
        assertEquals(2, coalIn(subject))
    }

    @Test
    fun `feed event carries the burn`() {
        val subject = feeder()
        val hungry = target()
        val events = RecordingEventDispatcher()

        activity(events).act(subject, 0L)

        val event = events.last<MinionFeedEvent>()
        assertSame(subject, event.minion)
        assertSame(hungry, event.target)
        assertEquals("coal", event.fuel.id)
        assertEquals(60_000L, event.durationMillis)
    }

    @Test
    fun `zero duration cancels the feed`() {
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
    fun `applies a changed duration`() {
        val subject = feeder()
        val hungry = target()
        every { fuelService.applyDirect(hungry, coal, 5_000L) } returns true
        val events = RecordingEventDispatcher(edit = { event ->
            if (event is MinionFeedEvent) event.durationMillis = 5_000L
        })

        assertEquals(ActivityResult.WORKED, activity(events).act(subject, 0L))

        verify(exactly = 1) { fuelService.applyDirect(hungry, coal, 5_000L) }
    }

    @Test
    fun `feed event needs a listener`() {
        val subject = feeder()
        target()
        val events = RecordingEventDispatcher()
        val silent = FeederActivity(
            targets = FeederTargets(
                entities = entities,
                fuels = fuels,
                minionOf = { entity -> stands.firstOrNull { (stand, _) -> stand == entity }?.second },
                isActive = { true },
                islandAt = { null },
                takesInput = { false }
            ),
            fuels = fuels,
            fuelService = fuelService,
            storageSaved = {},
            signals = ActivitySignals(events, dispatch) { false }
        )

        assertEquals(ActivityResult.WORKED, silent.act(subject, 0L))

        assertEquals(emptyList<Any>(), events.events)
    }

    @Test
    fun `fires the feed trigger once`() {
        val subject = feeder()
        val hungry = target()
        target()

        activity().act(subject, 0L)

        val spot = hungry.location
        verify(exactly = 1) { dispatch.triggerAt(subject, TriggerMinionFeed, spot, 1.0) }
        verify(exactly = 1) { dispatch.triggerAt(any(), any(), any(), any()) }
    }

    @Test
    fun `no trigger on cancelled burn`() {
        val subject = feeder()
        target()
        every { fuelService.applyDirect(any(), any(), any()) } returns false

        activity().act(subject, 0L)

        verify(exactly = 0) { dispatch.triggerAt(any(), any(), any(), any()) }
    }

    @Test
    fun `skips island if privileges off`() {
        val subject = feeder()
        target(minionOwner = UUID.randomUUID())
        val shared = island()

        val subjectActivity = activity(islandAt = { shared }, islandPrivileges = { false })

        assertEquals(ActivityResult.BUSY, subjectActivity.act(subject, 0L))

        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `skips another island provider`() {
        val subject = feeder()
        val other = target(minionOwner = UUID.randomUUID())
        val islands = mapOf(
            subject.location to island(islandProvider = "a"),
            other.location to island(islandProvider = "b")
        )

        val subjectActivity = activity(islandAt = { islands[it] }, islandPrivileges = { true })

        assertEquals(ActivityResult.BUSY, subjectActivity.act(subject, 0L))

        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `skips another island id`() {
        val subject = feeder()
        val other = target(minionOwner = UUID.randomUUID())
        val islands = mapOf(subject.location to island(islandId = "one"), other.location to island(islandId = "two"))

        val subjectActivity = activity(islandAt = { islands[it] }, islandPrivileges = { true })

        assertEquals(ActivityResult.BUSY, subjectActivity.act(subject, 0L))

        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `skips where owner cannot work`() {
        val subject = feeder()
        target()

        assertEquals(ActivityResult.BUSY, activity(canWork = { _, _ -> false }).act(subject, 0L))

        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `fuel filter rejects unlisted`() {
        val subject = feeder(coalCount = 0, settings = FeederSettings(listOf("coal")))
        subject.storage.add(ItemStack(Material.BLAZE_ROD, 2))
        target()

        assertEquals(ActivityResult.BLOCKED, activity().act(subject, 0L))

        assertEquals("no-fuel-stored", subject.blockedReason)
        verify(exactly = 0) { fuelService.applyDirect(any(), any(), any()) }
    }

    @Test
    fun `prefers the last burned fuel`() {
        val subject = feeder()
        subject.storage.add(ItemStack(Material.BLAZE_ROD, 2))
        val hungry = target()
        hungry.burningFuelId = "blaze_rod"

        assertEquals(ActivityResult.WORKED, activity().act(subject, 0L))

        verify(exactly = 1) { fuelService.applyDirect(hungry, blazeRod, 120_000L) }
        verify(exactly = 0) { fuelService.applyDirect(any(), coal, any()) }
    }

    @Test
    fun `feeds next after a cancel`() {
        val subject = feeder()
        val first = target()
        val second = target()
        every { fuelService.applyDirect(first, coal, 60_000L) } returns false

        assertEquals(ActivityResult.WORKED, activity().act(subject, 0L))

        verify(exactly = 1) { fuelService.applyDirect(second, coal, 60_000L) }
        assertEquals(2, coalIn(subject))
    }

    @Test
    fun `second feeder sees the burn`() {
        val first = feeder()
        val second = feeder()
        val shared = target()
        val subject = activity()

        assertEquals(ActivityResult.WORKED, subject.act(first, 0L))
        shared.burningFuelEndsAt = System.currentTimeMillis() + 60_000L

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
    fun `ignores items no longer fuel`() {
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
