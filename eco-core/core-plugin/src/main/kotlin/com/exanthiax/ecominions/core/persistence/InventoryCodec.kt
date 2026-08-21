package com.exanthiax.ecominions.core.persistence

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Serialises minion storage to the byte array stored in persistent data. */
object InventoryCodec {

    /**
     * Writes [items] up to its last occupied slot.
     *
     * Storage capacity is configurable and can be large, so writing the empty
     * tail would put kilobytes of nulls in an entity's persistent data for a
     * minion holding one stack. Interior gaps are still written, because slot
     * positions have to survive the round trip.
     */
    fun encode(items: Array<ItemStack?>): ByteArray {
        val length = occupiedLength(items)
        val bytes = ByteArrayOutputStream()
        BukkitObjectOutputStream(bytes).use { out ->
            out.writeInt(length)
            for (index in 0 until length) {
                out.writeObject(items[index])
            }
        }
        return bytes.toByteArray()
    }

    /**
     * How many slots [encode] writes: everything up to and including the last
     * occupied one.
     *
     * Its own function because [encode] cannot be exercised in a unit test -
     * `ItemStack.serialize` needs a running server - while this can.
     */
    internal fun occupiedLength(items: Array<ItemStack?>): Int = items.indexOfLast { it != null } + 1

    fun decode(bytes: ByteArray): Array<ItemStack?> {
        BukkitObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
            val size = input.readInt()
            return Array(size) { input.readObject() as? ItemStack }
        }
    }
}
