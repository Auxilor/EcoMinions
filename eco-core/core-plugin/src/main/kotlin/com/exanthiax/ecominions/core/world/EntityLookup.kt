package com.exanthiax.ecominions.core.world

import wiki.ecohub.game.entity.Entities
import wiki.ecohub.game.entity.lookup.EmptyTestableEntity
import wiki.ecohub.game.entity.lookup.TestableEntity

/** The sibling of [BlockLookup] for mobs, which may be custom mobs from EcoMobs. */
interface EntityLookup {
    fun lookup(id: String): TestableEntity?
}

class EcoEntityLookup : EntityLookup {
    override fun lookup(id: String): TestableEntity? =
        Entities.lookup(id).takeIf { it !is EmptyTestableEntity }
}
