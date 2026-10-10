package com.exanthiax.ecominions.minions.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoundaryBoxTest {

    /**
     * The outline has to trace the blocks [MiningTargetFinder] actually scans:
     * x and z over the radius, y from one below the minion up over the height.
     */
    @Test
    fun `the outline spans exactly the blocks the minion mines`() {
        val points = BoundaryBox.edges(radius = 1, height = 3, floorOffset = -1)

        assertEquals(-1.0, points.minOf { it.x })
        assertEquals(2.0, points.maxOf { it.x })
        assertEquals(-1.0, points.minOf { it.z })
        assertEquals(2.0, points.maxOf { it.z })
        assertEquals(-1.0, points.minOf { it.y })
        assertEquals(2.0, points.maxOf { it.y })
    }

    /** A taller box only grows upward - the floor stays one block below the minion. */
    @Test
    fun `height grows the outline upward only`() {
        val points = BoundaryBox.edges(radius = 3, height = 6, floorOffset = -1)

        assertEquals(-1.0, points.minOf { it.y })
        assertEquals(5.0, points.maxOf { it.y })
    }

    /** Only edges, so no point may sit in the middle of a face. */
    @Test
    fun `every point lies on an edge`() {
        val points = BoundaryBox.edges(radius = 1, height = 3, floorOffset = -1)

        assertTrue(
            points.all { point ->
                val extremes = listOf(
                    point.x == -1.0 || point.x == 2.0,
                    point.y == -1.0 || point.y == 2.0,
                    point.z == -1.0 || point.z == 2.0
                )
                extremes.count { it } >= 2
            }
        )
    }

    @Test
    fun `shared corners are not emitted twice`() {
        val points = BoundaryBox.edges(radius = 1, height = 3, floorOffset = -1)

        assertEquals(points.size, points.distinct().size)
    }

    /** A radius upgrade must never turn into thousands of particles per draw. */
    @Test
    fun `an oversized box draws nothing rather than flooding the client`() {
        assertTrue(BoundaryBox.edges(radius = 200, height = 200, floorOffset = -1).isEmpty())
    }

    @Test
    fun `edges start at the floor offset`() {
        val heights = BoundaryBox.edges(1, 2, 0).map { it.y }

        assertEquals(0.0, heights.min())
        assertEquals(2.0, heights.max())
    }
}
