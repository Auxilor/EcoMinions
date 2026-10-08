package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.lifecycle.Tickable
import com.exanthiax.ecominions.core.scheduling.Regions
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.model.ParticleSettings
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionActivityService
import com.exanthiax.ecominions.minions.service.MinionService
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Draws every live minion from one repeating task: pose, hologram and the
 * storage-full or idle particles.
 *
 * As with mining, the loop runs on the global region and hands each minion to the
 * region that owns its stand.
 */
class MinionRenderLoop(
    private val minions: MinionService,
    private val animator: MinionAnimator,
    private val hologram: MinionHologram,
    private val particles: ParticleEmitter,
    private val settings: SettingsHolder<MinionSettings>,
    private val activity: MinionActivityService,
    private val regions: Regions
) : Tickable {

    override val tickInterval = RENDER_INTERVAL_TICKS

    override fun tick() {
        val now = Bukkit.getCurrentTick().toLong()
        val hologramInterval = settings.value.hologramRefreshTicks

        for (minion in minions.all()) {
            regions.on(minion.entity) { render(minion, now, hologramInterval) }
        }
    }

    private fun render(minion: Minion, now: Long, hologramInterval: Int) {
        if (minion.entity.isDead) {
            return
        }

        // An idle minion keeps its nameplate so it does not look broken, but
        // it must not bob, swing or emit its full-storage particles - it is
        // doing nothing. It gets its own idle particles instead.
        val active = activity.isActive(minion)
        if (active) {
            animator.apply(minion, now)
        }

        if (now % hologramInterval < RENDER_INTERVAL_TICKS) {
            hologram.refresh(minion)
        }

        if (active) {
            emitFullParticles(minion, now)
            emitBoundaryBox(minion, now)
        } else {
            emitIdleParticles(minion, now)
        }
    }

    /** Only drawn while storage is full, and only when someone is there to see it. */
    private fun emitFullParticles(minion: Minion, now: Long) {
        if (minion.animation != Minion.AnimationState.FULL) {
            return
        }
        val layer = minion.state.definition.particles.full ?: return
        emitNearby(minion, now, layer, minion.state.definition.particles.fullPlayerRadius)
    }

    /** Only drawn while the minion is idle, and only when someone is there to see it. */
    private fun emitIdleParticles(minion: Minion, now: Long) {
        val layer = minion.state.definition.particles.idle ?: return
        emitNearby(minion, now, layer, minion.state.definition.particles.idlePlayerRadius)
    }

    /**
     * Outlines the mining box while the minion has nothing to mine, so its owner
     * can see the area that needs clearing. Only the owner sees it: a stalled farm
     * would otherwise blanket every passer-by in outlines.
     */
    private fun emitBoundaryBox(minion: Minion, now: Long) {
        if (minion.animation != Minion.AnimationState.BLOCKED) {
            return
        }
        val layer = minion.state.definition.particles.blocked ?: return
        if (now % layer.intervalTicks.coerceAtLeast(1) >= RENDER_INTERVAL_TICKS) {
            return
        }

        val owner = minion.state.owner?.let { Bukkit.getPlayer(it) } ?: return
        if (!regions.owns(owner)) {
            return
        }
        val corner = minion.entity.location.block.location
        if (owner.world != corner.world) {
            return
        }
        val watchRadius = minion.state.definition.particles.blockedPlayerRadius.toDouble()
        if (owner.location.distanceSquared(corner) > watchRadius * watchRadius) {
            return
        }

        for (offset in BoundaryBox.edges(minion.stats.radius, minion.stats.height)) {
            particles.emitTo(owner, corner.clone().add(offset), layer)
        }
    }

    private fun emitNearby(minion: Minion, now: Long, layer: ParticleSettings.Layer, radius: Int) {
        if (now % layer.intervalTicks.coerceAtLeast(1) >= RENDER_INTERVAL_TICKS) {
            return
        }

        val watchRadius = radius.toDouble()
        val watched = minion.entity.world
            .getNearbyEntities(minion.entity.location, watchRadius, watchRadius, watchRadius)
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
