package com.exanthiax.ecominions.core.scheduling

import com.willfp.eco.core.Eco
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.FoliaSupport
import com.willfp.eco.core.Prerequisite
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity

/**
 * Runs work on the thread that owns what it touches.
 *
 * Folia ticks each region of the world on its own thread, and only that thread may
 * touch the region's blocks and entities. Every call here runs inline when the
 * current thread already owns the target, which is always the case on Paper and
 * Spigot, so nothing is rescheduled or delayed off Folia.
 */
class Regions(
    @PublishedApi internal val plugin: EcoPlugin
) {
    /** Whether the current thread may touch [entity]. Always true off Folia. */
    fun owns(entity: Entity): Boolean = Eco.get().isOwnedByCurrentRegion(entity)

    /** Whether the current thread may touch the world at [location]. Always true off Folia. */
    fun owns(location: Location): Boolean = Eco.get().isOwnedByCurrentRegion(location)

    /** Runs [action] on the region owning [entity]: now if this thread owns it, else on its next tick. */
    inline fun on(entity: Entity, crossinline action: () -> Unit) {
        if (owns(entity)) {
            action()
        } else {
            plugin.scheduler.on(entity).run(Runnable { action() })
        }
    }

    /**
     * As [on], running [onRetired] instead if [entity] is removed or unloaded
     * before the hop lands.
     */
    inline fun on(entity: Entity, crossinline onRetired: () -> Unit, crossinline action: () -> Unit) {
        if (owns(entity)) {
            action()
        } else {
            plugin.scheduler.on(entity).onRetired(Runnable { onRetired() }).run(Runnable { action() })
        }
    }

    /** Runs [action] on the region owning a chunk: now if this thread owns it, else on its next tick. */
    inline fun at(world: World, chunkX: Int, chunkZ: Int, crossinline action: () -> Unit) {
        val corner = Location(world, (chunkX shl 4).toDouble(), 0.0, (chunkZ shl 4).toDouble())
        if (owns(corner)) {
            action()
        } else {
            plugin.scheduler.at(world, chunkX, chunkZ).run(Runnable { action() })
        }
    }

    /** Runs [action] on the global region: now if this is the global region thread, else on its next tick. */
    inline fun global(crossinline action: () -> Unit) {
        if (!Prerequisite.HAS_FOLIA.isMet || Bukkit.isGlobalTickThread()) {
            action()
        } else {
            plugin.scheduler.global().run(Runnable { action() })
        }
    }

    /**
     * Whether [entity] can be touched from this thread, logging [what] once if
     * it cannot. For actions that can be refused, such as a menu click on a
     * minion the menu was opened on from afar.
     */
    fun ownsOrWarn(entity: Entity, what: String): Boolean {
        if (owns(entity)) {
            return true
        }
        FoliaSupport.isUnsupported("$what from outside its region")
        return false
    }

    /**
     * Logs once if a call that has to answer synchronously is made from a thread
     * that does not own [entity]. The call still goes ahead: a caller waiting on
     * an answer cannot be deferred without blocking a region thread.
     */
    fun warnIfNotOwned(entity: Entity, what: String) {
        ownsOrWarn(entity, what)
    }

    /** As [warnIfNotOwned], for the world at [location]. */
    fun warnIfNotOwned(location: Location, what: String) {
        if (!owns(location)) {
            FoliaSupport.isUnsupported("$what from outside its region")
        }
    }
}
