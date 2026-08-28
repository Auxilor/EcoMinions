package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.minions.model.ParticleSettings
import org.bukkit.Location
import org.bukkit.entity.Player
import kotlin.random.Random

/**
 * Spawns a configured particle layer at a location.
 *
 * eco's particle API spawns a whole batch on one exact point, so the scatter that
 * Bukkit's own offset arguments used to provide is applied here: one particle per
 * point, jittered within the layer's offset.
 */
class ParticleEmitter {

    /** Spawns [layer] for everyone who can see it. */
    fun emit(location: Location, layer: ParticleSettings.Layer) {
        for (point in scatter(location, layer)) {
            layer.particle.spawn(point, 1)
        }
    }

    /** Spawns [layer] so that only [player] sees it. */
    fun emitTo(player: Player, location: Location, layer: ParticleSettings.Layer) {
        for (point in scatter(location, layer)) {
            layer.particle.spawnTo(player, point, 1)
        }
    }

    /**
     * @return One point per particle. A zero offset returns the location untouched,
     * so a wireframe lands exactly on its point rather than near it.
     */
    private fun scatter(location: Location, layer: ParticleSettings.Layer): List<Location> {
        val count = layer.count.coerceAtLeast(1)
        if (layer.offset <= 0.0) {
            return List(count) { location }
        }

        return List(count) {
            location.clone().add(jitter(layer.offset), jitter(layer.offset), jitter(layer.offset))
        }
    }

    private fun jitter(offset: Double): Double = Random.nextDouble(-offset, offset)
}
