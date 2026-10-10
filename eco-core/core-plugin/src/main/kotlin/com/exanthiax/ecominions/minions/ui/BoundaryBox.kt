package com.exanthiax.ecominions.minions.ui

import org.bukkit.util.Vector

/**
 * The wireframe outline of a minion's mining box, as offsets from the corner of
 * the block the minion stands on.
 *
 * Drawn for a blocked minion so its owner can see which area needs clearing. The
 * bounds mirror the scan in `MiningTargetFinder`: the radius horizontally, and
 * the height stat vertically starting at the type's floor.
 */
object BoundaryBox {

    /** @return One point per [SPACING] blocks along the box's twelve edges, corners included once. */
    fun edges(radius: Int, height: Int, floorOffset: Int): List<Vector> {
        val xs = samples(-radius.toDouble(), (radius + 1).toDouble())
        val ys = samples(floorOffset.toDouble(), (floorOffset + height).toDouble())
        val zs = samples(-radius.toDouble(), (radius + 1).toDouble())

        if (xs.size * ys.size * zs.size > MAX_VOLUME_SAMPLES) {
            return emptyList()
        }

        val points = LinkedHashSet<Vector>()
        for (x in xs) {
            for (y in ys) {
                for (z in zs) {
                    if (onEdge(x, xs, y, ys, z, zs)) {
                        points += Vector(x, y, z)
                    }
                }
            }
        }
        return points.toList()
    }

    /** A point is on an edge when it is at the extreme of at least two axes. */
    private fun onEdge(
        x: Double,
        xs: List<Double>,
        y: Double,
        ys: List<Double>,
        z: Double,
        zs: List<Double>
    ): Boolean {
        var extremes = 0
        if (isExtreme(x, xs)) extremes++
        if (isExtreme(y, ys)) extremes++
        if (isExtreme(z, zs)) extremes++
        return extremes >= 2
    }

    private fun isExtreme(value: Double, axis: List<Double>): Boolean =
        value == axis.first() || value == axis.last()

    /** Inclusive of both ends, so the far face always lands on the box edge. */
    private fun samples(from: Double, to: Double): List<Double> {
        val result = mutableListOf(from)
        var next = from + SPACING
        while (next < to) {
            result += next
            next += SPACING
        }
        result += to
        return result
    }

    private const val SPACING = 1.0

    /**
     * Guards against a heavily upgraded radius turning one draw into thousands of
     * particles. Comfortably above the largest box any shipped type produces.
     */
    private const val MAX_VOLUME_SAMPLES = 8000
}
