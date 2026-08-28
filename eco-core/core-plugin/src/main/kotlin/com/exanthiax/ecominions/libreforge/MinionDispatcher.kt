package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.Dispatcher
import org.bukkit.Location
import java.util.UUID

/**
 * A minion, as something libreforge can run effects on.
 *
 * Uses the minion's own uuid rather than the armor stand's, because that is what
 * survives pickup and re-placement - libreforge caches holders and active effects
 * against this, so it must be the identity the player thinks of as "their minion".
 */
@JvmInline
value class MinionDispatcher(
    override val dispatcher: Minion
) : Dispatcher<Minion> {
    override val uuid: UUID
        get() = dispatcher.uuid

    override val location: Location
        get() = dispatcher.location
}

fun Minion.toDispatcher(): Dispatcher<Minion> = MinionDispatcher(this)
