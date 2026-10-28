package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.persistence.MinionDataKeys
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.testsupport.FakePersistentDataContainer
import io.mockk.every
import io.mockk.mockk
import org.bukkit.NamespacedKey
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionStateRepositoryTest {
    private val definition = MinionDefinitionFixture.build()
    private val keys = mockk<MinionDataKeys> {
        every { isMinion } returns NamespacedKey("ecominions", "is_minion")
        every { type } returns NamespacedKey("ecominions", "type")
        every { level } returns NamespacedKey("ecominions", "level")
        every { xp } returns NamespacedKey("ecominions", "xp")
        every { owner } returns NamespacedKey("ecominions", "owner")
        every { upgrades } returns NamespacedKey("ecominions", "upgrades")
        every { disabledUpgrades } returns NamespacedKey("ecominions", "disabled_upgrades")
        every { inventory } returns NamespacedKey("ecominions", "inventory")
        every { minionUuid } returns NamespacedKey("ecominions", "uuid")
        every { shutdownTimestamp } returns NamespacedKey("ecominions", "shutdown_ts")
        every { fuelId } returns NamespacedKey("ecominions", "fuel_id")
        every { fuelRemaining } returns NamespacedKey("ecominions", "fuel_remaining")
        every { storedExperience } returns NamespacedKey("ecominions", "stored_experience")
        every { killSample } returns NamespacedKey("ecominions", "kill_sample")
        every { limitedSince } returns NamespacedKey("ecominions", "limited_ts")
        every { customName } returns NamespacedKey("ecominions", "custom_name")
    }
    private val repository = MinionStateRepository(keys) { id -> definition.takeIf { it.id == id } }

    @Test
    fun `stored experience round trips`() {
        val container = FakePersistentDataContainer.create()

        repository.write(container, MinionState(definition).withStoredExperience(42))

        assertEquals(42, repository.read(container)!!.storedExperience)
    }

    @Test
    fun `a container from before stored experience reads zero`() {
        val container = FakePersistentDataContainer.create()
        repository.write(container, MinionState(definition))
        container.remove(keys.storedExperience)

        assertEquals(0, repository.read(container)!!.storedExperience)
    }

    @Test
    fun `a container without a kill sample reads an empty one`() {
        val container = FakePersistentDataContainer.create()
        repository.write(container, MinionState(definition))

        assertTrue(repository.read(container)!!.killSample.isEmpty())
    }

    @Test
    fun `limitedSince survives a write and read`() {
        val container = FakePersistentDataContainer.create()

        repository.write(container, MinionState(definition, limitedSince = 1_234L))

        assertEquals(1_234L, repository.read(container)!!.limitedSince)
    }

    @Test
    fun `custom name round trips`() {
        val container = FakePersistentDataContainer.create()

        repository.write(container, MinionState(definition, customName = "Bob"))

        assertEquals("Bob", repository.read(container)!!.customName)
    }

    @Test
    fun `no custom name reads null`() {
        val container = FakePersistentDataContainer.create()
        repository.write(container, MinionState(definition, customName = "Bob"))

        repository.write(container, MinionState(definition))

        assertNull(repository.read(container)!!.customName)
    }
    
    @Test
    fun `disabled upgrades round trip`() {
        val container = FakePersistentDataContainer.create()

        repository.write(container, MinionState(definition, disabledUpgrades = setOf("auto_sell", "speed")))

        assertEquals(setOf("auto_sell", "speed"), repository.read(container)!!.disabledUpgrades)
    }

    @Test
    fun `old data reads all enabled`() {
        val container = FakePersistentDataContainer.create()
        repository.write(container, MinionState(definition))

        assertTrue(repository.read(container)!!.disabledUpgrades.isEmpty())
    }
}
