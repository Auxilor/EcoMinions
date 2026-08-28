package com.exanthiax.ecominions.libreforge

import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.willfp.libreforge.NamedValue
import com.willfp.libreforge.get
import com.willfp.libreforge.registerPlaceholderProvider

/**
 * Binds `%tier_value%` and `%tier%` inside an upgrade holder's effect arguments.
 *
 * Resolved from the minion rather than baked into the compiled config, so one
 * authored `effects:` block serves every rung of the ladder.
 */
class UpgradePlaceholderProvider(
    private val upgrades: () -> Collection<Upgrade>
) {
    fun register() {
        registerPlaceholderProvider { holder, dispatcher ->
            val minion = dispatcher.get<Minion>() ?: return@registerPlaceholderProvider emptyList()
            val key = holder.holder.id.key

            for (upgrade in upgrades()) {
                val tier = minion.state.upgradeLevel(upgrade.id)
                if (key != "upgrade_${upgrade.id}_$tier") {
                    continue
                }
                val value = upgrade.tier(tier)?.value ?: 0.0
                return@registerPlaceholderProvider listOf(
                    NamedValue("tier_value", value),
                    NamedValue("tier", tier.toDouble())
                )
            }

            emptyList()
        }
    }
}
