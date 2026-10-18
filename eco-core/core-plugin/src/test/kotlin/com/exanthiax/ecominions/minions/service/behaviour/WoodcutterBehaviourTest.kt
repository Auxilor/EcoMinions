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
import kotlin.math.abs

class WoodcutterBehaviourTest {

    private val behaviour = WoodcutterBehaviour(TreeService())

    /** The point of a tree minion: something that looks like a tree stands there. */
    @Test
    fun `a grown tree keeps its leaves`() {
        val world = FakeWorld()
        val minion = grownTree(world)

        assertTrue(world.countOf(Material.OAK_LEAVES) > 0, "the canopy should be standing")
        assertTrue(world.countOf(Material.OAK_LOG) > 0, "the trunk should be standing")
        assertEquals(1, minion.mining.standingTrees.size)
    }

    /** Leaves are not a target block, so felling the trunk is what collects them. */
    @Test
    fun `harvests the leaves once the last log is felled`() {
        val world = FakeWorld()
        val minion = grownTree(world)
        val logs = minion.mining.standingTrees.single().logs

        for (log in logs) {
            behaviour.onBlockBroken(minion, log, definition = null, original = Material.OAK_LOG, now = 500L)
        }

        assertEquals(0, world.countOf(Material.OAK_LEAVES))
        assertEquals(0, world.countOf(Material.OAK_LOG))
        assertTrue(minion.mining.standingTrees.isEmpty())
    }

    /** A half-felled tree is still a tree - the canopy stays up until the trunk is gone. */
    @Test
    fun `keeps the leaves while any log is still standing`() {
        val world = FakeWorld()
        val minion = grownTree(world)
        val logs = minion.mining.standingTrees.single().logs

        behaviour.onBlockBroken(minion, logs.first(), definition = null, original = Material.OAK_LOG, now = 500L)

        assertTrue(world.countOf(Material.OAK_LEAVES) > 0)
        assertEquals(1, minion.mining.standingTrees.size)
    }

    /** A canopy never grows through the minion standing in the middle. */
    @Test
    fun `keeps leaves out of the minion's column`() {
        val world = FakeWorld()
        grownTree(world)

        for (y in 0..MINION_COLUMN_TOP) {
            assertNotEquals(Material.OAK_LEAVES, world.typeAt(0, y, 0))
        }
    }

    /** A sapling is never planted where its tree would grow into a neighbour's. */
    @Test
    fun `plants a sapling clear of existing logs`() {
        val world = FakeWorld()
        world.fillFloor(Material.DIRT)
        world.set(-2, 0, 0, Material.OAK_LOG)
        val minion = minion(world, spacing = 1)

        assertTrue(behaviour.onBeforeMine(minion, now = 0L))

        val sapling = world.positionsOf(Material.OAK_SAPLING).single()
        assertFalse(sapling in listOf(Triple(-2, 0, -1), Triple(-2, 0, 1)), "planted beside the log")
    }

    @Test
    fun `plants no sapling where the owner cannot build`() {
        val world = FakeWorld()
        world.fillFloor(Material.DIRT)
        val protectedBehaviour = WoodcutterBehaviour(TreeService()) { _, block -> block.x >= 0 }
        val minion = minion(world)

        while (protectedBehaviour.onBeforeMine(minion, now = 0L)) {
            // plant until nowhere is left
        }

        assertTrue(world.countOf(Material.OAK_SAPLING) > 0)
        assertTrue(world.positionsOf(Material.OAK_SAPLING).all { it.first >= 0 })
    }

    @Test
    fun `a tree grows no leaves where the owner cannot build`() {
        val world = FakeWorld()
        world.fillFloor(Material.DIRT)
        val protectedBehaviour = WoodcutterBehaviour(TreeService()) { _, block -> block.y < 1 || block.x != -1 }
        val minion = minion(world)
        assertTrue(protectedBehaviour.onBeforeMine(minion, now = 0L))
        minion.mining.saplings.keys.retainAll { it.blockX == -2 && it.blockZ == 0 }

        protectedBehaviour.onBeforeMine(minion, now = REGEN_DELAY_TICKS.toLong())

        assertEquals(Material.OAK_LOG, world.typeAt(-2, 0, 0))
        assertTrue(world.countOf(Material.OAK_LEAVES) > 0)
        assertTrue(world.positionsOf(Material.OAK_LEAVES).none { it.first == -1 })
    }

    /** The minion counts as a tree for spacing, so nothing is planted right beside it. */
    @Test
    fun `plants nothing within spacing of the minion`() {
        val world = FakeWorld()
        world.fillFloor(Material.DIRT)
        val minion = minion(world, spacing = 2)

        assertFalse(behaviour.onBeforeMine(minion, now = 0L))
        assertEquals(0, world.countOf(Material.OAK_SAPLING))
    }

    /** The radius fills with several trees kept apart, not one tree by the minion. */
    @Test
    fun `plants several spaced saplings`() {
        val world = FakeWorld()
        world.fillFloor(Material.DIRT)
        val minion = minion(world, spacing = 1)

        repeat(SPACED_SAPLINGS) {
            assertTrue(behaviour.onBeforeMine(minion, now = 0L), "stopped after $it saplings")
        }

        val saplings = world.positionsOf(Material.OAK_SAPLING)
        for (first in saplings) {
            for (second in saplings) {
                val (x1, _, z1) = first
                val (x2, _, z2) = second
                val besideOnAxis = (x1 == x2 && abs(z1 - z2) == 1) || (z1 == z2 && abs(x1 - x2) == 1)
                assertFalse(besideOnAxis, "$first and $second are side by side")
            }
        }
    }

    /** Plants a sapling, waits out the regen delay, then grows the tree. */
    private fun grownTree(world: FakeWorld): Minion {
        world.fillFloor(Material.DIRT)
        val minion = minion(world)

        assertTrue(behaviour.onBeforeMine(minion, now = 0L), "should have planted a sapling")
        assertEquals(Material.OAK_SAPLING, world.typeAt(-2, 0, 0))
        minion.mining.saplings.keys.retainAll { it.blockX == -2 && it.blockZ == 0 }

        behaviour.onBeforeMine(minion, now = REGEN_DELAY_TICKS.toLong())
        assertEquals(Material.OAK_LOG, world.typeAt(-2, 0, 0), "the sapling becomes the base of the trunk")
        world.positionsOf(Material.OAK_SAPLING).forEach { (x, y, z) -> world.set(x, y, z, Material.AIR) }
        minion.mining.saplings.clear()
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
                seedBlocks = listOf(FakeBlockLookup.testable(Material.OAK_SAPLING)),
                spacing = spacing
            )
        )
        return mockk<Minion>().also { minion ->
            every { minion.state } returns mockk { every { this@mockk.definition } returns definition }
            every { minion.stats } returns mockk {
                every { this@mockk.radius } returns 2
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
        const val MINION_COLUMN_TOP = 5
        const val SPACED_SAPLINGS = 4
    }
}
