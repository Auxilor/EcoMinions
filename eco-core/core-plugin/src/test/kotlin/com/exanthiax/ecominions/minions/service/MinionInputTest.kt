package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionDepositEvent
import com.exanthiax.ecominions.api.event.MinionExtractEvent
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.BreederSettings
import com.exanthiax.ecominions.minions.model.FeederSettings
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.testsupport.FakeItemLookup
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.inventory.PlayerInventory
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionInputTest {

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

    private val saved = mutableListOf<Minion>()

    private val fuels = mockk<FuelService> {
        every { matching(any()) } answers {
            val item = firstArg<ItemStack>()
            if (item.type == Material.COAL) Fuel("coal", "Coal", "coal", 1000L, 1.0) else null
        }
    }

    private val events = RecordingEventDispatcher()
    private val input = inputWith(events)
    private val player = mockk<Player>(relaxed = true)

    private fun inputWith(dispatcher: RecordingEventDispatcher) = MinionInput(fuels, { saved += it }, dispatcher)

    private fun playerWith(inventory: PlayerInventory) = mockk<Player>(relaxed = true) {
        every { this@mockk.inventory } returns inventory
    }

    private fun roomyInventory() = mockk<PlayerInventory> { every { addItem(*anyVararg()) } returns hashMapOf() }

    private fun minion(definition: MinionDefinition, slots: Int = 1): Minion = Minion(
        state = MinionState(definition = definition),
        entity = mockk<ArmorStand>(relaxed = true),
        storage = MinionStorage(slots) { a, b -> a.type == b.type },
        stats = stats
    )

    private fun feeder() = minion(MinionDefinitionFixture.build(feeder = FeederSettings(listOf("coal"))))

    private fun breeder() = minion(
        MinionDefinitionFixture.build(
            breeder = BreederSettings(emptyList(), listOf(FakeItemLookup.testable(Material.WHEAT)), "8")
        )
    )

    @Test
    fun `refuses items off the list`() {
        val subject = feeder()

        assertEquals(MinionInput.Deposit.Refused, input.deposit(player, subject, ItemStack(Material.BLAZE_ROD, 5)))

        assertEquals(0, subject.storage.used)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `accepts listed fuel and food`() {
        assertTrue(input.accepts(feeder().state.definition, ItemStack(Material.COAL)))
        assertTrue(input.accepts(breeder().state.definition, ItemStack(Material.WHEAT)))
        assertFalse(input.accepts(breeder().state.definition, ItemStack(Material.COAL)))
    }

    @Test
    fun `deposit keeps the leftover`() {
        val subject = breeder()
        subject.storage.add(ItemStack(Material.WHEAT, 60))

        assertEquals(MinionInput.Deposit.Accepted(4), input.deposit(player, subject, ItemStack(Material.WHEAT, 10)))

        assertEquals(64, subject.storage.getItem(0)?.amount)
        assertEquals(listOf(subject), saved)
    }

    @Test
    fun `withdraw puts back what does not fit`() {
        val subject = breeder()
        subject.storage.add(ItemStack(Material.WHEAT, 64))
        val inventory = mockk<PlayerInventory> {
            every { addItem(*anyVararg()) } returns hashMapOf(0 to ItemStack(Material.WHEAT, 20))
        }

        assertEquals(44, input.withdraw(playerWith(inventory), subject, ItemStack(Material.WHEAT)))

        assertEquals(20, subject.storage.getItem(0)?.amount)
        assertEquals(listOf(subject), saved)
        verify(exactly = 1) { inventory.addItem(*anyVararg()) }
    }

    @Test
    fun `withdraw empties the slot when everything fits`() {
        val subject = breeder()
        subject.storage.add(ItemStack(Material.WHEAT, 10))
        val inventory = roomyInventory()

        assertEquals(10, input.withdraw(playerWith(inventory), subject, ItemStack(Material.WHEAT)))

        assertNull(subject.storage.getItem(0))
    }

    @Test
    fun `a cancelled deposit event moves nothing`() {
        val subject = breeder()
        val cancelling = inputWith(RecordingEventDispatcher(allow = false))

        assertEquals(MinionInput.Deposit.Cancelled, cancelling.deposit(player, subject, ItemStack(Material.WHEAT, 10)))

        assertEquals(0, subject.storage.used)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `a full minion accepts nothing`() {
        val subject = breeder()
        subject.storage.add(ItemStack(Material.WHEAT, 64))

        assertEquals(MinionInput.Deposit.Accepted(0), input.deposit(player, subject, ItemStack(Material.WHEAT, 10)))
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `the deposit event carries the player minion and item`() {
        val subject = breeder()

        input.deposit(player, subject, ItemStack(Material.WHEAT, 10))

        val event = events.last<MinionDepositEvent>()
        assertSame(player, event.player)
        assertSame(subject, event.minion)
        assertEquals(10, event.item.amount)
    }

    @Test
    fun `a refused item fires no deposit event`() {
        input.deposit(player, feeder(), ItemStack(Material.BLAZE_ROD, 5))

        assertTrue(events.events.isEmpty())
    }

    @Test
    fun `a cancelled extract event on withdraw moves nothing`() {
        val subject = breeder()
        subject.storage.add(ItemStack(Material.WHEAT, 10))
        val inventory = roomyInventory()

        val cancelling = inputWith(RecordingEventDispatcher(allow = false))

        assertEquals(0, cancelling.withdraw(playerWith(inventory), subject, ItemStack(Material.WHEAT)))

        assertEquals(10, subject.storage.getItem(0)?.amount)
        assertTrue(saved.isEmpty())
        verify(exactly = 0) { inventory.addItem(*anyVararg()) }
    }

    @Test
    fun `the extract event carries the stack about to be taken`() {
        val subject = breeder()
        subject.storage.add(ItemStack(Material.WHEAT, 10))

        input.withdraw(playerWith(roomyInventory()), subject, ItemStack(Material.WHEAT))

        val event = events.last<MinionExtractEvent>()
        assertSame(subject, event.minion)
        assertEquals(listOf(10), event.items.map { it.amount })
    }

    @Test
    fun `a listener that lowers the amount withdraws only that amount`() {
        val subject = breeder()
        subject.storage.add(ItemStack(Material.WHEAT, 10))
        val lowering = RecordingEventDispatcher(edit = { event ->
            if (event is MinionExtractEvent) event.items.forEach { it.amount = 3 }
        })

        assertEquals(3, inputWith(lowering).withdraw(playerWith(roomyInventory()), subject, ItemStack(Material.WHEAT)))

        assertEquals(7, subject.storage.getItem(0)?.amount)
    }

    @Test
    fun `an empty withdraw fires no event`() {
        assertEquals(0, input.withdraw(playerWith(roomyInventory()), breeder(), ItemStack(Material.WHEAT)))

        assertTrue(events.events.isEmpty())
    }

    @Test
    fun `takesInput is false for a miner`() {
        assertFalse(input.takesInput(MinionDefinitionFixture.build()))
        assertTrue(input.takesInput(feeder().state.definition))
        assertTrue(input.takesInput(breeder().state.definition))
    }
}
