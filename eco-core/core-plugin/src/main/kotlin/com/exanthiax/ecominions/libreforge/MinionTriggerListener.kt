package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.minions.service.Minion
import com.willfp.libreforge.triggers.DispatchedTrigger

/**
 * Observes every trigger [MinionTriggerDispatch] sends.
 *
 * An interface rather than the function type it reads like, because the
 * composition root wires this up in the plugin's field initialisers, which run
 * before libreforge-loader has put libreforge on the classpath: a Kotlin lambda
 * there compiles to an `invokedynamic` whose bootstrap resolves
 * [DispatchedTrigger] on the spot and kills plugin load with
 * NoClassDefFoundError. An ordinary implementing class defers that resolution to
 * the first dispatch, long after libreforge is loaded.
 */
interface MinionTriggerListener {
    fun onTrigger(minion: Minion, dispatched: DispatchedTrigger)

    /** Ignores every dispatch, for the wiring that has nothing to observe. */
    object None : MinionTriggerListener {
        override fun onTrigger(minion: Minion, dispatched: DispatchedTrigger) = Unit
    }
}
