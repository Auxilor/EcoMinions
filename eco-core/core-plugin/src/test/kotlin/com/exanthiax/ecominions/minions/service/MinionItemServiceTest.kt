package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.event.MinionGiveEvent
import com.exanthiax.ecominions.api.event.MinionTakeEvent
import com.exanthiax.ecominions.core.event.RecordingEventDispatcher
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinionItemServiceTest {

    private val type = MinionDefinitionFixture.build()
    private val built = slot<MinionState>()
    private val itemFactory = mockk<MinionItemFactory> {
        every { build(capture(built)) } answers { ItemStack(Material.PLAYER_HEAD) }
        every { read(any()) } answers {
            if (firstArg<ItemStack>().type == Material.PLAYER_HEAD) MinionState(definition = type) else null
        }
    }

    private val slots = arrayOfNulls<ItemStack>(4)
    private val inventory = mockk<PlayerInventory>(relaxed = true) {
        every { size } returns slots.size
        every { getItem(any<Int>()) } answers { slots[firstArg()] }
        every { setItem(any<Int>(), any()) } answers { slots[firstArg()] = secondArg() }
        every { contents } answers { slots.copyOf() }
        every { addItem(*anyVararg()) } returns hashMapOf()
    }
    private val player = mockk<Player>(relaxed = true) { every { inventory } returns this@MinionItemServiceTest.inventory }

    @Test
    fun `giving builds the listener's level and amount`() {
        val events = RecordingEventDispatcher { event ->
            (event as? MinionGiveEvent)?.apply {
                level = 4
                amount = 3
            }
        }

        val given = MinionItemService(itemFactory, events).give(player, type, 1, 1)

        assertEquals(3, given)
        assertEquals(4, built.captured.level)
    }

    @Test
    fun `a cancelled give builds nothing`() {
        val given = MinionItemService(itemFactory, RecordingEventDispatcher(allow = false)).give(player, type, 1, 1)

        assertEquals(0, given)
        verify(exactly = 0) { itemFactory.build(any()) }
    }

    @Test
    fun `taking removes only minion items, up to the amount`() {
        slots[0] = ItemStack(Material.PLAYER_HEAD, 2)
        slots[1] = ItemStack(Material.STONE, 5)
        slots[2] = ItemStack(Material.PLAYER_HEAD, 3)

        val taken = MinionItemService(itemFactory, RecordingEventDispatcher()).take(player, type, 4)

        assertEquals(4, taken)
        assertEquals(null, slots[0])
        assertEquals(5, slots[1]?.amount)
        assertEquals(1, slots[2]?.amount)
    }

    @Test
    fun `a listener can lower how many are taken`() {
        slots[0] = ItemStack(Material.PLAYER_HEAD, 5)
        val events = RecordingEventDispatcher { (it as? MinionTakeEvent)?.amount = 2 }

        val taken = MinionItemService(itemFactory, events).take(player, type, 5)

        assertEquals(2, taken)
        assertEquals(3, slots[0]?.amount)
    }

    @Test
    fun `counting sums every minion stack`() {
        slots[0] = ItemStack(Material.PLAYER_HEAD, 2)
        slots[3] = ItemStack(Material.PLAYER_HEAD, 6)

        assertEquals(8, MinionItemService(itemFactory, RecordingEventDispatcher()).count(player, type))
    }
}
