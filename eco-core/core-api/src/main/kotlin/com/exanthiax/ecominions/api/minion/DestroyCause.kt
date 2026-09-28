package com.exanthiax.ecominions.api.minion

/** Why a minion is being removed from the world. */
enum class DestroyCause {
    /** Removed through the API by another plugin. */
    API,

    /** Removed by an EcoMinions command. */
    COMMAND,

    /** Removed because a player picked it up. */
    PICKUP
}
