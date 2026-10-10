package com.exanthiax.ecominions.core.persistence

import com.exanthiax.ecominions.minions.model.KillRecord
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Serialises a slayer's kill sample to the byte array stored in persistent data. */
object KillSampleCodec {
    fun encode(records: List<KillRecord>): ByteArray {
        val bytes = ByteArrayOutputStream()
        BukkitObjectOutputStream(bytes).use { out ->
            out.writeInt(records.size)
            for (record in records) {
                out.writeInt(record.experience)
                out.writeInt(record.drops.size)
                record.drops.forEach(out::writeObject)
            }
        }
        return bytes.toByteArray()
    }

    fun decode(bytes: ByteArray): List<KillRecord> =
        BukkitObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
            List(input.readInt()) {
                val experience = input.readInt()
                KillRecord(List(input.readInt()) { input.readObject() as ItemStack }, experience)
            }
        }
}
