package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.placeholders.TemplateRenderer
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionType
import com.willfp.eco.core.items.Items
import org.bukkit.inventory.ItemStack

/** Builds and reads the placeable item that carries a minion between worlds. */
class MinionItemFactory(
    private val repository: MinionStateRepository,
    private val renderer: TemplateRenderer
) {
    fun build(state: MinionState): ItemStack {
        val type = state.type
        val item = Items.lookup(type.spawnItem.itemLookup).item.clone()

        val meta = item.itemMeta ?: return item
        val context = TemplateContext.of(
            "level" to state.level,
            "minion_name" to type.name
        )

        meta.setDisplayName(renderer.render(type.spawnItem.displayName, context))
        meta.lore = renderer.render(type.spawnItem.lore, context)
        repository.write(meta.persistentDataContainer, state)

        item.itemMeta = meta
        return item
    }

    /** @return The state stored on [item], or null if it is not a minion item. */
    fun read(item: ItemStack): MinionState? =
        item.itemMeta?.let { repository.read(it.persistentDataContainer) }

    fun isMinionItem(item: ItemStack?): Boolean =
        item?.itemMeta?.let { repository.isMinion(it.persistentDataContainer) } ?: false

    /** The head shown on a stand that has no configured helmet. */
    fun headFor(type: MinionType): ItemStack = build(MinionState(type = type))
}
