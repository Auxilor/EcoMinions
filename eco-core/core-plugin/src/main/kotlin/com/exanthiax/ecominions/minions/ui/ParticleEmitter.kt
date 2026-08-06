package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.minions.model.ParticleSettings
import org.bukkit.Location
import org.bukkit.Particle

/** Spawns a configured particle layer at a location. */
class ParticleEmitter {

    fun emit(location: Location, layer: ParticleSettings.Layer) {
        val world = location.world ?: return
        val dust = layer.dustColor

        if (layer.particle == Particle.DUST && dust != null) {
            world.spawnParticle(
                Particle.DUST,
                location,
                layer.count,
                layer.offset,
                layer.offset,
                layer.offset,
                layer.speed,
                Particle.DustOptions(dust, DUST_SIZE)
            )
            return
        }

        world.spawnParticle(
            layer.particle,
            location,
            layer.count,
            layer.offset,
            layer.offset,
            layer.offset,
            layer.speed
        )
    }

    private companion object {
        const val DUST_SIZE = 1.0f
    }
}
