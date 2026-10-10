package com.exanthiax.ecominions.minions.integration.items

import com.exanthiax.ecominions.minions.service.activity.OfflineYield
import com.willfp.ecoitems.api.EcoItemsLoot
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * The only place EcoItems is called from. Every method is safe to call when
 * EcoItems is absent.
 */
class EcoItemsIntegrationService {
    @Volatile
    private var pluginAvailable = false

    fun init() {
        pluginAvailable = Bukkit.getPluginManager().isPluginEnabled("EcoItems")
    }

    /** @return EcoItems' fishing catch at [location] for [player], or null to keep the vanilla one. */
    fun rollFishing(location: Location, player: Player): OfflineYield? {
        if (!pluginAvailable) {
            return null
        }
        return EcoItemsLoot.rollFishing(location, player)?.let { OfflineYield(listOf(it.item), it.experience) }
    }
}
