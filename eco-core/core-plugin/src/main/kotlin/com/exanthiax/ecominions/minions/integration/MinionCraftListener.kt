package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.persistence.MinionDataKeys
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionType
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionService
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack

/** Turns the marker item produced by a minion recipe into the real minion item. */
class MinionCraftListener(
    private val minions: MinionService,
    private val itemFactory: MinionItemFactory,
    private val keys: MinionDataKeys
) : Listener {

    @EventHandler
    fun onPrepare(event: PrepareItemCraftEvent) {
        val type = typeOf(event.inventory.result) ?: return
        val viewer = event.viewers.firstOrNull() as? Player

        val permission = type.crafting?.permission.orEmpty()
        if (permission.isNotBlank() && viewer != null && !viewer.hasPermission(permission)) {
            event.inventory.result = ItemStack(Material.AIR)
            return
        }

        event.inventory.result = itemFactory.build(MinionState(type = type))
    }

    @EventHandler
    fun onCraft(event: CraftItemEvent) {
        val type = typeOf(event.currentItem) ?: return
        event.currentItem = itemFactory.build(MinionState(type = type))
    }

    private fun typeOf(item: ItemStack?): MinionType? {
        val container = item?.itemMeta?.persistentDataContainer ?: return null
        val typeId = container.get(keys.type, MinionDataKeys.STRING) ?: return null
        return minions.getType(typeId)
    }
}
