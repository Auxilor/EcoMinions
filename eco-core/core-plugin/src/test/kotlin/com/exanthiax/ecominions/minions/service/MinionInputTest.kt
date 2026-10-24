package com.exanthiax.ecominions.minions.service

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
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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

    private val input = MinionInput(fuels) { saved += it }

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

        assertEquals(0, input.deposit(subject, ItemStack(Material.BLAZE_ROD, 5)))

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

        assertEquals(4, input.deposit(subject, ItemStack(Material.WHEAT, 10)))

        assertEquals(64, subject.storage.getItem(0)?.amount)
        assertEquals(listOf(subject), saved)
    }

    @Test
    fun `withdraw puts back what does not fit`() {
        val subject = breeder()
        subject.storage.add(ItemStack(Material.WHEAT, 64))
        val inventory = mockk<Inventory> {
            every { addItem(*anyVararg()) } returns hashMapOf(0 to ItemStack(Material.WHEAT, 20))
        }

        assertEquals(44, input.withdraw(subject, ItemStack(Material.WHEAT), inventory))

        assertEquals(20, subject.storage.getItem(0)?.amount)
        assertEquals(listOf(subject), saved)
        verify(exactly = 1) { inventory.addItem(*anyVararg()) }
    }

    @Test
    fun `withdraw empties the slot when everything fits`() {
        val subject = breeder()
        subject.storage.add(ItemStack(Material.WHEAT, 10))
        val inventory = mockk<Inventory> { every { addItem(*anyVararg()) } returns hashMapOf() }

        assertEquals(10, input.withdraw(subject, ItemStack(Material.WHEAT), inventory))

        assertNull(subject.storage.getItem(0))
    }

    @Test
    fun `takesInput is false for a miner`() {
        assertFalse(input.takesInput(MinionDefinitionFixture.build()))
        assertTrue(input.takesInput(feeder().state.definition))
        assertTrue(input.takesInput(breeder().state.definition))
    }
}
