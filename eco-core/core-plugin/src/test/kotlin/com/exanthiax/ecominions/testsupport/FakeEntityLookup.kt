package com.exanthiax.ecominions.testsupport

import com.exanthiax.ecominions.core.world.EntityLookup
import com.willfp.eco.core.entities.TestableEntity
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType

/** An in-memory [EntityLookup]. Matching is by [Entity.getType] only. */
class FakeEntityLookup(
    private val registered: Map<String, TestableEntity> = emptyMap()
) : EntityLookup {
    override fun lookup(id: String): TestableEntity? = registered[id]

    companion object {
        fun testable(type: EntityType): TestableEntity = object : TestableEntity {
            override fun matches(entity: Entity?): Boolean = entity?.type == type

            override fun spawn(location: Location): Entity = error("tests spawn through the activity's spawner")
        }
    }
}
