package com.exanthiax.ecominions.libreforge.conditions

import com.exanthiax.ecominions.minions.service.Minion
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.config.arguments
import wiki.ecohub.config.section.getDoubleFromExpression
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.condition.Condition
import wiki.ecohub.rules.holder.Dispatcher
import wiki.ecohub.rules.holder.ProvidedHolder
import wiki.ecohub.rules.holder.get
import wiki.ecohub.rules.trigger.TriggerData

object ConditionMinionStorageAbovePercent : Condition<NoCompileData>("minion_storage_above_percent") {
    override val description = "Passes when the minion's storage is at least the given percentage full."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only works on minions. Anywhere else it never passes."
    )

    override val arguments = arguments {
        require(
            "percent",
            "You must specify the percentage!",
            description = "How full the storage must be, from 0 to 100.",
            type = ArgType.EXPRESSION
        )
    }

    override fun isMet(
        dispatcher: Dispatcher<*>,
        config: Config,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ): Boolean {
        val minion = dispatcher.get<Minion>() ?: return false
        if (minion.storageSize == 0) {
            return false
        }

        val filled = minion.usedStorage.toDouble() / minion.storageSize * 100.0
        return filled >= config.getDoubleFromExpression("percent", null as TriggerData?)
    }
}
