package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TreeShapeTest {

    private fun TreeShape.logs() = blocks().filter { it.kind == TreeBlock.Kind.LOG }
    private fun TreeShape.leaves() = blocks().filter { it.kind == TreeBlock.Kind.LEAF }

    @Test
    fun `the trunk is a straight column from the sapling`() {
        val logs = TreeShape(trunk = 4, radius = 2, layers = 2, style = TreeShape.Style.ROUND).logs()

        assertEquals(listOf(0, 1, 2, 3), logs.map { it.y })
        assertTrue(logs.all { it.x == 0 && it.z == 0 }, "the trunk should not wander")
    }

    @Test
    fun `no leaf ever occupies the trunk column`() {
        for (style in TreeShape.Style.entries) {
            val shape = TreeShape(trunk = 4, radius = 3, layers = 2, style = style)
            val trunkYs = shape.logs().map { it.y }.toSet()

            assertTrue(
                shape.leaves().none { it.x == 0 && it.z == 0 && it.y in trunkYs },
                "$style put a leaf inside its own trunk"
            )
        }
    }

    @Test
    fun `nothing is placed twice`() {
        for (style in TreeShape.Style.entries) {
            val blocks = TreeShape(trunk = 4, radius = 3, layers = 3, style = style).blocks()

            assertEquals(
                blocks.size,
                blocks.map { Triple(it.x, it.y, it.z) }.toSet().size,
                "$style placed two blocks in the same position"
            )
        }
    }

    @Test
    fun `a round canopy loses its top corners`() {
        val shape = TreeShape(trunk = 4, radius = 2, layers = 2, style = TreeShape.Style.ROUND)
        val top = shape.leaves().filter { it.y == 3 }

        assertFalse(
            top.any { it.x == 2 && it.z == 2 },
            "the topmost round layer should be trimmed, or the canopy reads as a cube"
        )
        assertTrue(shape.leaves().any { it.y == 2 && it.x == 2 && it.z == 2 }, "lower layers stay square")
    }

    @Test
    fun `a conifer narrows towards the top`() {
        val shape = TreeShape(trunk = 4, radius = 3, layers = 3, style = TreeShape.Style.CONIFER)
        val widthAt = { y: Int -> shape.leaves().filter { it.y == y }.maxOfOrNull { kotlin.math.abs(it.x) } ?: 0 }

        assertTrue(widthAt(1) > widthAt(3), "a spruce should be widest at the bottom")
    }

    @Test
    fun `a flat canopy is a single wide slab`() {
        val shape = TreeShape(trunk = 4, radius = 3, layers = 1, style = TreeShape.Style.FLAT)
        val canopyLevels = shape.leaves().map { it.y }.toSet()

        assertEquals(setOf(3, 4), canopyLevels, "an acacia is one slab plus its crown")
    }

    @Test
    fun `every block of a default tree is within a woodcutter's reach`() {
        val shape = TreeShape.DEFAULT

        assertTrue(
            shape.logs().all { it.y <= TreeShape.MAX_REACHABLE_TRUNK - 1 },
            "a log the minion cannot reach would be left standing forever"
        )
    }

    @Test
    fun `a one-block canopy keeps its corners`() {
        val shape = TreeShape(trunk = 4, radius = 1, layers = 1, style = TreeShape.Style.ROUND)

        assertTrue(
            shape.leaves().any { it.y == 3 && it.x == 1 && it.z == 1 },
            "trimming a radius-1 canopy leaves a plus, which reads as a shrub"
        )
        assertEquals(13, shape.blocks().size, "the generic tree is 4 logs, an 8-leaf ring and a crown")
    }

    @Test
    fun `a wider tree gets a plus-shaped crown and a narrow one a single leaf`() {
        val narrow = TreeShape(trunk = 4, radius = 1, layers = 1, style = TreeShape.Style.ROUND)
        val wide = TreeShape(trunk = 4, radius = 3, layers = 2, style = TreeShape.Style.ROUND)

        assertEquals(1, narrow.leaves().count { it.y == 4 })
        assertEquals(5, wide.leaves().count { it.y == 4 })
    }
}
