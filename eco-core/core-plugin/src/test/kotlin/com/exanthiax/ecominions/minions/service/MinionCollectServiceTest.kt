package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionExtractEvent
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

class MinionCollectServiceTest {

    private val stats = MinionStats(
        MinionDefinition.BaseStats(
            speedTicks = "40",
            efficiencyTicks = "10",
            radius = "2",
            storageSlots = "3",
            maxStorageSlots = "54",
            hardSlotCap = 54,
            regenDelayTicks = "60"
        ),
        level = 1
    )

    private val repository = mockk<MinionStateRepository>(relaxed = true)

    private fun minion() = Minion(
        state = MinionState(definition = MinionDefinitionFixture.build()),
        entity = mockk<ArmorStand>(relaxed = true),
        storage = MinionStorage(3) { a, b -> a.type == b.type },
        stats = stats
    ).apply {
        storage.add(ItemStack(Material.COBBLESTONE, 10))
        storage.add(ItemStack(Material.DIAMOND, 2))
    }

    /**
     * A player whose inventory keeps back [refused] of whatever it is handed, and
     * whose world either accepts or rejects dropped items.
     */
    private fun player(refused: Int = 0, dropsSpawn: Boolean = true): Player = mockk(relaxed = true) {
        every { inventory.addItem(*anyVararg()) } answers {
            val item = firstArg<Array<ItemStack>>().first()
            if (refused > 0) hashMapOf(0 to item.clone().apply { amount = refused }) else hashMapOf()
        }
        every { world.dropItemNaturally(any(), any<ItemStack>()) } returns mockk<Item> {
            every { isValid } returns dropsSpawn
        }
    }

    private fun sample(material: Material, cap: Int = 64): ItemStack =
        spyk(ItemStack(material)) { every { maxStackSize } returns cap }

    @Test
    fun `collecting moves everything into the inventory`() {
        val subject = minion()

        val collected = MinionCollectService(repository, RecordingEventDispatcher()).collect(player(), subject)

        assertEquals(12, collected)
        assertEquals(0, subject.storage.used)
        verify { repository.save(subject) }
    }

