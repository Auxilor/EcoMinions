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
    val xp: NamespacedKey = NamespacedKey(plugin, "xp")
    val owner: NamespacedKey = NamespacedKey(plugin, "owner")
    val upgrades: NamespacedKey = NamespacedKey(plugin, "upgrades")
    val inventory: NamespacedKey = NamespacedKey(plugin, "inventory")
    val minionUuid: NamespacedKey = NamespacedKey(plugin, "uuid")
    val shutdownTimestamp: NamespacedKey = NamespacedKey(plugin, "shutdown_ts")
    val fuelId: NamespacedKey = NamespacedKey(plugin, "fuel_id")
    val fuelRemaining: NamespacedKey = NamespacedKey(plugin, "fuel_remaining")
    val seeded: NamespacedKey = NamespacedKey(plugin, "seeded")
    val storedExperience: NamespacedKey = NamespacedKey(plugin, "stored_experience")
    val killSample: NamespacedKey = NamespacedKey(plugin, "kill_sample")

    /** On a mob a slayer spawned: the spawning minion's UUID. */
    val spawnedBy: NamespacedKey = NamespacedKey(plugin, "minion")

    companion object {
        val STRING: PersistentDataType<String, String> = PersistentDataType.STRING
        val INT: PersistentDataType<Int, Int> = PersistentDataType.INTEGER
        val LONG: PersistentDataType<Long, Long> = PersistentDataType.LONG
        val BYTE: PersistentDataType<Byte, Byte> = PersistentDataType.BYTE
        val BYTES: PersistentDataType<ByteArray, ByteArray> = PersistentDataType.BYTE_ARRAY
        val DOUBLE: PersistentDataType<Double, Double> = PersistentDataType.DOUBLE
        val LONGS: PersistentDataType<LongArray, LongArray> = PersistentDataType.LONG_ARRAY
    }
}
