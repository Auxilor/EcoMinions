package com.exanthiax.ecominions.minions.model

/** Encodes purchased upgrade tiers as the flat string kept in persistent data. */
object UpgradeCodec {

    fun encode(upgrades: Map<String, Int>): String =
        upgrades.entries.joinToString(",") { "${it.key}:${it.value}" }

    /** Malformed entries are dropped: persisted data is not admin-authored config. */
    fun decode(encoded: String): Map<String, Int> {
        if (encoded.isBlank()) {
            return emptyMap()
        }
        return encoded.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) {
                return@mapNotNull null
            }
            val tier = parts[1].toIntOrNull() ?: return@mapNotNull null
            parts[0] to tier
        }.toMap()
    }
}
