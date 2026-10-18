package com.exanthiax.ecominions.libreforge.conditions

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.Dispatcher
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.arguments
import com.willfp.libreforge.conditions.Condition
import com.willfp.libreforge.get

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
