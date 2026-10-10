package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.libreforge.MinionDrops
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.KillRecord
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionDropList
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.minions.model.SlayerSettings
import com.exanthiax.ecominions.minions.service.ExperienceSink
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.WorkAreaEntities
import com.exanthiax.ecominions.testsupport.FakeEntityLookup
import com.exanthiax.ecominions.testsupport.FakePersistentDataContainer
import com.exanthiax.ecominions.testsupport.FakeWorld
import com.willfp.libreforge.triggers.event.DropResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

class SlayerActivityTest {

    private val world = FakeWorld()
    private val spawnedByKey = NamespacedKey("ecominions", "minion")
    private val living = mutableListOf<LivingEntity>()
    private val entities = object : WorkAreaEntities {
        @Suppress("UNCHECKED_CAST")
        override fun <T : Entity> find(minion: Minion, type: Class<T>): List<T> =
            if (type == LivingEntity::class.java) living as List<T> else emptyList()
    }
    private val sink = mockk<ExperienceSink>(relaxed = true)
    private val spawned = mutableListOf<LivingEntity>()

    private val baseStats = MinionDefinition.BaseStats(
        speedTicks = "20",
        efficiencyTicks = "5",
        radius = "1",
        height = "2",
        floorOffset = 0,
        storageSlots = "3",
        maxStorageSlots = "54",
        hardSlotCap = 54,
        regenDelayTicks = "60"
    )
    private val stats = MinionStats(baseStats, level = 1)

    private fun minion(
        selfSeed: Boolean = true,
        drops: MinionDropList = MinionDropList.EMPTY,
        maxMobs: String = "2",
        creditOwner: Boolean = false
    ) = Minion(
        state = MinionState(
            uuid = UUID.randomUUID(),
            definition = MinionDefinitionFixture.build(
                type = MinionType.SLAYER,
                baseStats = baseStats,
                placement = PlacementSettings.NONE.copy(selfSeed = selfSeed),
                slayer = SlayerSettings(listOf(FakeEntityLookup.testable(EntityType.ZOMBIE)), maxMobs, drops, creditOwner)
            )
        ),
        entity = world.stand(),
        storage = MinionStorage(3) { a, b -> a.type == b.type },
        stats = stats
    )

    private fun dispatch(minion: Minion) = mockk<MinionTriggerDispatch>(relaxed = true) {
        every { drops(minion, any(), any(), any(), any(), any(), any(), any()) } answers {
            MinionDrops(thirdArg<List<ItemStack>>().map { DropResult(it, 0) }, 0)
        }
        every { storeAndDispatchCollect(minion, any()) } answers { minion.storage.store(secondArg()) }
    }

    private val owner = mockk<Player>(relaxed = true)

    private fun activity(minion: Minion, events: EventDispatcher = RecordingEventDispatcher()) = SlayerActivity(
        entities = entities,
        spawner = { _, _ -> mob(EntityType.ZOMBIE).also { spawned += it } },
        dispatch = dispatch(minion),
        events = events,
        experience = sink,
        spawnedByKey = spawnedByKey,
        ownerOf = { owner },
        listening = { true },
        rewardOf = { 5 },
        random = Random(1)
    )

    private fun mob(type: EntityType, tag: String? = null, named: Boolean = false): LivingEntity {
        val container = FakePersistentDataContainer.create()
        tag?.let { container.set(spawnedByKey, PersistentDataType.STRING, it) }
        return mockk<Zombie>(relaxed = true) {
            every { this@mockk.type } returns type
            every { uniqueId } returns UUID.randomUUID()
            every { isValid } returns true
            every { isDead } returns false
            every { persistentDataContainer } returns container
            every { customName() } returns if (named) Component.text("Bob") else null
        }
    }

    private fun deathOf(entity: LivingEntity, drops: MutableList<ItemStack>, experience: Int): EntityDeathEvent =
        mockk(relaxed = true) {
            every { this@mockk.entity } returns entity
            every { this@mockk.drops } returns drops
            every { droppedExp } returns experience
        }

    @Test
    fun `a matching mob in range is swung at and killed after the windup`() {
        val subject = minion()
        val zombie = mob(EntityType.ZOMBIE)
        living += zombie
        val activity = activity(subject)

        assertEquals(ActivityResult.WORKED, activity.act(subject, 100L))
        activity.onTick(subject, 104L)
        verify(exactly = 0) { zombie.health = 0.0 }
        activity.onTick(subject, 105L)
        verify { zombie.health = 0.0 }
    }

    @Test
    fun `named mobs and other minions' mobs are skipped`() {
        val subject = minion(selfSeed = false)
        living += mob(EntityType.ZOMBIE, named = true)
        living += mob(EntityType.ZOMBIE, tag = UUID.randomUUID().toString())
        living += mob(EntityType.SKELETON)

        assertEquals(ActivityResult.BLOCKED, activity(subject).act(subject, 0L))
        assertEquals("no-mobs", subject.blockedReason)
    }

