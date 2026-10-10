package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.world.EntityLookup
import com.exanthiax.ecominions.minions.model.SlayerSettings
import com.willfp.eco.core.config.interfaces.Config

/** Reads a slayer's `target-mobs`, `max-mobs`, `drops` and `credit-owner`. */
class SlayerSettingsLoader(
    private val entities: EntityLookup,
    private val dropLists: MinionDropListLoader
) {
    fun load(config: Config, context: LoadContext): SlayerSettings? {
        val configured = config.getStrings("target-mobs").filter { it.isNotBlank() }
        if (configured.isEmpty()) {
            context.violation("target-mobs", "no mobs - a slayer would never kill anything")
            return null
        }
        val targets = configured.mapNotNull { id ->
            entities.lookup(id) ?: context.invalid("target-mobs", id, "a mob or custom mob id")
        }
        if (targets.isEmpty()) {
            context.violation("target-mobs", "every configured mob is invalid")
            return null
        }
        return SlayerSettings(
            targets = targets,
            maxMobs = config.getStringOrNull("max-mobs")?.takeIf { it.isNotBlank() } ?: DEFAULT_MAX_MOBS,
            drops = dropLists.load(config.getSubsections("drops"), "drops", context),
            creditOwner = config.getBoolOrNull("credit-owner") ?: false
        )
    }

    private companion object {
        const val DEFAULT_MAX_MOBS = "4"
    }
}
