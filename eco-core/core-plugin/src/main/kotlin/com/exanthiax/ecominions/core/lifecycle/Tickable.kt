package com.exanthiax.ecominions.core.lifecycle

/** A repeating task owned by [LifecycleManager] rather than scheduling itself. */
interface Tickable {
    /** Ticks between runs. */
    val tickInterval: Long

    fun tick()
}
