package com.exanthiax.ecominions.core.registry

import com.exanthiax.ecominions.core.error.LoadContext
import com.willfp.eco.core.config.interfaces.Config

/**
 * Turns one config file into one validated model object.
 *
 * Implementations live in a slice's `integration` package and are the only place
 * config parsing is allowed. Returning null means "this file is broken"; the
 * loader must have reported at least one violation through [LoadContext] first.
 */
fun interface ConfigLoader<T : Any> {
    fun load(id: String, config: Config, context: LoadContext): T?
}
