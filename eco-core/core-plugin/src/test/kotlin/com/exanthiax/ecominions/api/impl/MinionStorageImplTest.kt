package com.exanthiax.ecominions.api.impl

import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionStateRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MinionStorageImplTest {

    private val storage = MinionStorage(2) { a, b -> a.type == b.type }
    private val minion = mockk<Minion> { every { storage } returns this@MinionStorageImplTest.storage }
    private val repository = mockk<MinionStateRepository>(relaxed = true)
    private val handle = MinionStorageImpl(minion, repository)

    @Test
    fun `adding saves and reports overflow`() {
        handle.add(ItemStack(Material.COBBLESTONE, 64))
        handle.add(ItemStack(Material.COBBLESTONE, 64))

        val leftover = handle.add(ItemStack(Material.COBBLESTONE, 10))

        assertEquals(10, leftover?.amount)
        assertEquals(128, handle.countOf(Material.COBBLESTONE))
        verify(exactly = 3) { repository.save(minion) }
    }

    @Test
    fun `contents are copies`() {
        handle.add(ItemStack(Material.DIAMOND, 5))

        handle.contents()[0]?.amount = 1

        assertEquals(5, handle.getItem(0)?.amount)
    }

    @Test
    fun `clearing empties and saves`() {
        handle.add(ItemStack(Material.DIAMOND, 5))

        handle.clear()

        assertNull(handle.getItem(0))
        assertEquals(0, handle.used)
        verify(exactly = 2) { repository.save(minion) }
    }
}
