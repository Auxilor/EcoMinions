package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.model.MinionSettings
import java.util.UUID

/**
 * Decides whether a minion is running, and moves it between running and idle.
 *
 * A minion runs only while its owner is online. Anything else - mining, selling,
 * fuel burn, animation - is a consequence of that one rule, so the rule lives in
 * one place rather than being implied by chunk loading.
 *
 * [isOnline] is injected rather than calling Bukkit directly so the rule can be
 * tested without a server.
 */
class MinionActivityService(
    private val repository: MinionStateRepository,
    private val offlineProgress: OfflineProgressService,
    private val settings: SettingsHolder<MinionSettings>,
    private val isOnline: (UUID) -> Boolean
) {
    /** An unowned minion is never active: there is nobody to gate on or pay. */
    fun isActive(minion: Minion): Boolean {
        val owner = minion.state.owner ?: return false
        return isOnline(owner)
    }

    /**
     * Stamps [minion] as idle, if it is not already.
     *
     * The guard matters because logging out and then unloading the chunk would
     * otherwise restamp and lose the earlier window.
     */
    fun markIdle(minion: Minion, now: Long = System.currentTimeMillis()) {
        if (minion.state.shutdownTimestamp != null) {
            return
        }
        minion.freezeFuel(now)
        minion.state = minion.state.copy(shutdownTimestamp = now)
        repository.markShutdown(minion, now)
    }

    /** Credits the idle window, then puts [minion] back to work. */
    fun resume(minion: Minion, now: Long = System.currentTimeMillis()) {
        val idleSince = minion.state.shutdownTimestamp ?: return

        if (settings.value.offlineEnabled) {
            offlineProgress.apply(minion, idleSince, now)
        }

        minion.state = minion.state.copy(shutdownTimestamp = null)
        minion.thawFuel(now)
        repository.clearShutdown(minion)
    }
}
