package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.api.island.IslandProvider
import com.exanthiax.ecominions.testsupport.FakeIsland
import com.exanthiax.ecominions.testsupport.FakeIslandProvider
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Location
import org.bukkit.World
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class IslandRegistryTest {
    private val warnings = mutableListOf<String>()
    private val registry = IslandRegistry { warnings += it }
    private val world = mockk<World> { every { name } returns "world" }
    private val location = Location(world, 1.0, 64.0, 2.0)
    private val owner = UUID.randomUUID()

    @Test
    fun `no providers finds no island`() {
        assertNull(registry.islandAt(location))
        assertTrue(registry.isEmpty())
    }

    @Test
    fun `the first provider to claim a location wins`() {
        val first = FakeIslandProvider("first").apply { claim(FakeIsland("first", "a", owner), location) }
        val second = FakeIslandProvider("second").apply { claim(FakeIsland("second", "b", owner), location) }
        registry.register(first)
        registry.register(second)

        assertEquals("first", registry.islandAt(location)?.provider)
    }

    @Test
    fun `a provider that throws counts as no island`() {
        val failing = mockk<IslandProvider> {
            every { id } returns "failing"
            every { islandAt(any()) } throws IllegalStateException("island deleted")
        }
        registry.register(failing)

        assertNull(registry.islandAt(location))
        assertNull(registry.islandAt(location))
        assertEquals(1, warnings.size)
        assertTrue("failing" in warnings.single() && "island deleted" in warnings.single())
    }

    @Test
    fun `a throwing provider does not hide a later one`() {
        val failing = mockk<IslandProvider> {
            every { id } returns "failing"
            every { islandAt(any()) } throws IllegalStateException("boom")
        }
        registry.register(failing)
        registry.register(FakeIslandProvider("good").apply { claim(FakeIsland("good", "a", owner), location) })

        assertEquals("good", registry.islandAt(location)?.provider)
    }

    @Test
    fun `islandsOf skips a provider that throws`() {
        val failing = mockk<IslandProvider> {
            every { id } returns "failing"
            every { islandsOf(any()) } throws IllegalStateException("boom")
        }
        registry.register(failing)
        registry.register(FakeIslandProvider("good").apply { claim(FakeIsland("good", "a", owner), location) })

        assertEquals(listOf("a"), registry.islandsOf(owner).map { it.id })
        assertEquals(1, warnings.size)
    }

    @Test
    fun `registering the same id replaces the old provider`() {
        registry.register(FakeIslandProvider("same"))
        val replacement = FakeIslandProvider("same")
        registry.register(replacement)

        assertEquals(listOf<IslandProvider>(replacement), registry.providers())
    }

    @Test
    fun `unregister removes the provider`() {
        registry.register(FakeIslandProvider("gone"))
        registry.unregister("gone")

        assertTrue(registry.isEmpty())
    }
}
