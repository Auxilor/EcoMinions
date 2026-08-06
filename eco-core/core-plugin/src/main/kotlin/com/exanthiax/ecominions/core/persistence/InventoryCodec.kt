package com.exanthiax.ecominions.core.persistence

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Serialises minion storage to the byte array stored in persistent data. */
object InventoryCodec {

    fun encode(items: Array<ItemStack?>): ByteArray {
        val bytes = ByteArrayOutputStream()
        BukkitObjectOutputStream(bytes).use { out ->
            out.writeInt(items.size)
            for (item in items) {
                out.writeObject(item)
            }
        }
        return bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): Array<ItemStack?> {
        BukkitObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
            val size = input.readInt()
            return Array(size) { input.readObject() as? ItemStack }
        }
    }
}
