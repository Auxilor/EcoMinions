package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.api.minion.LimitScope
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.model.MinionDefinition

/** How a limit is named to players: the minion or type it caps, or the lang names of the other two. */
fun Messages.limitName(scope: LimitScope, definition: MinionDefinition): String = when (scope) {
    LimitScope.PLAYER -> line("limit-names.player")
    LimitScope.ISLAND -> line("limit-names.island")
    LimitScope.TYPE -> definition.type.displayName()
    LimitScope.MINION -> definition.name
}

fun MinionType.displayName(): String = id.replaceFirstChar { it.uppercase() }
