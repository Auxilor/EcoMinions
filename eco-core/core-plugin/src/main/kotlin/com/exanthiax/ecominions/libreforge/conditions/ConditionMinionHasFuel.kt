package com.exanthiax.ecominions.libreforge.conditions

import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.Dispatcher
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.conditions.Condition
import com.willfp.libreforge.get

object ConditionMinionHasFuel : Condition<NoCompileData>("minion_has_fuel") {
    override val description = "Passes when the minion is burning fuel."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only works on minions. Anywhere else it never passes."
    )

    override fun isMet(
        dispatcher: Dispatcher<*>,
        config: Config,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ): Boolean {
        val minion = dispatcher.get<Minion>() ?: return false
        return minion.burningFuelId != null
    }
}
