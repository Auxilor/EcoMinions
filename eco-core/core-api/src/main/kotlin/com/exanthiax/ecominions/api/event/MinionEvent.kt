package com.exanthiax.ecominions.api.event

import com.exanthiax.ecominions.api.minion.Minion

/**
 * Implemented by every event about a minion that already exists.
 *
 * Place, give and take events do not implement this: the minion they describe
 * does not exist yet.
 */
interface MinionEvent {
    val minion: Minion
}
