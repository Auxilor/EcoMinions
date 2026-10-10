package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.config.IntRanges
import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.world.ItemLookup
import com.exanthiax.ecominions.minions.model.MinionDropList
import com.willfp.eco.core.config.interfaces.Config

/** Parses a `drops:` list once at load. A bad entry is reported and skipped, never fatal. */
class MinionDropListLoader(
    private val items: ItemLookup
) {
    fun load(sections: List<Config>, path: String, context: LoadContext): MinionDropList =
        MinionDropList(sections.mapNotNull { entry(it, path, context) })

    private fun entry(section: Config, path: String, context: LoadContext): MinionDropList.Entry? {
        val raw = section.getStringOrNull("item")?.takeIf { it.isNotBlank() }
            ?: return context.invalid(path, "", "an item or custom item id")
        val item = items.lookup(raw)?.item
            ?: return context.invalid(path, raw, "an item or custom item id")

        return MinionDropList.Entry(
            item = item,
            chance = readChance(section, path, context),
            amount = readRange(section, "amount", path, context) ?: 1..1,
            experience = readRange(section, "xp", path, context)
        )
    }

    private fun readChance(section: Config, path: String, context: LoadContext): Double {
        val chance = section.getDoubleOrNull("chance") ?: return 1.0
        if (chance !in 0.0..1.0) {
            context.violation(path, "chance $chance is outside 0 to 1 - it is clamped")
        }
        return chance.coerceIn(0.0, 1.0)
    }

    private fun readRange(section: Config, key: String, path: String, context: LoadContext): IntRange? {
        val raw = section.getStringOrNull(key)?.takeIf { it.isNotBlank() } ?: return null
        return IntRanges.parse(raw) ?: context.invalid(path, raw, "a whole number or a range like 1-3 for $key")
    }
}
