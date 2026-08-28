package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.placeholders.InternalPlaceholder
import com.exanthiax.ecominions.core.placeholders.PlaceholderSource
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionService
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.placeholder.Placeholder
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import org.bukkit.Bukkit

/** The minions slice's contribution to both placeholder scopes. */
class MinionPlaceholders(
    private val minions: MinionService
) : PlaceholderSource {

    override fun externalPlaceholders(plugin: EcoPlugin): List<Placeholder> {
        val placeholders = mutableListOf<Placeholder>(
            PlayerPlaceholder(plugin, "count") { player ->
                minions.countOwnedBy(player).toString()
            },
            PlayerPlaceholder(plugin, "total_levels") { player ->
                minions.ownedBy(player).sumOf { it.level }.toString()
            },
            PlayerPlaceholder(plugin, "limit") { player ->
                minions.placementLimit(player).toString()
            }
        )

        for (type in minions.types()) {
            placeholders += PlayerPlaceholder(plugin, "count_${type.id}") { player ->
                minions.ownedBy(player).count { it.state.type.id == type.id }.toString()
            }
        }

        return placeholders
    }

    override fun internalPlaceholders(): List<InternalPlaceholder> = listOf(
        InternalPlaceholder("minion_name") { it.minion()?.state?.type?.name },
        InternalPlaceholder("type") { it.minion()?.state?.type?.id },
        InternalPlaceholder("level") { it.minion()?.level?.toString() },
        InternalPlaceholder("owner") { it.minion()?.let(::ownerName) },
        InternalPlaceholder("used") { it.minion()?.usedStorage?.toString() },
        InternalPlaceholder("total") { it.minion()?.storageSize?.toString() },
        InternalPlaceholder("radius") { it.minion()?.stats?.radius?.toString() },
        InternalPlaceholder("diameter") { it.minion()?.stats?.radius?.let { radius -> (radius * 2 + 1).toString() } },
        InternalPlaceholder("height") { it.minion()?.stats?.height?.toString() },
        InternalPlaceholder("speed") { it.minion()?.stats?.speedTicks?.toString() },
        InternalPlaceholder("efficiency") { it.minion()?.stats?.efficiencyTicks?.toString() },
        InternalPlaceholder("capacity") { it.minion()?.stats?.storageSize?.toString() }
    )

    private fun ownerName(minion: Minion): String {
        val owner = minion.state.owner ?: return UNOWNED
        return Bukkit.getOfflinePlayer(owner).name ?: UNKNOWN
    }

    private fun TemplateContext.minion(): Minion? = get(MINION)

    companion object {
        /** Context key every minion-aware placeholder resolves against. */
        const val MINION = "minion"

        private const val UNOWNED = "Unowned"
        private const val UNKNOWN = "Unknown"
    }
}
