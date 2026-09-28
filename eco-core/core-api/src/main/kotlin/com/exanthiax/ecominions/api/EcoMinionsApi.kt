package com.exanthiax.ecominions.api

import com.exanthiax.ecominions.api.fuel.FuelsManager
import com.exanthiax.ecominions.api.menu.MenusManager
import com.exanthiax.ecominions.api.minion.MinionsManager
import com.exanthiax.ecominions.api.upgrade.UpgradesManager
import org.bukkit.Bukkit

/**
 * Entry point of the EcoMinions API.
 *
 * Obtain an instance with [EcoMinionsApi.instance] or [EcoMinionsApi.get]. The
 * implementation is registered with Bukkit's services manager when the plugin
 * enables, so calls made before that point will throw.
 */
interface EcoMinionsApi {
    fun minions(): MinionsManager

    fun upgrades(): UpgradesManager

    fun fuels(): FuelsManager

    fun menus(): MenusManager

    companion object {
        /**
         * The registered API implementation.
         *
         * @throws IllegalStateException If EcoMinions is not enabled.
         */
        @JvmStatic
        val instance: EcoMinionsApi
            get() = get()

        /**
         * @return The registered API implementation.
         * @throws IllegalStateException If EcoMinions is not enabled.
         */
        @JvmStatic
        fun get(): EcoMinionsApi = getOrNull()
            ?: throw IllegalStateException("EcoMinions is not enabled")

        /** @return The registered API implementation, or null if EcoMinions is not enabled. */
        @JvmStatic
        fun getOrNull(): EcoMinionsApi? = Bukkit.getServicesManager()
            .getRegistration(EcoMinionsApi::class.java)
            ?.provider
    }
}
