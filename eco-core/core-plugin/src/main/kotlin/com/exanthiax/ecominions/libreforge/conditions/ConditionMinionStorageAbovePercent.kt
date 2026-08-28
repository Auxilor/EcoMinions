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
import com.willfp.libreforge.getDoubleFromExpression
import com.willfp.libreforge.triggers.TriggerData

object ConditionMinionStorageAbovePercent : Condition<NoCompileData>("minion_storage_above_percent") {
    override val description = "Passes when the minion's storage is at least the given percentage full."
    override val categories = setOf("minion")

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
