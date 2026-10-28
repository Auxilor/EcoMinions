package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.event.MinionBreedEvent
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionBreed
import com.exanthiax.ecominions.minions.model.BreederSettings
import com.exanthiax.ecominions.minions.model.accepts
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.WorkAreaEntities
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.Animals
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/**
 * Breeds one ready pair in its box per action, using two of the food stored in itself.
 *
 * Earns nothing offline: breeding needs live animals, and nobody is there to load food.
 */
class BreederActivity(
    private val entities: WorkAreaEntities,
    private val storageSaved: (Minion) -> Unit,
    private val spawnedByKey: NamespacedKey,
    private val signals: ActivitySignals,
    private val random: Random = Random.Default,
    private val canWork: (Minion, Block) -> Boolean = { _, _ -> true }
) : MinionActivity {

    override fun act(minion: Minion, now: Long): ActivityResult {
        val settings = minion.state.definition.breeder
        return when {
            settings == null -> {
                ActivityResult.BLOCKED
            }
            storedFood(minion, settings).isEmpty() -> {
                minion.blockedReason = NO_FOOD_STORED
                ActivityResult.BLOCKED
            }
            else -> {
                cycle(minion, settings)
            }
        }
    }

    override fun hasWork(minion: Minion): Boolean = true

    override fun offlineCycle(minion: Minion): OfflineYield? = null

    private fun isReady(animal: Animals): Boolean =
        animal.isAdult && animal.canBreed() && !animal.isLoveMode && animal.customName() == null &&
            !animal.persistentDataContainer.has(spawnedByKey)

    private fun storedFood(minion: Minion, settings: BreederSettings): List<ItemStack> =
        (0 until minion.storage.size).mapNotNull { index ->
            minion.storage.getItem(index)?.takeIf(settings::accepts)
        }

    private fun cycle(minion: Minion, settings: BreederSettings): ActivityResult {
        val animals = entities.find(minion, Animals::class.java)
            .filter { animal -> settings.targets.any { it.matches(animal) } && canWork(minion, animal.location.block) }
        val groups = animals.filter(::isReady).groupBy { it.type }.values.shuffled(random)
        return when {
            animals.size >= minion.state.definition.baseStats.evaluate(settings.maxMobs, minion.level) -> {
                minion.blockedReason = PEN_FULL
                ActivityResult.BUSY
            }
            breedFirst(minion, settings, groups) -> {
                ActivityResult.WORKED
            }
            else -> {
                minion.blockedReason = NOTHING_TO_BREED
                ActivityResult.BUSY
            }
        }
    }

    private fun breedFirst(minion: Minion, settings: BreederSettings, groups: List<List<Animals>>): Boolean {
        for (group in groups) {
            if (breed(minion, settings, group)) {
                return true
            }
        }
        return false
    }

    private fun foodFor(minion: Minion, settings: BreederSettings, pair: List<Animals>): ItemStack? =
        if (pair.size < 2) {
            null
        } else {
            storedFood(minion, settings)
                .firstOrNull { minion.storage.countSimilar(it) >= FOOD_PER_BREEDING && pair.first().isBreedItem(it) }
                ?.let(::portionOf)
        }

    private fun breed(minion: Minion, settings: BreederSettings, pair: List<Animals>): Boolean {
        val food = foodFor(minion, settings, pair)
        val fed = food != null &&
            signals.allows(
                MinionBreedEvent(minion = minion, first = pair[0], second = pair[1], food = food.clone()),
                MinionBreedEvent.getHandlerList()
            ) &&
            consumeFood(minion, food)
        if (fed) {
            pair.take(2).forEach { it.loveModeTicks = LOVE_MODE_TICKS }
            storageSaved(minion)
            signals.trigger(minion, TriggerMinionBreed, pair.first().location, FOOD_PER_BREEDING.toDouble())
        }
        return fed
    }

    private fun consumeFood(minion: Minion, food: ItemStack): Boolean {
        val removed = minion.storage.remove(food)
        if (removed in 1 until FOOD_PER_BREEDING) {
            minion.storage.add(food.clone().apply { amount = removed })
        }
        return removed >= FOOD_PER_BREEDING
    }

    private fun portionOf(food: ItemStack): ItemStack = food.clone().apply { amount = FOOD_PER_BREEDING }

    private companion object {
        const val NOTHING_TO_BREED = "nothing-to-breed"
        const val NO_FOOD_STORED = "no-food-stored"
        const val PEN_FULL = "pen-full"
        const val LOVE_MODE_TICKS = 600
        const val FOOD_PER_BREEDING = 2
    }
}
