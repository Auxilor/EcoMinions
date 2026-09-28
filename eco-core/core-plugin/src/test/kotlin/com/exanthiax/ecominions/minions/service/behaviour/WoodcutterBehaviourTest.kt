package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.minions.model.TargetSet
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MiningState
import com.exanthiax.ecominions.minions.service.TreeService
import com.exanthiax.ecominions.testsupport.FakeBlockLookup
import com.exanthiax.ecominions.testsupport.FakeWorld
import io.mockk.every
import io.mockk.mockk
import org.bukkit.Material
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WoodcutterBehaviourTest {

    private val behaviour = WoodcutterBehaviour(TreeService())

    /** The point of a tree minion: something that looks like a tree stands there. */
    @Test
    fun `a grown tree keeps its leaves`() {
        val world = FakeWorld()
        val minion = grownTree(world)

        assertTrue(world.countOf(Material.OAK_LEAVES) > 0, "the canopy should be standing")
        assertTrue(world.countOf(Material.OAK_LOG) > 0, "the trunk should be standing")
        assertNotEquals(null, minion.mining.standingTree)
    }

    /** Leaves are not a target block, so felling the trunk is what collects them. */
    @Test
    fun `harvests the leaves once the last log is felled`() {
        val world = FakeWorld()
        val minion = grownTree(world)
        val logs = minion.mining.standingTree!!.logs

        for (log in logs) {
            behaviour.onBlockBroken(minion, log, definition = null, original = Material.OAK_LOG, now = 500L)
        }

        assertEquals(0, world.countOf(Material.OAK_LEAVES))
        assertEquals(0, world.countOf(Material.OAK_LOG))
        assertEquals(null, minion.mining.standingTree)
    }

    /** A half-felled tree is still a tree - the canopy stays up until the trunk is gone. */
    @Test
    fun `keeps the leaves while any log is still standing`() {
        val world = FakeWorld()
        val minion = grownTree(world)
        val logs = minion.mining.standingTree!!.logs

        behaviour.onBlockBroken(minion, logs.first(), definition = null, original = Material.OAK_LOG, now = 500L)

        assertTrue(world.countOf(Material.OAK_LEAVES) > 0)
        assertNotEquals(null, minion.mining.standingTree)
    }

    /** A sapling is never planted where its tree would grow into a neighbour's. */
    @Test
    fun `plants a sapling clear of existing logs`() {
        val world = FakeWorld()
        world.fillFloor(Material.DIRT)
        world.set(-2, 0, 0, Material.OAK_LOG)
        val minion = minion(world, spacing = 2)

        assertTrue(behaviour.onIdle(minion, now = 0L))

        val (x, _, _) = world.positionsOf(Material.OAK_SAPLING).single()
        assertTrue(x > 0, "planted within 2 blocks of the log at x=$x")
    }

    /** Nowhere far enough from other trees means nothing is planted. */
    @Test
    fun `plants nothing when every spot is too close`() {
        val world = FakeWorld()
        world.fillFloor(Material.DIRT)
        world.set(2, 0, 0, Material.OAK_LOG)
        world.set(-2, 0, 0, Material.OAK_LOG)
        val minion = minion(world, spacing = 2)

        assertFalse(behaviour.onIdle(minion, now = 0L))
        assertEquals(0, world.countOf(Material.OAK_SAPLING))
    }

    /** Plants a sapling, waits out the regen delay, then grows the tree. */
    private fun grownTree(world: FakeWorld): Minion {
        world.fillFloor(Material.DIRT)
        val minion = minion(world)

        assertTrue(behaviour.onIdle(minion, now = 0L), "should have planted a sapling")
        assertEquals(Material.OAK_SAPLING, world.typeAt(-1, 0, 0))

        assertTrue(behaviour.onIdle(minion, now = REGEN_DELAY_TICKS.toLong()), "should have grown the tree")
        assertEquals(Material.OAK_LOG, world.typeAt(-1, 0, 0), "the sapling becomes the base of the trunk")
        return minion
    }

    private fun minion(world: FakeWorld, spacing: Int = 0): Minion {
        val oak = MinionBlock.regenDefault("oak", "OAK_LOG", FakeBlockLookup.testable(Material.OAK_LOG))
            .copy(
                plant = FakeBlockLookup.testable(Material.OAK_SAPLING),
                leaves = FakeBlockLookup.testable(Material.OAK_LEAVES),
                vanillaMaterial = Material.OAK_LOG
            )
        val definition = MinionDefinitionFixture.build(
            type = MinionType.WOODCUTTER,
            targetBlocks = TargetSet(listOf(oak)),
            placement = PlacementSettings(
                liftRegen = false,
                surfaceBlock = null,
                selfSeed = true,
                seedBlock = FakeBlockLookup.testable(Material.OAK_SAPLING),
                spacing = spacing
            )
        )
        return mockk<Minion>().also { minion ->
            every { minion.state } returns mockk { every { this@mockk.definition } returns definition }
            every { minion.stats } returns mockk {
                every { this@mockk.radius } returns 1
                every { this@mockk.height } returns 6
                every { this@mockk.regenDelayTicks } returns REGEN_DELAY_TICKS
            }
            every { minion.entity } returns world.stand()
            every { minion.mining } returns MiningState()
            every { minion.storage } returns mockk(relaxed = true)
        }
    }

    private companion object {
        const val REGEN_DELAY_TICKS = 60
    }
}
