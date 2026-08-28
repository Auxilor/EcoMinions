package com.exanthiax.ecominions.core.world

import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.TestableItem

/**
 * The item-identity operations this plugin needs.
 *
 * The sibling of [BlockLookup], and an interface for the same two reasons:
 * eco's own are statics on [Items], which no unit test can call, and a minion's
 * drops may be custom items no material can name.
 */
interface ItemLookup {
    /** @return The item [id] names, or null if nothing on this server matches it. */
    fun lookup(id: String): TestableItem?
}

/** The real thing, delegating to eco. */
class EcoItemLookup : ItemLookup {
    /**
     * eco answers an unresolvable id with an empty item rather than null, and
     * keeps that sentinel private - [Items.isEmpty] is its own test for it.
     */
    override fun lookup(id: String): TestableItem? =
        Items.lookup(id).takeIf { !Items.isEmpty(it.item) }
}
