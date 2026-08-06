package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpgradeCodecTest {

    @Test
    fun `encoding then decoding preserves every tier`() {
        val upgrades = mapOf("speed" to 3, "capacity" to 1)

        assertEquals(upgrades, UpgradeCodec.decode(UpgradeCodec.encode(upgrades)))
    }

    @Test
    fun `an empty map encodes to an empty string`() {
        assertEquals("", UpgradeCodec.encode(emptyMap()))
    }

    @Test
    fun `blank data decodes to no upgrades`() {
        assertTrue(UpgradeCodec.decode("").isEmpty())
    }

    @Test
    fun `entries without a tier are dropped`() {
        assertEquals(mapOf("speed" to 2), UpgradeCodec.decode("speed:2,capacity"))
    }

    @Test
    fun `entries with a non-numeric tier are dropped`() {
        assertEquals(mapOf("speed" to 2), UpgradeCodec.decode("speed:2,fortune:many"))
    }
}
