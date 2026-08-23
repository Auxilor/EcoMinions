package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.minions.model.ParticleSettings
import com.willfp.eco.core.particle.SpawnableParticle
import io.mockk.mockk
import org.bukkit.Location
import org.bukkit.entity.Player
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class ParticleEmitterTest {

    private val emitter = ParticleEmitter()
    private val origin = Location(null, 10.0, 64.0, -5.0)

    /** eco spawns a whole batch on one point, so the spread has to be built here. */
    @Test
    fun `every particle in the count is spawned individually`() {
        val particle = RecordingParticle()

        emitter.emit(origin, layer(particle, count = 4, offset = 0.4))

        assertEquals(4, particle.spawned.size)
        assertTrue(particle.spawned.all { (_, amount) -> amount == 1 })
    }

    @Test
    fun `particles are scattered within the layer's offset`() {
        val particle = RecordingParticle()

        emitter.emit(origin, layer(particle, count = 50, offset = 0.4))

        assertTrue(
            particle.spawned.all { (location, _) ->
                abs(location.x - origin.x) <= 0.4 &&
                    abs(location.y - origin.y) <= 0.4 &&
                    abs(location.z - origin.z) <= 0.4
            }
        )
    }

    /** A wireframe outline has to land exactly on its point, not near it. */
    @Test
    fun `a zero offset spawns on the point itself`() {
        val particle = RecordingParticle()

        emitter.emit(origin, layer(particle, count = 2, offset = 0.0))

        assertTrue(particle.spawned.all { (location, _) -> location == origin })
    }

    @Test
    fun `emitting does not move the caller's location`() {
        val particle = RecordingParticle()

        emitter.emit(origin, layer(particle, count = 5, offset = 0.4))

        assertEquals(Location(null, 10.0, 64.0, -5.0), origin)
    }

    @Test
    fun `targeting a player spawns for that player alone`() {
        val particle = RecordingParticle()
        val player = mockk<Player>()

        emitter.emitTo(player, origin, layer(particle, count = 3, offset = 0.0))

        assertEquals(3, particle.targeted.size)
        assertTrue(particle.targeted.all { it === player })
        assertTrue(particle.spawned.isEmpty())
    }

    private fun layer(particle: SpawnableParticle, count: Int, offset: Double) =
        ParticleSettings.Layer(
            particle = particle,
            intervalTicks = 20,
            count = count,
            offset = offset
        )

    private class RecordingParticle : SpawnableParticle {
        val spawned = mutableListOf<Pair<Location, Int>>()
        val targeted = mutableListOf<Player>()

        override fun spawn(location: Location, amount: Int) {
            spawned += location.clone() to amount
        }

        override fun spawnTo(player: Player, location: Location, amount: Int) {
            targeted += player
        }
    }
}