    @Test
    fun `self-seed spawns a frozen tagged mob`() {
        world.fillLayer(-1)
        val subject = minion()

        assertEquals(ActivityResult.SEEDED, activity(subject).act(subject, 0L))

        val mob = spawned.single()
        verify { mob.setAI(false) }
        verify { mob.isPersistent = true }
        assertEquals(subject.uuid.toString(), mob.persistentDataContainer.get(spawnedByKey, PersistentDataType.STRING))
        assertEquals(setOf(mob.uniqueId), subject.mining.spawnedMobs)
    }

    @Test
    fun `self-seed at max-mobs waits`() {
        world.fillLayer(-1)
        val subject = minion(maxMobs = "1")
        subject.mining.spawnedMobs += UUID.randomUUID()

        assertEquals(ActivityResult.BUSY, activity(subject).act(subject, 0L))
    }

    @Test
    fun `a configured drop list replaces the death drops and is sampled`() {
        val subject = minion(drops = MinionDropList(listOf(MinionDropList.Entry(ItemStack(Material.DIAMOND), 1.0, 1..1, 2..2))))
        val zombie = mob(EntityType.ZOMBIE)
        living += zombie
        val activity = activity(subject)
        activity.act(subject, 0L)
        activity.onTick(subject, 5L)
        val vanillaDrops = mutableListOf(ItemStack(Material.ROTTEN_FLESH))

        assertTrue(activity.onDeath(deathOf(zombie, vanillaDrops, 5)))

        assertTrue(vanillaDrops.isEmpty())
        assertEquals(1, subject.storage.count { it.type == Material.DIAMOND })
        verify { sink.add(subject, 2) }
        assertEquals(2, subject.state.killSample.single().experience)
    }

    @Test
    fun `a cancelled kill event stores nothing`() {
        val subject = minion()
        val zombie = mob(EntityType.ZOMBIE)
        living += zombie
        val activity = activity(subject, RecordingEventDispatcher(allow = false))
        activity.act(subject, 0L)
        activity.onTick(subject, 5L)

        activity.onDeath(deathOf(zombie, mutableListOf(ItemStack(Material.ROTTEN_FLESH)), 5))

        assertEquals(0, subject.storage.count { true })
        assertTrue(subject.state.killSample.isEmpty())
    }

    @Test
    fun `a death the minion did not cause is not handled`() {
        val subject = minion()

        assertFalse(activity(subject).onDeath(deathOf(mob(EntityType.ZOMBIE), mutableListOf(), 0)))
    }

    @Test
    fun `a tagged mob that dies to a player leaves the spawned set`() {
        val subject = minion()
        val mob = mob(EntityType.ZOMBIE, tag = subject.uuid.toString())
        subject.mining.spawnedMobs += mob.uniqueId

        activity(subject).forget(mob) { id -> subject.takeIf { it.uuid == id } }

        assertTrue(subject.mining.spawnedMobs.isEmpty())
    }

    @Test
    fun `recount finds only this minion's tagged mobs in the box`() {
        val subject = minion()
        living += mob(EntityType.ZOMBIE, tag = subject.uuid.toString())
        living += mob(EntityType.ZOMBIE, tag = subject.uuid.toString())
        living += mob(EntityType.ZOMBIE, tag = UUID.randomUUID().toString())
        subject.mining.spawnedMobs += UUID.randomUUID()

        activity(subject).recount(subject)

        assertEquals(2, subject.mining.spawnedMobs.size)
    }

    @Test
    fun `the owner is credited only when credit-owner is on`() {
        val credited = minion(creditOwner = true)
        val uncredited = minion(creditOwner = false)
        val first = mob(EntityType.ZOMBIE)
        val second = mob(EntityType.ZOMBIE)

        living += first
        activity(credited).apply { act(credited, 0L); onTick(credited, 5L) }
        living.clear()
        living += second
        activity(uncredited).apply { act(uncredited, 0L); onTick(uncredited, 5L) }

        verify { first.setKiller(owner) }
        verify(exactly = 0) { second.setKiller(any()) }
    }

    @Test
    fun `without a killer the mob's own xp reward is paid`() {
        val subject = minion()
        val zombie = mob(EntityType.ZOMBIE)
        living += zombie
        val activity = activity(subject)
        activity.act(subject, 0L)
        activity.onTick(subject, 5L)

        activity.onDeath(deathOf(zombie, mutableListOf(), 0))

        verify { sink.add(subject, 5) }
    }

    @Test
    fun `offline replays the sample or nothing`() {
        val subject = minion()
        val activity = activity(subject)

        assertNull(activity.offlineCycle(subject))

        subject.state = subject.state.withKillSample(listOf(KillRecord(listOf(ItemStack(Material.BONE)), 3)))

        assertEquals(3, activity.offlineCycle(subject)!!.experience)
    }
}
