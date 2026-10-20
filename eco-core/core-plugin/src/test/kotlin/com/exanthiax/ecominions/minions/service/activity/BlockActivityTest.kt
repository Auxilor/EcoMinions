package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import com.exanthiax.ecominions.minions.service.MiningTargetFinder
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.behaviour.MinionBehaviour
import com.exanthiax.ecominions.minions.service.behaviour.MinionBehaviours
import io.mockk.every
import io.mockk.mockk
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class BlockActivityTest {

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

    private val target = mockk<Block>(relaxed = true)
    private val targets = mockk<MiningTargetFinder> { every { find(any()) } returns target }

    private fun minion() = Minion(
        state = MinionState(definition = MinionDefinitionFixture.build(type = MinionType.MINER)),
        entity = mockk<ArmorStand>(relaxed = true),
        storage = MinionStorage(1) { a, b -> a.type == b.type },
        stats = stats
    )

    private fun activity(seeds: Boolean): BlockActivity {
        val behaviour = mockk<MinionBehaviour>(relaxed = true) { every { onBeforeMine(any(), any()) } returns seeds }
        val behaviours = mockk<MinionBehaviours> { every { this@mockk[any()] } returns behaviour }
        return BlockActivity(targets, mockk(relaxed = true), mockk(relaxed = true), behaviours, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
    }

    @Test
    fun `keeps seeding after a seeded cycle while spots are left`() {
        val subject = minion()
        subject.mining.seededLastCycle = true

        assertEquals(ActivityResult.SEEDED, activity(seeds = true).act(subject, 0L))
    }

    @Test
    fun `mines once the area is full`() {
        val subject = minion()
        subject.mining.seededLastCycle = true

        assertEquals(ActivityResult.WORKED, activity(seeds = false).act(subject, 0L))
        assertSame(target, subject.mining.pendingTarget)
    }
}
