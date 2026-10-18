package com.exanthiax.ecominions

import com.exanthiax.ecominions.api.EcoMinionsApi
import com.exanthiax.ecominions.api.fuel.FuelsManager
import com.exanthiax.ecominions.api.island.IslandProvider
import com.exanthiax.ecominions.api.island.IslandsManager
import com.exanthiax.ecominions.api.island.MinionIsland
import com.exanthiax.ecominions.api.menu.MenusManager
import com.exanthiax.ecominions.api.minion.MinionsManager
import com.exanthiax.ecominions.api.upgrade.UpgradesManager
import org.bukkit.Location

/**
 * The API implementation registered with Bukkit's services manager.
 *
 * Pure delegation: each manager is an adapter in `api/impl` that maps the public
 * contract onto the internal services.
 */
class EcoMinionsApiImpl(
    private val minions: MinionsManager,
    private val upgrades: UpgradesManager,
    private val fuels: FuelsManager,
    private val menus: MenusManager
) : EcoMinionsApi {

    override fun minions(): MinionsManager = minions

    override fun upgrades(): UpgradesManager = upgrades

    override fun fuels(): FuelsManager = fuels

    override fun menus(): MenusManager = menus

    override fun islands(): IslandsManager = placeholderIslands

    private val placeholderIslands = object : IslandsManager {
        override fun register(provider: IslandProvider) = Unit

        override fun unregister(id: String) = Unit

        override fun providers(): List<IslandProvider> = emptyList()

        override fun islandAt(location: Location): MinionIsland? = null
    }
}
