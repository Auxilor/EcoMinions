package com.exanthiax.ecominions.libreforge

import com.willfp.libreforge.drops.DropContribution
import com.willfp.libreforge.triggers.event.DropCause
import com.willfp.libreforge.triggers.event.DropContext
import io.mockk.mockk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.Zombie
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * Covers the contribution step only. The dispatch itself builds a libreforge
 * `TriggerData`, which needs a running server.
 */
class MinionTriggerDispatchTest {

    private val location = mockk<Location>(relaxed = true)

    @Test
    fun `block drops take contributed items`() {
        val dispatch = MinionTriggerDispatch(contributions = { _, _, _ -> DropContribution(listOf(ItemStack(Material.DIAMOND)), 0) })

        val contributed = dispatch.contribute(DropCause.BLOCK, DropContext(), location)

        assertEquals(listOf(Material.DIAMOND), contributed.items.map { it.type })
    }

    @Test
    fun `contributed experience is returned`() {
        val dispatch = MinionTriggerDispatch(contributions = { _, _, _ -> DropContribution(emptyList(), 4) })

        assertEquals(4, dispatch.contribute(DropCause.ENTITY, DropContext(), location).xp)
    }

    @Test
    fun `custom drops ask for no contributions`() {
        var asked = false
        val dispatch = MinionTriggerDispatch(contributions = { _, _, _ -> asked = true; DropContribution.EMPTY })

        dispatch.contribute(DropCause.CUSTOM, DropContext(), location)

        assertFalse(asked)
    }

    @Test
    fun `the entity reaches the drop context`() {
        val victim = mockk<Zombie>(relaxed = true)
        var seen: Entity? = null
        val dispatch = MinionTriggerDispatch(contributions = { _, context, _ -> seen = context.entity; DropContribution.EMPTY })

        dispatch.contribute(DropCause.ENTITY, DropContext(entity = victim), location)

        assertSame(victim, seen)
    }
}