    @Test
    fun `a cancelled extract leaves storage alone`() {
        val subject = minion()

        val collected = MinionCollectService(repository, RecordingEventDispatcher(allow = false)).collect(player(), subject)

        assertNull(collected)
        assertEquals(2, subject.storage.used)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `only what a listener leaves in the list is taken`() {
        val subject = minion()
        val events = RecordingEventDispatcher { event ->
            (event as? MinionExtractEvent)?.items?.removeIf { it.type == Material.DIAMOND }
        }

        val collected = MinionCollectService(repository, events).collect(player(), subject)

        assertEquals(10, collected)
        assertEquals(Material.DIAMOND, subject.storage.getItem(1)?.type)
        assertEquals(1, subject.storage.used)
    }

    @Test
    fun `what the inventory refuses goes back into storage`() {
        val subject = minion()

        val collected = MinionCollectService(repository, RecordingEventDispatcher()).collect(player(refused = 1), subject)

        assertEquals(10, collected)
        assertEquals(2, subject.storage.count { true })
    }

    @Test
    fun `partial fill counts delivered`() {
        val subject = minion().apply {
            storage.clear()
            storage.add(ItemStack(Material.COBBLESTONE, 10))
        }
        val player = player()
        every { player.inventory.addItem(*anyVararg()) } answers {
            val item = firstArg<Array<ItemStack>>().first()
            item.amount -= 4
            hashMapOf(0 to item)
        }

        val collected = MinionCollectService(repository, RecordingEventDispatcher()).collect(player, subject)

        assertEquals(4, collected)
        assertEquals(6, subject.storage.count { true })
    }

    @Test
    fun `drop empties storage`() {
        val subject = minion()
        val player = player()

        val dropped = MinionCollectService(repository, RecordingEventDispatcher()).drop(player, subject)

        assertEquals(12, dropped)
        assertEquals(0, subject.storage.used)
        verify(exactly = 2) { player.world.dropItemNaturally(player.location, any<ItemStack>()) }
        verify(exactly = 0) { player.inventory.addItem(*anyVararg()) }
        verify { repository.save(subject) }
    }

    @Test
    fun `cancelled drop keeps storage`() {
        val subject = minion()
        val player = player()

        val dropped = MinionCollectService(repository, RecordingEventDispatcher(allow = false)).drop(player, subject)

        assertNull(dropped)
        assertEquals(2, subject.storage.used)
        verify(exactly = 0) { player.world.dropItemNaturally(any(), any<ItemStack>()) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `drop honours listener filter`() {
        val subject = minion()
        val events = RecordingEventDispatcher { event ->
            (event as? MinionExtractEvent)?.items?.removeIf { it.type == Material.DIAMOND }
        }

        val dropped = MinionCollectService(repository, events).drop(player(), subject)

        assertEquals(10, dropped)
        assertEquals(1, subject.storage.used)
    }

    @Test
    fun `rejected drop stays stored`() {
        val subject = minion()

        val dropped = MinionCollectService(repository, RecordingEventDispatcher())
            .drop(player(dropsSpawn = false), subject)

        assertEquals(0, dropped)
        assertEquals(12, subject.storage.count { true })
    }

    @Test
    fun `dropping nothing does not save`() {
        val subject = minion().apply { storage.clear() }

        val dropped = MinionCollectService(repository, RecordingEventDispatcher()).drop(player(), subject)

        assertEquals(0, dropped)
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `withdraw takes one stack`() {
        val subject = minion().apply { storage.add(ItemStack(Material.COBBLESTONE, 64)) }
        val events = RecordingEventDispatcher()

        val withdrawn = MinionCollectService(repository, events)
            .withdraw(player(), subject, sample(Material.COBBLESTONE))

        assertEquals(64, withdrawn)
        assertEquals(10, subject.storage.countSimilar(ItemStack(Material.COBBLESTONE)))
        assertEquals(2, subject.storage.countSimilar(ItemStack(Material.DIAMOND)))
        verify { repository.save(subject) }
        val event = events.last<MinionExtractEvent>()
        assertSame(subject, event.minion)
        assertEquals(listOf(Material.COBBLESTONE to 64), event.items.map { it.type to it.amount })
    }

    @Test
    fun `emptied withdraw skips save`() {
        val subject = minion()
        val events = RecordingEventDispatcher(edit = { event ->
            if (event is MinionExtractEvent) event.items.clear()
        })

        val withdrawn = MinionCollectService(repository, events)
            .withdraw(player(), subject, sample(Material.COBBLESTONE))

        assertEquals(0, withdrawn)
        assertEquals(10, subject.storage.countSimilar(ItemStack(Material.COBBLESTONE)))
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `withdraw keeps the overflow`() {
        val subject = minion().apply { storage.add(ItemStack(Material.COBBLESTONE, 64)) }

        val withdrawn = MinionCollectService(repository, RecordingEventDispatcher())
            .withdraw(player(refused = 20), subject, sample(Material.COBBLESTONE))

        assertEquals(44, withdrawn)
        assertEquals(30, subject.storage.countSimilar(ItemStack(Material.COBBLESTONE)))
    }

    @Test
    fun `withdraw obeys item stack cap`() {
        val subject = minion()
        val withdrawn = MinionCollectService(repository, RecordingEventDispatcher())
            .withdraw(player(), subject, sample(Material.COBBLESTONE, cap = 4))

        assertEquals(4, withdrawn)
        assertEquals(6, subject.storage.countSimilar(ItemStack(Material.COBBLESTONE)))
    }

    @Test
    fun `cancelled withdraw keeps all`() {
        val subject = minion()
        val player = player()

        val withdrawn = MinionCollectService(repository, RecordingEventDispatcher(allow = false))
            .withdraw(player, subject, sample(Material.COBBLESTONE))

        assertNull(withdrawn)
        assertEquals(12, subject.storage.count { true })
        verify(exactly = 0) { player.inventory.addItem(*anyVararg()) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `lowered withdraw takes less`() {
        val subject = minion()
        val events = RecordingEventDispatcher(edit = { event ->
            if (event is MinionExtractEvent) event.items.forEach { it.amount = 3 }
        })

        val withdrawn = MinionCollectService(repository, events)
            .withdraw(player(), subject, sample(Material.COBBLESTONE))

        assertEquals(3, withdrawn)
        assertEquals(7, subject.storage.countSimilar(ItemStack(Material.COBBLESTONE)))
    }

    @Test
    fun `empty withdraw fires no event`() {
        val events = RecordingEventDispatcher()

        val withdrawn = MinionCollectService(repository, events)
            .withdraw(player(), minion(), sample(Material.GOLD_INGOT))

        assertEquals(0, withdrawn)
        assertTrue(events.events.isEmpty())
    }

    @Test
    fun `claiming gives the stored xp and empties it`() {
        val subject = minion()
        subject.state = subject.state.withStoredExperience(12)
        val player = mockk<Player>(relaxed = true)

        val claimed = MinionCollectService(repository, RecordingEventDispatcher()).claimExperience(player, subject)

        assertEquals(12, claimed)
        assertEquals(0, subject.state.storedExperience)
        verify { player.giveExp(12) }
        verify { repository.save(subject) }
    }

    @Test
    fun `claiming nothing gives nothing`() {
        val player = mockk<Player>(relaxed = true)

        assertEquals(0, MinionCollectService(repository, RecordingEventDispatcher()).claimExperience(player, minion()))
        verify(exactly = 0) { player.giveExp(any()) }
    }

    private val withdrawals = CopyOnWriteArrayList<ItemStack>()

    private fun recording(events: RecordingEventDispatcher = RecordingEventDispatcher()) =
        MinionCollectService(repository, events) { _, _, item -> withdrawals += item }

    @Test
    fun `collect fires per stack`() {
        recording().collect(player(), minion())

        assertEquals(
            listOf(Material.COBBLESTONE to 10, Material.DIAMOND to 2),
            withdrawals.map { it.type to it.amount }
        )
    }

    @Test
    fun `withdrawn skips the overflow`() {
        recording().collect(player(refused = 1), minion())

        assertEquals(listOf(9, 1), withdrawals.map { it.amount })
    }

    @Test
    fun `drop fires nothing`() {
        recording().drop(player(), minion())

        assertTrue(withdrawals.isEmpty())
    }

    @Test
    fun `withdrawn runs after save`() {
        val subject = minion()

        MinionCollectService(repository, RecordingEventDispatcher()) { _, _, _ ->
            verify { repository.save(subject) }
        }.collect(player(), subject)
    }

    @Test
    fun `cancel fires nothing`() {
        recording(RecordingEventDispatcher(allow = false)).collect(player(), minion())

        assertTrue(withdrawals.isEmpty())
    }

    @Test
    fun `withdraw fires what moved`() {
        val subject = minion().apply { storage.add(ItemStack(Material.COBBLESTONE, 64)) }

        recording().withdraw(player(refused = 20), subject, sample(Material.COBBLESTONE))

        assertEquals(listOf(44), withdrawals.map { it.amount })
    }

    @Test
    fun `withdraw saves first`() {
        val subject = minion()

        MinionCollectService(repository, RecordingEventDispatcher()) { _, _, _ ->
            verify { repository.save(subject) }
        }.withdraw(player(), subject, sample(Material.COBBLESTONE))
    }
}
