package com.exanthiax.ecominions.libreforge.conditions

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.minions.service.Minion
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.config.arguments
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.condition.Condition
import wiki.ecohub.rules.holder.Dispatcher
import wiki.ecohub.rules.holder.ProvidedHolder
import wiki.ecohub.rules.holder.get

object ConditionMinionIsType : Condition<NoCompileData>("minion_is_type") {
    override val description = "Passes when the minion is one of the given types."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only works on minions. Anywhere else it never passes.",
        "This checks the minion's type, not its ID. Use minion types like miner, not minion IDs like coal."
    )

    override val arguments = arguments {
        require(
            "types",
            "You must specify the minion types!",
            description = "The minion types to match: miner, woodcutter, farmer, slayer, fishing or collector.",
            type = ArgType.STRING_LIST
        )
    }

    override fun isMet(
        dispatcher: Dispatcher<*>,
        config: Config,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ): Boolean {
        val minion = dispatcher.get<Minion>() ?: return false
        val wanted = config.getStrings("types").mapNotNull { MinionType.parse(it) }
        return minion.state.definition.type in wanted
    }
}
