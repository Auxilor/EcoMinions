package com.exanthiax.ecominions.minions.model

import org.bukkit.Color
import org.bukkit.Particle

/** The particle layers a minion type emits. */
data class ParticleSettings(
    /** Emitted while storage is full and a player is nearby. */
    val full: Layer?,
    val fullPlayerRadius: Int
) {
    data class Layer(
        val particle: Particle,
        val intervalTicks: Int,
        val count: Int,
        val offset: Double,
        val speed: Double,
        val dustColor: Color?
    )

    companion object {
        val DEFAULT = ParticleSettings(
            full = Layer(
                particle = Particle.ANGRY_VILLAGER,
                intervalTicks = 20,
                count = 4,
                offset = 0.4,
                speed = 0.0,
                dustColor = null
            ),
            fullPlayerRadius = 16
        )
    }
}
