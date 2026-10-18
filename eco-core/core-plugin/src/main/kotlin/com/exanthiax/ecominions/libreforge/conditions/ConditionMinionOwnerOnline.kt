package com.exanthiax.ecominions.libreforge.conditions

import com.exanthiax.ecominions.libreforge.ownerPlayer
import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.Dispatcher
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.conditions.Condition
import com.willfp.libreforge.get

object ConditionMinionOwnerOnline : Condition<NoCompileData>("minion_owner_online") {
    override val description = "Passes when the minion's owner is online."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only works on minions. Anywhere else it never passes.",
        "Use it to guard effects that act on the player, as minions keep working while their owner is offline."
    )

    override fun isMet(
        dispatcher: Dispatcher<*>,
        config: Config,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ): Boolean {
        val minion = dispatcher.get<Minion>() ?: return false
        return minion.ownerPlayer() != null
    }
}
