package com.exanthiax.ecominions.upgrades.model

import com.willfp.libreforge.Holder
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpgradeTest {

    private fun tier(level: Int, holder: Holder?) = UpgradeTier(
        level = level,
        value = level.toDouble(),
        statDelta = "",
        costs = emptyList(),
        holder = holder
    )

    @Test
    fun `holderFor returns the holder compiled for that tier`() {
        val tierOne = mockk<Holder>()
        val tierTwo = mockk<Holder>()
        val upgrade = Upgrade(
            id = "speed",
            name = "Speed",
            iconLookup = "sugar",
            tiers = mapOf(1 to tier(1, tierOne), 2 to tier(2, tierTwo))
        )

        assertSame(tierOne, upgrade.holderFor(1))
        assertSame(tierTwo, upgrade.holderFor(2))
    }

    @Test
    fun `holderFor returns null for an unowned or unconfigured tier`() {
        val upgrade = Upgrade(
            id = "speed",
            name = "Speed",
            iconLookup = "sugar",
            tiers = mapOf(1 to tier(1, mockk()))
        )

        assertNull(upgrade.holderFor(0))
        assertNull(upgrade.holderFor(2))
    }

    @Test
    fun `a tier with no effects block has no holder`() {
        val upgrade = Upgrade(
            id = "speed",
            name = "Speed",
            iconLookup = "sugar",
            tiers = mapOf(1 to tier(1, null))
        )

        assertNull(upgrade.holderFor(1))
    }

    @Test
    fun `selectable defaults to false`() {
        val upgrade = Upgrade(
            id = "speed",
            name = "Speed",
            iconLookup = "sugar",
            tiers = mapOf(1 to tier(1, null))
        )

        assertFalse(upgrade.selectable)
    }

    @Test
    fun `selectable can be turned on`() {
        val upgrade = Upgrade(
            id = "compactor",
            name = "Compactor",
            iconLookup = "furnace",
            tiers = mapOf(1 to tier(1, null)),
            selectable = true
        )

        assertTrue(upgrade.selectable)
    }
}
