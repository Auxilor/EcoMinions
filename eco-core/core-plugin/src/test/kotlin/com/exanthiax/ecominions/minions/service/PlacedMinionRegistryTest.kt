package com.exanthiax.ecominions.minions.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class PlacedMinionRegistryTest {
    private val data = mutableMapOf<UUID, List<String>>()
    private val registry = PlacedMinionRegistry({ data[it].orEmpty() }, { owner, entries -> data[owner] = entries })
    private val owner = UUID.randomUUID()

    @Test
    fun `counts every minion and each type`() {
        registry.add(owner, UUID.randomUUID(), "stone")
        registry.add(owner, UUID.randomUUID(), "stone")
        registry.add(owner, UUID.randomUUID(), "coal")

        assertEquals(3, registry.count(owner))
        assertEquals(2, registry.count(owner, "stone"))
        assertEquals(1, registry.count(owner, "coal"))
    }

    /** A minion records itself every time its chunk loads, so adding twice must not count twice. */
    @Test
    fun `adding the same minion again is a no-op`() {
        val minion = UUID.randomUUID()
        registry.add(owner, minion, "stone")
        registry.add(owner, minion, "stone")

        assertEquals(1, registry.count(owner))
    }

    @Test
    fun `removing forgets only that minion`() {
        val kept = UUID.randomUUID()
        val removed = UUID.randomUUID()
        registry.add(owner, kept, "stone")
        registry.add(owner, removed, "stone")

        registry.remove(owner, removed)

        assertEquals(listOf("$kept:stone"), data[owner])
    }

    @Test
    fun `clearing forgets everything for that owner`() {
        registry.add(owner, UUID.randomUUID(), "stone")

        registry.clear(owner)

        assertEquals(0, registry.count(owner))
    }
}
