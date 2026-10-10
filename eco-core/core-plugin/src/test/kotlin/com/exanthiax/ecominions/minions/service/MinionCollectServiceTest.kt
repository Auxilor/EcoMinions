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
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

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

    /** A player whose inventory keeps back [refused] of whatever it is handed. */
    private fun player(refused: Int = 0): Player = mockk(relaxed = true) {
        every { inventory.addItem(*anyVararg()) } answers {
            val item = firstArg<Array<ItemStack>>().first()
            if (refused > 0) hashMapOf(0 to item.clone().apply { amount = refused }) else hashMapOf()
        }
    }

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
}
