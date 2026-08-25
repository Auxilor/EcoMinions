package com.exanthiax.ecominions.libreforge.conditions

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

    override val arguments = arguments {
        require(
            "types",
            "You must specify the minion types!",
            description = "A list of minion type ids.",
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
        return minion.state.definition.id in config.getStrings("types").map { it.lowercase() }
    }
}
