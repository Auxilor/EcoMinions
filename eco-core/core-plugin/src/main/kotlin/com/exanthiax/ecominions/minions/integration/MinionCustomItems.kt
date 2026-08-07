package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionService
import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.items.CustomItem
import com.willfp.eco.core.items.Items
import org.bukkit.NamespacedKey

/**
 * Exposes every minion's spawn item to eco's item lookup system as
 * `ecominions:<id>`, so shops, crates, recipes and rewards can hand one out.
 *
 * The lookup yields a level one item, matching what `/minions give` produces. The
 * test matches any minion item of that type whatever its stored level or owner,
 * so a levelled-up minion still resolves.
 */
class MinionCustomItems(
    private val plugin: EcoPlugin,
    private val minions: MinionService,
    private val itemFactory: MinionItemFactory
) {
    fun registerAll() {
        for (type in minions.types()) {
            val key = NamespacedKey(plugin, type.id)
            Items.registerCustomItem(
                key,
                CustomItem(
                    key,
                    { itemFactory.read(it)?.type?.id == type.id },
                    itemFactory.build(MinionState(type = type))
                )
            )
        }
    }
}
