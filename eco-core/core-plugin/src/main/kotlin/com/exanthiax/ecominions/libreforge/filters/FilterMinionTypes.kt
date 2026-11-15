package com.exanthiax.ecominions.libreforge.filters

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.filters.Filter
import com.willfp.libreforge.get
import com.willfp.libreforge.triggers.TriggerData
import org.bukkit.entity.Entity

/**
 * Matches the minion behind a trigger by type.
 *
 * Reads the dispatcher for minion triggers and the victim for player triggers that
 * carry a minion, so one filter covers both sides.
 */
class FilterMinionTypes(
    private val minionAt: (Entity) -> Minion?
) : Filter<Collection<MinionType>, Collection<String>>("minion_types") {
    override val description = "Matches when the minion is one of the given types."
    override val categories = setOf("minion")
    override val valueType = ArgType.STRING_LIST
    override val additionalInfo = listOf(
        "Types: miner, woodcutter, farmer, slayer, fishing, collector, feeder or breeder. Not minion IDs like coal.",
        "Works on minion triggers, and on player triggers that carry a minion, like minion_withdraw and click_minion.",
        "Passes automatically when there is no minion in the trigger data."
    )

    override fun getValue(config: Config, data: TriggerData?, key: String): Collection<String> =
        config.getStrings(key)

    override fun isMet(data: TriggerData, value: Collection<String>, compileData: Collection<MinionType>): Boolean {
        val minion = data.dispatcher.get<Minion>() ?: data.victim?.let(minionAt) ?: return true
        return minion.state.definition.type in compileData
    }

    override fun makeCompileData(
        config: Config,
        context: ViolationContext,
        values: Collection<String>
    ): Collection<MinionType> = values.mapNotNull { MinionType.parse(it) }
}
