package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.api.minion.LimitBreach
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
    private val islandMembers: (Minion) -> Collection<UUID>,
    private val limitBreach: (Minion) -> LimitBreach? = { null }
) {
    /** An unowned minion is never active: there is nobody to gate on or pay. */
    fun isActive(minion: Minion): Boolean = isActiveWhere(minion) { isOnline(it) }

    /**
     * As [isActive], treating [leaving] as offline. Bukkit still lists a player
     * while their quit event is being handled.
     */
    fun isActiveWithout(minion: Minion, leaving: UUID): Boolean =
        isActiveWhere(minion) { it != leaving && isOnline(it) }

    /** Whether a limit is holding [minion] back. */
    fun isLimited(minion: Minion): Boolean = minion.state.limitedSince != null

    /** Whether [minion]'s limit status changed since it was last reconciled. */
    fun needsReconcile(minion: Minion): Boolean = (limitBreach(minion) != null) != isLimited(minion)

    private fun isActiveWhere(minion: Minion, online: (UUID) -> Boolean): Boolean {
        val owner = minion.state.owner ?: return false
        if (limitBreach(minion) != null) {
            return false
        }
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
     * An unowned minion is left as it is: stamping it would pay a later owner for time it had none.
     *
     * @return Whether [minion] is active.
     */
    fun reconcile(minion: Minion, now: Long = System.currentTimeMillis()): Boolean {
        if (minion.state.owner == null) {
            return false
        }
        if (limitBreach(minion) != null) {
            markLimited(minion, now)
            return false
        }
        if (isLimited(minion)) {
            minion.state = minion.state.copy(limitedSince = null, shutdownTimestamp = now)
            repository.clearLimited(minion)
            repository.markShutdown(minion, now)
        }
        minion.seenAllowed = true
        return isActive(minion).also { active ->
            if (active) {
                resume(minion, now)
            } else {
                markIdle(minion, now)
            }
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

    /**
     * Stamps [minion] as held back by a limit, if it is not already. An idle window that ran
     * while it was within its limits is paid up to [now]; nothing after it ever is.
     */
    private fun markLimited(minion: Minion, now: Long) {
        if (isLimited(minion)) {
            return
        }
        val idleSince = minion.state.shutdownTimestamp
        if (idleSince == null) {
            markIdle(minion, now)
        } else {
            if (minion.seenAllowed && settings.value.offlineEnabled) {
                offlineProgress.apply(minion, idleSince, now)
            }
            minion.state = minion.state.copy(shutdownTimestamp = now)
            repository.markShutdown(minion, now)
        }
        minion.state = minion.state.copy(limitedSince = now)
        repository.markLimited(minion, now)
    }

    /** Credits the idle window, then puts [minion] back to work. A minion held back by a limit stays idle. */
    fun resume(minion: Minion, now: Long = System.currentTimeMillis()) {
        if (isLimited(minion)) {
            return
        }
        val idleSince = minion.state.shutdownTimestamp ?: return

        if (settings.value.offlineEnabled) {
            offlineProgress.apply(minion, idleSince, now)
        }

        minion.state = minion.state.copy(shutdownTimestamp = null)
        minion.thawFuel(now)
        repository.clearShutdown(minion)
    }
}
