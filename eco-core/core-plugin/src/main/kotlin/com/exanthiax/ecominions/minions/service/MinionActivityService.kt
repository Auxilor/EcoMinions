package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.model.MinionSettings
import java.util.UUID

/**
 * Decides whether a minion is running, and moves it between running and idle.
 *
 * A minion runs while its owner is online or, when `islands.active-while-member-online`
 * is on, while any member of the island it stands on is. Anything else - mining, selling,
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
    private val isOnline: (UUID) -> Boolean,
    private val islandMembers: (Minion) -> Collection<UUID>
) {
    /** An unowned minion is never active: there is nobody to gate on or pay. */
    fun isActive(minion: Minion): Boolean = isActiveWhere(minion) { isOnline(it) }

    /**
     * As [isActive], treating [leaving] as offline. Bukkit still lists a player
     * while their quit event is being handled.
     */
    fun isActiveWithout(minion: Minion, leaving: UUID): Boolean =
        isActiveWhere(minion) { it != leaving && isOnline(it) }

    private fun isActiveWhere(minion: Minion, online: (UUID) -> Boolean): Boolean {
        val owner = minion.state.owner ?: return false
        if (online(owner)) {
            return true
        }
        return settings.value.activeWhileMemberOnline && islandMembers(minion).any(online)
    }

    /**
     * Brings [minion]'s idle stamp in step with [isActive]: an active minion is resumed and an
     * inactive one is stamped idle, each only if it is not already.
     *
     * Island membership can change with no join or quit (an invite, kick or leave), and settings
     * can change on reload, so this runs on the minion's region every tick rather than on events.
     *
     * @return Whether [minion] is active.
     */
    fun reconcile(minion: Minion, now: Long = System.currentTimeMillis()): Boolean =
        isActive(minion).also { active ->
            if (active) {
                resume(minion, now)
            } else {
                markIdle(minion, now)
            }
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
