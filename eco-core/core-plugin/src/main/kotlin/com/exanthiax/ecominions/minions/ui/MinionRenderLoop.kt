package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.lifecycle.Tickable
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionActivityService
import com.exanthiax.ecominions.minions.service.MinionService
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Draws every live minion from one repeating task: pose, hologram and the
 * storage-full particles.
 */
class MinionRenderLoop(
    private val minions: MinionService,
    private val animator: MinionAnimator,
    private val hologram: MinionHologram,
    private val particles: ParticleEmitter,
    private val settings: SettingsHolder<MinionSettings>,
    private val activity: MinionActivityService
) : Tickable {

    override val tickInterval = RENDER_INTERVAL_TICKS

    override fun tick() {
        val now = Bukkit.getCurrentTick().toLong()
        val hologramInterval = settings.value.hologramRefreshTicks

        for (minion in minions.all()) {
            if (minion.entity.isDead) {
                continue
            }

            // An idle minion keeps its nameplate so it does not look broken, but
            // it must not bob, swing or emit particles - it is doing nothing.
            val active = activity.isActive(minion)
            if (active) {
                animator.apply(minion, now)
            }

            if (now % hologramInterval < RENDER_INTERVAL_TICKS) {
                hologram.refresh(minion)
            }

            if (active) {
                emitFullParticles(minion, now)
            }
        }
    }

    /** Only drawn while storage is full, and only when someone is there to see it. */
    private fun emitFullParticles(minion: Minion, now: Long) {
        if (minion.animation != Minion.AnimationState.FULL) {
            return
        }
        val layer = minion.state.type.particles.full ?: return
        if (now % layer.intervalTicks.coerceAtLeast(1) >= RENDER_INTERVAL_TICKS) {
            return
        }

        val radius = minion.state.type.particles.fullPlayerRadius.toDouble()
        val watched = minion.entity.world
            .getNearbyEntities(minion.entity.location, radius, radius, radius)
            .any { it is Player }
        if (!watched) {
            return
        }

        particles.emit(minion.entity.location.clone().add(0.0, PARTICLE_HEIGHT, 0.0), layer)
    }

    private companion object {
        const val RENDER_INTERVAL_TICKS = 5L
        const val PARTICLE_HEIGHT = 1.6
    }
}
