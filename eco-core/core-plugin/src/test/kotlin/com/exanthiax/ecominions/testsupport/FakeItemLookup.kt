package com.exanthiax.ecominions.testsupport

import com.exanthiax.ecominions.core.world.ItemLookup
import com.willfp.eco.core.items.TestableItem
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/**
 * An in-memory [ItemLookup] for tests.
 *
 * Matching is by [ItemStack.getType] only, which is all the pure layers need:
 * nothing below the eco seam knows what a custom item is.
 */
class FakeItemLookup(
    private val registered: Map<String, TestableItem> = emptyMap()
) : ItemLookup {

    override fun lookup(id: String): TestableItem? = registered[id]

    companion object {
        /** A [TestableItem] that matches one material. */
        fun testable(material: Material): TestableItem = MaterialFake(material)
    }

    private class MaterialFake(private val material: Material) : TestableItem {
        override fun matches(itemStack: ItemStack?): Boolean = itemStack?.type == material

        override fun getItem(): ItemStack = ItemStack(material)
    }
}
