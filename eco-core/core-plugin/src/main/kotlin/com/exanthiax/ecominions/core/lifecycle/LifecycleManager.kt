package com.exanthiax.ecominions.core.lifecycle

import com.willfp.eco.core.EcoPlugin
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask

/**
 * Owns every repeating task in the plugin. Reload semantics live here and
 * nowhere else: components declare a [Tickable], they never schedule.
 */
class LifecycleManager(
    private val plugin: EcoPlugin,
    private val tickables: List<Tickable>
) {
    private val tasks = mutableListOf<BukkitTask>()

    fun start() {
        for (tickable in tickables) {
            val interval = tickable.tickInterval.coerceAtLeast(1L)
            tasks += Bukkit.getScheduler().runTaskTimer(plugin, Runnable { tickable.tick() }, interval, interval)
        }
    }

    fun stop() {
        tasks.forEach { it.cancel() }
        tasks.clear()
    }

    fun restart() {
        stop()
        start()
    }
}
