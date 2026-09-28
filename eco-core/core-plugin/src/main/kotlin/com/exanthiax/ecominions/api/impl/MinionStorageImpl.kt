package com.exanthiax.ecominions.api.impl

import com.exanthiax.ecominions.api.minion.MinionStorage
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionStateRepository
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/** The public handle on one minion's storage. Saves after every change. */
class MinionStorageImpl(
    private val minion: Minion,
    private val repository: MinionStateRepository
) : MinionStorage {

    override val size: Int
        get() = minion.storage.size

    override val used: Int
        get() = minion.storage.used

    override val isFull: Boolean
        get() = minion.storage.isFull

    override fun contents(): Array<ItemStack?> =
        minion.storage.snapshot().map { it?.clone() }.toTypedArray()

    override fun getItem(slot: Int): ItemStack? = minion.storage.getItem(slot)?.clone()

    override fun setItem(slot: Int, item: ItemStack?) {
        minion.storage.setItem(slot, item?.clone())
        repository.save(minion)
    }

    override fun add(item: ItemStack): ItemStack? =
        minion.storage.add(item.clone()).also { repository.save(minion) }

    override fun remove(item: ItemStack): Int =
        minion.storage.remove(item).also { repository.save(minion) }

    override fun clear() {
        minion.storage.clear()
        repository.save(minion)
    }

    override fun countOf(material: Material): Int = minion.storage.count { it.type == material }
}
