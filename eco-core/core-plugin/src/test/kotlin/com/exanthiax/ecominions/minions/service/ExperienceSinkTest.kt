package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.ExperienceMode
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.model.SellPricingSettings
import io.mockk.mockk
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExperienceSinkTest {

    private fun settings(mode: ExperienceMode) = SettingsHolder(
        MinionSettings(
            hologramOffset = 1.6,
            hologramRefreshTicks = 20,
            invulnerable = true,
            showRegenMarker = true,
            regenMarkerFallback = Material.AIR,
            clearSeededOnPickup = false,
            hardSlotCap = 54,
            offlineEnabled = true,
            offlineEfficiency = 1.0,
            limitsEnabled = true,
            defaultLimit = 5,
            permissionOverrides = true,
            sellPricing = SellPricingSettings(autoFromEcoShop = false, multiplier = 1.0),
            experienceMode = mode
        )
    )

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

    private fun minion() = Minion(MinionState(definition = MinionDefinitionFixture.build()), mockk(relaxed = true), MinionStorage(1), stats)

    @Test
    fun `store mode adds to the counter`() {
        val orbs = mutableListOf<Int>()
        val subject = minion()
        val sink = ExperienceSink(settings(ExperienceMode.STORE)) { _, amount -> orbs += amount }

        sink.add(subject, 5)
        sink.add(subject, 5)

        assertEquals(10, subject.state.storedExperience)
        assertTrue(orbs.isEmpty())
    }

    @Test
    fun `orb mode spawns an orb and stores nothing`() {
        val orbs = mutableListOf<Int>()
        val subject = minion()

        ExperienceSink(settings(ExperienceMode.ORB)) { _, amount -> orbs += amount }.add(subject, 5)

        assertEquals(listOf(5), orbs)
        assertEquals(0, subject.state.storedExperience)
    }

    @Test
    fun `store ignores the mode`() {
        val subject = minion()

        ExperienceSink(settings(ExperienceMode.ORB)) { _, _ -> }.store(subject, 7)

        assertEquals(7, subject.state.storedExperience)
    }

    @Test
    fun `zero and less do nothing`() {
        val subject = minion()
        val sink = ExperienceSink(settings(ExperienceMode.STORE)) { _, _ -> error("no orb expected") }

        sink.add(subject, 0)
        sink.add(subject, -3)

        assertEquals(0, subject.state.storedExperience)
    }
}
