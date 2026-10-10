package com.exanthiax.ecominions.core.world

import com.willfp.eco.core.entities.Entities
import com.willfp.eco.core.entities.TestableEntity
import com.willfp.eco.core.entities.impl.EmptyTestableEntity

/** The sibling of [BlockLookup] for mobs, which may be custom mobs from EcoMobs. */
interface EntityLookup {
    fun lookup(id: String): TestableEntity?
}

class EcoEntityLookup : EntityLookup {
    override fun lookup(id: String): TestableEntity? =
        Entities.lookup(id).takeIf { it !is EmptyTestableEntity }
}
