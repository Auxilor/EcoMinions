package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.api.minion.LimitBreach
import com.exanthiax.ecominions.api.minion.LimitScope
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionLimits
import com.exanthiax.ecominions.minions.service.OwnerCaps
import com.exanthiax.ecominions.minions.service.PlacementLimits
import io.mockk.every
import io.mockk.mockk
import org.bukkit.entity.ArmorStand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class MinionPlaceholdersTest {
    private val owner = UUID.randomUUID()
    private val coal = MinionDefinitionFixture.build(id = "coal", limit = 2, name = "Coal Minion")
    private val messages = mockk<Messages> {
        every { line("infinity") } returns "∞"
        every { line("limit-names.player") } returns "total"
        every { line("gui.main.limit.status-active") } returns "Working"
        every { line("gui.main.limit.status-limited") } returns "Inactive"
    }
    private val limits = mockk<MinionLimits> {
        every { capsFor(owner) } returns OwnerCaps(PlacementLimits.UNLIMITED, emptyMap(), mapOf("coal" to 2))
        every { countOf(owner, LimitScope.MINION, coal) } returns 3
        every { countOf(owner, LimitScope.PLAYER, coal) } returns 3
        every { countOfType(owner, MinionType.MINER) } returns 3
    }
    private val placeholders = MinionPlaceholders(mockk(), mockk(), limits, messages).internalPlaceholders()
        .associateBy { it.key }

    private fun minion(limitedSince: Long?, customName: String? = null): Minion {
        val stats = MinionStats(
            MinionDefinition.BaseStats(
                speedTicks = "40",
                efficiencyTicks = "10",
                radius = "2",
                storageSlots = "1",
                maxStorageSlots = "54",
                hardSlotCap = 54,
                regenDelayTicks = "60"
            ),
            level = 1
        )
        return Minion(
            state = MinionState(definition = coal, owner = owner, limitedSince = limitedSince, customName = customName),
            entity = mockk<ArmorStand>(relaxed = true),
            storage = MinionStorage(stats.storageSize),
            stats = stats
        )
    }

    private fun resolve(key: String, minion: Minion): String? =
        placeholders.getValue(key).resolve(TemplateContext.of(MinionPlaceholders.MINION to minion))

    @Test
    fun `limit placeholders show the breach of a limited minion`() {
        val subject = minion(limitedSince = 1_000L)
        every { limits.breachOf(subject) } returns LimitBreach(LimitScope.MINION, 3, 2)

        assertEquals("Inactive", resolve("limit_status", subject))
        assertEquals("Coal Minion", resolve("limit_name", subject))
        assertEquals("3", resolve("limit_count", subject))
        assertEquals("2", resolve("limit_max", subject))
        assertEquals("3", resolve("minion_count", subject))
        assertEquals("2", resolve("minion_limit", subject))
        assertEquals("3", resolve("type_count", subject))
    }

    @Test
    fun `uncapped limits render as infinity`() {
        val subject = minion(limitedSince = null)
        every { limits.breachOf(subject) } returns null

        assertEquals("Working", resolve("limit_status", subject))
        assertEquals("∞", resolve("player_limit", subject))
        assertEquals("∞", resolve("type_limit", subject))
        assertEquals("", resolve("limit_name", subject))
    }

    @Test
    fun `minion name is the type name`() {
        assertEquals("Coal Minion", resolve("minion_name", minion(limitedSince = null)))
    }

    @Test
    fun `minion name uses custom name`() {
        assertEquals("Bob", resolve("minion_name", minion(limitedSince = null, customName = "Bob")))
    }
}
