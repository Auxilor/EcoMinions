package com.exanthiax.ecominions.minions.model

import com.willfp.eco.core.particle.Particles
import com.willfp.eco.core.particle.SpawnableParticle

/** The particle layers a minion emits. */
data class ParticleSettings(
    /** Emitted while storage is full and a player is nearby. */
    val full: Layer?,
    val fullPlayerRadius: Int,
    /** Emitted while the minion is idle (owner offline) and a player is nearby. */
    val idle: Layer?,
    val idlePlayerRadius: Int,
    /**
     * Traced around the mining box while the minion has nothing to mine, so the
     * owner can see the area that needs clearing. Shown to the owner alone -
     * a stalled farm would otherwise blanket every passer-by in outlines.
     */
    val blocked: Layer?,
    val blockedPlayerRadius: Int
) {
    /**
     * [particle] comes from eco's lookup, so a type can use any of eco's particle
     * strings - `flame`, `dust:ff5050`, `dust_transition:ff0000:0000ff`.
     *
     * eco spawns a whole batch on a single point, so [count] and [offset] are
     * applied by [com.exanthiax.ecominions.minions.ui.ParticleEmitter] rather than
     * by the particle itself.
     */
    data class Layer(
        val particle: SpawnableParticle,
        val intervalTicks: Int,
        val count: Int,
        val offset: Double
    )

    companion object {
        const val DEFAULT_PLAYER_RADIUS = 16

        /**
         * Resolved lazily: eco registers its particle factories during startup, and
         * a lookup before that would silently resolve to an empty particle.
         */
        val DEFAULT: ParticleSettings by lazy {
            ParticleSettings(
                full = Layer(
                    particle = Particles.lookup("angry_villager"),
                    intervalTicks = 20,
                    count = 4,
                    offset = 0.4
                ),
                fullPlayerRadius = DEFAULT_PLAYER_RADIUS,
                idle = null,
                idlePlayerRadius = DEFAULT_PLAYER_RADIUS,
                blocked = Layer(
                    particle = Particles.lookup("dust:ff5050"),
                    intervalTicks = 20,
                    count = 1,
                    offset = 0.0
                ),
                blockedPlayerRadius = DEFAULT_PLAYER_RADIUS
            )
        }
    }
}
