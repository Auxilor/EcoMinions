package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.integration.items.EcoItemsIntegrationService
import com.exanthiax.ecominions.minions.model.VanillaFishingTable
import com.exanthiax.ecominions.minions.service.activity.OfflineYield
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

/** Picks one catch: the minion's own `drops:`, then EcoItems while the owner is online, then vanilla. */
class FishingLootRoller(
    private val ecoItems: EcoItemsIntegrationService,
    private val vanillaTable: (Location, Player?) -> List<ItemStack>?,
    private val random: Random = Random.Default
) {
    fun roll(minion: Minion, water: Location, owner: Player?): OfflineYield {
        val configured = minion.state.definition.fishing?.drops
        if (configured != null && !configured.isEmpty) {
            return configured.roll(random)
        }
        owner?.let { ecoItems.rollFishing(water, it) }?.let { return it }
        val catch = vanillaTable(water, owner)?.takeIf { it.isNotEmpty() } ?: listOf(VanillaFishingTable.roll(random))
        return OfflineYield(catch, random.nextInt(MIN_XP, MAX_XP + 1))
    }

    private companion object {
        const val MIN_XP = 1
        const val MAX_XP = 6
    }
}
