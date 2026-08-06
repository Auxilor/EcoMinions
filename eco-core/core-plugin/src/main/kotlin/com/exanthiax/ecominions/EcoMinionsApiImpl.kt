package com.exanthiax.ecominions

import com.exanthiax.ecominions.api.EcoMinionsApi
import com.exanthiax.ecominions.api.fuel.FuelsManager
import com.exanthiax.ecominions.api.minion.MinionsManager
import com.exanthiax.ecominions.api.upgrade.UpgradesManager

/**
 * The API implementation registered with Bukkit's services manager.
 *
 * Services implement the manager interfaces directly, so this is pure delegation
 * with no mapping layer.
 */
class EcoMinionsApiImpl(
    private val minions: MinionsManager,
    private val upgrades: UpgradesManager,
    private val fuels: FuelsManager
) : EcoMinionsApi {

    override val apiVersion: String = API_VERSION

    override fun minions(): MinionsManager = minions

    override fun upgrades(): UpgradesManager = upgrades

    override fun fuels(): FuelsManager = fuels

    private companion object {
        const val API_VERSION = "1.0.0"
    }
}
