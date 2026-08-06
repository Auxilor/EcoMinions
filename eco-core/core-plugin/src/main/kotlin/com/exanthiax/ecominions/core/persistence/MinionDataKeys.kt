package com.exanthiax.ecominions.core.persistence

import com.willfp.eco.core.EcoPlugin
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType

/**
 * Every persistent data key the plugin writes, created in one place.
 *
 * Keys are namespaced by the plugin, so the stored namespace stays `ecominions`.
 */
class MinionDataKeys(plugin: EcoPlugin) {
    val isMinion: NamespacedKey = NamespacedKey(plugin, "is_minion")
    val type: NamespacedKey = NamespacedKey(plugin, "type")
    val level: NamespacedKey = NamespacedKey(plugin, "level")
    val owner: NamespacedKey = NamespacedKey(plugin, "owner")
    val upgrades: NamespacedKey = NamespacedKey(plugin, "upgrades")
    val inventory: NamespacedKey = NamespacedKey(plugin, "inventory")
    val minionUuid: NamespacedKey = NamespacedKey(plugin, "uuid")
    val shutdownTimestamp: NamespacedKey = NamespacedKey(plugin, "shutdown_ts")
    val fuelId: NamespacedKey = NamespacedKey(plugin, "fuel_id")
    val fuelEndsAt: NamespacedKey = NamespacedKey(plugin, "fuel_ends_at")

    companion object {
        val STRING: PersistentDataType<String, String> = PersistentDataType.STRING
        val INT: PersistentDataType<Int, Int> = PersistentDataType.INTEGER
        val LONG: PersistentDataType<Long, Long> = PersistentDataType.LONG
        val BYTE: PersistentDataType<Byte, Byte> = PersistentDataType.BYTE
        val BYTES: PersistentDataType<ByteArray, ByteArray> = PersistentDataType.BYTE_ARRAY
    }
}
