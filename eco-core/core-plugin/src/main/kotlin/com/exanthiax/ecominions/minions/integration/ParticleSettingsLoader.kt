package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.minions.model.ParticleSettings
import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.Color
import org.bukkit.Particle

/** Parses the `particles` section of a minion type. */
class ParticleSettingsLoader {

    fun load(section: Config?, context: LoadContext): ParticleSettings {
        if (section == null) {
            return ParticleSettings.DEFAULT
        }

        val full = section.getSubsection("full")
        if (full == null || !full.has("enabled")) {
            return ParticleSettings.DEFAULT
        }

        return ParticleSettings(
            full = readLayer(full, context.with("particles.full")),
            fullPlayerRadius = section.getIntOrNull("full.nearby-player-radius") ?: DEFAULT_RADIUS
        )
    }

    private fun readLayer(section: Config, context: LoadContext): ParticleSettings.Layer? {
        if (!section.getBool("enabled")) {
            return null
        }

        val name = section.getString("type").uppercase()
        val particle = runCatching { Particle.valueOf(name) }.getOrNull()
            ?: return context.invalid("type", name, "a particle on this server version")

        return ParticleSettings.Layer(
            particle = particle,
            intervalTicks = section.getIntOrNull("interval-ticks") ?: DEFAULT_INTERVAL,
            count = section.getIntOrNull("count") ?: 1,
            offset = section.getDoubleOrNull("offset") ?: DEFAULT_OFFSET,
            speed = section.getDoubleOrNull("speed") ?: 0.0,
            dustColor = readColor(section)
        )
    }

    private fun readColor(section: Config): Color? {
        val red = section.getIntOrNull("dust.r") ?: return null
        val green = section.getIntOrNull("dust.g") ?: return null
        val blue = section.getIntOrNull("dust.b") ?: return null
        return Color.fromRGB(red, green, blue)
    }

    private companion object {
        const val DEFAULT_RADIUS = 16
        const val DEFAULT_INTERVAL = 10
        const val DEFAULT_OFFSET = 0.2
    }
}
