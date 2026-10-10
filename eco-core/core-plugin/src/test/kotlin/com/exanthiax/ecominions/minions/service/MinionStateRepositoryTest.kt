package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.persistence.MinionDataKeys
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.testsupport.FakePersistentDataContainer
import io.mockk.every
import io.mockk.mockk
import org.bukkit.NamespacedKey
import org.junit.jupiter.api.Assertions.assertEquals
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
        every { inventory } returns NamespacedKey("ecominions", "inventory")
        every { minionUuid } returns NamespacedKey("ecominions", "uuid")
        every { shutdownTimestamp } returns NamespacedKey("ecominions", "shutdown_ts")
        every { fuelId } returns NamespacedKey("ecominions", "fuel_id")
        every { fuelRemaining } returns NamespacedKey("ecominions", "fuel_remaining")
        every { storedExperience } returns NamespacedKey("ecominions", "stored_experience")
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
}
