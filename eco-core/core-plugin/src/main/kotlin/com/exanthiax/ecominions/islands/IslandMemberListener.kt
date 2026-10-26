package com.exanthiax.ecominions.islands

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.scheduling.Regions
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionActivityService
import com.exanthiax.ecominions.minions.service.MinionService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

/**
 * Starts and stops other players' minions as island members come and go, when
 * `islands.active-while-member-online` is on.
 *
 * Only minions the player does not own are handled here; their own stay with
 * `MinionOwnerListener`. Minions are found
 * through the [IslandMinionIndex], so no island provider is asked anything on join or quit.
 */
class IslandMemberListener(
    private val minions: MinionService,
    private val activity: MinionActivityService,
    private val islands: IslandRegistry,
    private val index: IslandMinionIndex,
    private val settings: SettingsHolder<MinionSettings>,
    private val regions: Regions
) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!settings.value.activeWhileMemberOnline) {
            return
        }
        val now = System.currentTimeMillis()
        for (minion in sharedMinions(event.player.uniqueId)) {
            regions.on(minion.entity) {
                if (activity.isActive(minion)) {
                    activity.resume(minion, now)
                }
            }
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        if (!settings.value.activeWhileMemberOnline) {
            return
        }
        val now = System.currentTimeMillis()
        val quitter = event.player.uniqueId
        for (minion in sharedMinions(quitter)) {
            regions.on(minion.entity) {
                if (!activity.isActiveWithout(minion, quitter)) {
                    activity.markIdle(minion, now)
                }
            }
        }
    }

    private fun sharedMinions(player: UUID): List<Minion> =
        islands.islandsOf(player)
            .flatMap { index.minionsOn(it) }
            .distinct()
            .mapNotNull { minions.getByUuid(it) }
            .filter { it.state.owner != player }
}
