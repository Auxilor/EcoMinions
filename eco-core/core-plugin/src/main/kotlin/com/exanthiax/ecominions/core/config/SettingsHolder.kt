package com.exanthiax.ecominions.core.config

/**
 * Holds the current value of a settings object.
 *
 * Services are given the holder rather than a `Config`, so config parsing stays
 * in loaders while a reload can still swap the values underneath them.
 */
class SettingsHolder<T : Any>(initial: T) {
    var value: T = initial
        private set

    fun update(newValue: T) {
        value = newValue
    }
}
