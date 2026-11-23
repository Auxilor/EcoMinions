package com.exanthiax.ecominions.libreforge.filters

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.minions.service.Minion
import org.bukkit.entity.Entity
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.config.ConfigViolation
import wiki.ecohub.config.ViolationContext
import wiki.ecohub.rules.filter.Filter
import wiki.ecohub.rules.holder.get
import wiki.ecohub.rules.trigger.TriggerData

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
        "With no minion in the trigger data, minion_types blocks and not_minion_types passes."
    )

    override val isValueCacheable = true

    override fun getValue(config: Config, data: TriggerData?, key: String): Collection<String> =
        config.getStrings(key)

    override fun isMet(data: TriggerData, value: Collection<String>, compileData: Collection<MinionType>): Boolean {
        val minion = data.dispatcher.get<Minion>() ?: data.victim?.let(minionAt) ?: return false
        return minion.state.definition.type in compileData
    }

    override fun makeCompileData(
        config: Config,
        context: ViolationContext,
        values: Collection<String>
    ): Collection<MinionType> = values.mapNotNull { raw ->
        MinionType.parse(raw).also { type ->
            if (type == null) {
                context.log(this, ConfigViolation(param = id, message = "Unknown minion type: $raw"))
            }
        }
    }
}
