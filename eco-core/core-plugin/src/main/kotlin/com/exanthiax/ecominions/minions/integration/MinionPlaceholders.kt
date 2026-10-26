package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.api.minion.LimitScope
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.persistence.InventoryCodec
import com.exanthiax.ecominions.core.placeholders.InternalPlaceholder
import com.exanthiax.ecominions.core.placeholders.PlaceholderSource
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionLimits
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.PlacementLimits
import com.exanthiax.ecominions.minions.ui.limitName
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.placeholder.Placeholder
import com.willfp.eco.core.placeholder.PlayerPlaceholder
import org.bukkit.Bukkit
import java.util.UUID

/** The minions slice's contribution to both placeholder scopes. */
class MinionPlaceholders(
    private val minions: MinionService,
    private val fuels: FuelService,
    private val limits: MinionLimits,
    private val messages: Messages
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
                minions.placementLimit(player).takeIf { it != PlacementLimits.UNLIMITED }?.toString()
                    ?: plugin.langYml.getFormattedString("infinity")
            }
        )

        val infinity = { plugin.langYml.getFormattedString("infinity") }
        placeholders += PlayerPlaceholder(plugin, "inactive") { player -> limits.inactiveCount(player.uniqueId).toString() }

        for (type in minions.types()) {
            placeholders += PlayerPlaceholder(plugin, "count_${type.id}") { player ->
                minions.countOwnedBy(player, type.id).toString()
            }
            placeholders += PlayerPlaceholder(plugin, "limit_${type.id}") { player ->
                capText(limits.capsFor(player.uniqueId).minionCap(type.id), infinity)
            }
        }

        for (type in MinionType.entries) {
            placeholders += PlayerPlaceholder(plugin, "count_type_${type.id}") { player ->
                limits.countOfType(player.uniqueId, type).toString()
            }
            placeholders += PlayerPlaceholder(plugin, "limit_type_${type.id}") { player ->
                capText(limits.capsFor(player.uniqueId).typeCap(type), infinity)
            }
        }

        return placeholders
    }

    override fun internalPlaceholders(): List<InternalPlaceholder> = listOf(
        InternalPlaceholder("minion_name") { it.state()?.definition?.name },
        InternalPlaceholder("type") { it.state()?.definition?.id },
        InternalPlaceholder("kind") { context ->
            context.state()?.definition?.type?.id?.replaceFirstChar { it.uppercase() }
        },
        InternalPlaceholder("level") { it.state()?.level?.toString() },
        InternalPlaceholder("stored_experience") { it.state()?.storedExperience?.toString() },
        InternalPlaceholder("owner") { it.state()?.let(::ownerName) },
        InternalPlaceholder("used") { context ->
            context.minion()?.usedStorage?.toString() ?: context.state()?.let(::storedSlots)?.toString()
        },
        InternalPlaceholder("total") { context ->
            context.minion()?.storageSize?.toString() ?: context.stats()?.storageSize?.toString()
        },
        InternalPlaceholder("radius") { it.stats()?.radius?.toString() },
        InternalPlaceholder("diameter") { it.stats()?.radius?.let { radius -> (radius * 2 + 1).toString() } },
        InternalPlaceholder("height") { it.stats()?.height?.toString() },
        InternalPlaceholder("speed") { context ->
            context.minion()?.let { fuels.effectiveSpeedTicks(it, it.stats.speedTicks).toString() }
                ?: context.stats()?.speedTicks?.toString()
        },
        InternalPlaceholder("efficiency") { it.stats()?.efficiencyTicks?.toString() },
        InternalPlaceholder("capacity") { it.stats()?.storageSize?.toString() },
        InternalPlaceholder("limit_status") { context ->
            context.state()?.let {
                messages.line(if (it.limitedSince != null) "gui.main.limit.status-limited" else "gui.main.limit.status-active")
            }
        },
        InternalPlaceholder("limit_name") { context ->
            context.minion()?.let { minion ->
                limits.breachOf(minion)?.let { messages.limitName(it.scope, minion.state.definition) } ?: ""
            }
        },
        InternalPlaceholder("limit_count") { context ->
            context.minion()?.let { minion -> limits.breachOf(minion)?.count?.toString() ?: "" }
        },
        InternalPlaceholder("limit_max") { context ->
            context.minion()?.let { minion -> limits.breachOf(minion)?.let { capText(it.max) } ?: "" }
        },
        InternalPlaceholder("minion_count") { context -> context.ownerCount(LimitScope.MINION) },
        InternalPlaceholder("minion_limit") { context ->
            context.ownedState()?.let { (owner, state) -> capText(limits.capsFor(owner).minionCap(state.definition.id)) }
        },
        InternalPlaceholder("type_count") { context ->
            context.ownedState()?.let { (owner, state) -> limits.countOfType(owner, state.definition.type).toString() }
        },
        InternalPlaceholder("type_limit") { context ->
            context.ownedState()?.let { (owner, state) -> capText(limits.capsFor(owner).typeCap(state.definition.type)) }
        },
        InternalPlaceholder("player_count") { context -> context.ownerCount(LimitScope.PLAYER) },
        InternalPlaceholder("player_limit") { context ->
            context.ownedState()?.let { (owner, _) -> capText(limits.capsFor(owner).total) }
        }
    )

    private fun capText(cap: Int, infinity: () -> String = { messages.line("infinity") }): String =
        if (cap == PlacementLimits.UNLIMITED) infinity() else cap.toString()

    private fun TemplateContext.ownedState(): Pair<UUID, MinionState>? =
        state()?.let { state -> state.owner?.let { it to state } }

    private fun TemplateContext.ownerCount(scope: LimitScope): String? =
        ownedState()?.let { (owner, state) -> limits.countOf(owner, scope, state.definition).toString() }

    private fun ownerName(state: MinionState): String {
        val owner = state.owner ?: return UNOWNED
        return Bukkit.getOfflinePlayer(owner).name ?: UNKNOWN
    }

    private fun storedSlots(state: MinionState): Int =
        state.inventoryBytes?.let { bytes -> InventoryCodec.decode(bytes).count { it != null } } ?: 0

    private fun TemplateContext.minion(): Minion? = get(MINION)

    /** A placed minion's state, or the state carried by an item when there is no minion. */
    private fun TemplateContext.state(): MinionState? = minion()?.state ?: get(STATE)

    /** A placed minion's live stats, or an item's base stats at its stored level. */
    private fun TemplateContext.stats(): MinionStats? =
        minion()?.stats ?: get<MinionState>(STATE)?.let { MinionStats(it.definition.baseStats, it.level) }

    companion object {
        /** Context key every minion-aware placeholder resolves against. */
        const val MINION = "minion"

        /** Context key for a minion that is not placed, such as the one an item carries. */
        const val STATE = "minion_state"

        private const val UNOWNED = "Unowned"
        private const val UNKNOWN = "Unknown"
    }
}
