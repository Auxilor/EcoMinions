package com.exanthiax.ecominions.api.island

/** An action on a minion that an island may allow or deny a player. */
enum class IslandPrivilege {
    /** Placing a minion on the island. */
    PLACE,

    /** Opening a minion's menu and using its buttons. */
    USE,

    /** Picking a minion up. */
    PICKUP
}
