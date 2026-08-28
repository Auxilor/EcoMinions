package com.exanthiax.ecominions.minions.ui

import com.willfp.eco.core.gui.menu.Menu
import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MenuViewersTest {

    private val refreshed = mutableListOf<Pair<Menu, Player>>()
    private val viewers = MenuViewers { menu, player -> refreshed += menu to player }

    private fun player(online: Boolean = true): Player =
        mockk<Player>(relaxed = true).also { every { it.isOnline } returns online }

    @Test
    fun `a watched menu is refreshed on every tick`() {
        val player = player()
        val menu = mockk<Menu>()

        viewers.watch(player, menu)
        viewers.tick()
        viewers.tick()

        assertEquals(listOf(menu to player, menu to player), refreshed)
    }

    @Test
    fun `a closed menu stops being refreshed`() {
        val player = player()
        val menu = mockk<Menu>()

        viewers.watch(player, menu)
        viewers.forget(player, menu)
        viewers.tick()

        assertEquals(emptyList<Pair<Menu, Player>>(), refreshed)
    }

    @Test
    fun `closing the previous menu does not stop the one that replaced it`() {
        val player = player()
        val previous = mockk<Menu>()
        val current = mockk<Menu>()

        viewers.watch(player, previous)
        viewers.watch(player, current)
        // Bukkit fires the close of the old inventory after the new one opens.
        viewers.forget(player, previous)
        viewers.tick()

        assertEquals(listOf(current to player), refreshed)
    }

    @Test
    fun `an offline player is dropped`() {
        val player = player(online = false)

        viewers.watch(player, mockk())
        viewers.tick()
        viewers.tick()

        assertEquals(emptyList<Pair<Menu, Player>>(), refreshed)
    }
}
