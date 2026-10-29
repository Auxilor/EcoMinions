package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.api.event.MinionBreedEvent
import com.exanthiax.ecominions.minions.service.ExperienceSink
import com.exanthiax.ecominions.minions.service.MinionService
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityBreedEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/** Sends the XP from a breeder minion's breeding through `minion.xp.mode`, where vanilla would drop it as orbs. */
class MinionBreedListener(
    private val minions: MinionService,
    private val experience: ExperienceSink,
    private val bredByKey: NamespacedKey
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onMinionBreed(event: MinionBreedEvent) {
        for (animal in listOf(event.first, event.second)) {
            animal.persistentDataContainer.set(bredByKey, PersistentDataType.STRING, event.minion.uuid.toString())
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBreed(event: EntityBreedEvent) {
        val parents = listOf(event.mother, event.father)
        val bredBy = parents.firstNotNullOfOrNull {
            it.persistentDataContainer.get(bredByKey, PersistentDataType.STRING)
        }
        parents.forEach { it.persistentDataContainer.remove(bredByKey) }

        val minion = bredBy
            ?.takeIf { event.breeder == null }
            ?.let { minions.getByUuid(UUID.fromString(it)) }
            ?: return
        experience.add(minion, event.experience)
        event.experience = 0
    }
}
