package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.model.MinionStat
import com.exanthiax.ecominions.minions.service.Minion

/**
 * Capacity is structural: changing it resizes a live inventory rather than only
 * moving a number, so it is the one stat effect with an after-change hook.
 */
object EffectMinionCapacity : MinionStatEffect(
    id = "minion_capacity",
    stat = MinionStat.INVENTORY_ROWS,
    argument = "rows"
) {
    override val description = "Adds rows to a minion's storage."
    override val categories = setOf("minion")

    override fun afterChange(minion: Minion) {
        val overflow = minion.resizeStorage(minion.stats.inventoryRows, STORAGE_TITLE)

        for (item in overflow) {
            minion.entity.world.dropItemNaturally(minion.entity.location, item)
        }
    }

    private const val STORAGE_TITLE = "Minion"
}
