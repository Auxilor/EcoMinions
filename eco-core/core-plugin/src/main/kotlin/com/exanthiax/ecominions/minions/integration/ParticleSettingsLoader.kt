package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.minions.model.ParticleSettings
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.particle.Particles
import com.willfp.eco.core.particle.impl.EmptyParticle

/** Parses the `particles` section of a minion type. */
class ParticleSettingsLoader {

    fun load(section: Config?, context: LoadContext): ParticleSettings {
        if (section == null) {
            return ParticleSettings.DEFAULT
        }

        return ParticleSettings(
            full = layer(section, "full", context) ?: ParticleSettings.DEFAULT.full,
            fullPlayerRadius = section.getIntOrNull("full.nearby-player-radius") ?: DEFAULT_RADIUS,
            idle = layer(section, "idle", context),
            idlePlayerRadius = section.getIntOrNull("idle.nearby-player-radius") ?: DEFAULT_RADIUS,
            blocked = layer(section, "blocked", context) ?: ParticleSettings.DEFAULT.blocked,
            blockedPlayerRadius = section.getIntOrNull("blocked.nearby-player-radius") ?: DEFAULT_RADIUS
        )
    }

    /**
     * @return The configured layer, or null when the type does not describe one -
     * either because the section is absent, or because it is explicitly disabled.
     */
    private fun layer(section: Config, name: String, context: LoadContext): ParticleSettings.Layer? {
        val layer = section.getSubsection(name)?.takeIf { it.has("enabled") } ?: return null
        if (!layer.getBool("enabled")) {
            return null
        }
        return readLayer(layer, context.with("particles.$name"))
    }

    /**
     * `particle` is an eco particle string, so a type can use any particle eco
     * knows: `flame`, `dust:ff5050`, `dust_transition:ff0000:0000ff`.
     */
    private fun readLayer(section: Config, context: LoadContext): ParticleSettings.Layer? {
        val name = section.getString("particle")
        val particle = Particles.lookup(name)
        if (particle is EmptyParticle) {
            return context.invalid("particle", name, "a particle eco recognises")
        }

        return ParticleSettings.Layer(
            particle = particle,
            intervalTicks = section.getIntOrNull("interval-ticks") ?: DEFAULT_INTERVAL,
            count = section.getIntOrNull("count") ?: 1,
            offset = section.getDoubleOrNull("offset") ?: DEFAULT_OFFSET
        )
    }

    private companion object {
        const val DEFAULT_RADIUS = 16
        const val DEFAULT_INTERVAL = 10
        const val DEFAULT_OFFSET = 0.2
    }
}
