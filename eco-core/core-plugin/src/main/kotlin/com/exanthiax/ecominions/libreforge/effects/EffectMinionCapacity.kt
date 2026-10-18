package com.exanthiax.ecominions.libreforge.effects

import com.exanthiax.ecominions.minions.model.MinionStat
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.arguments
import com.exanthiax.ecominions.minions.service.Minion

/**
 * Capacity is structural: changing it resizes a live inventory rather than only
 * moving a number, so it is the one stat effect with an after-change hook.
 */
object EffectMinionCapacity : MinionStatEffect(
    id = "minion_capacity",
    stat = MinionStat.INVENTORY_SLOTS,
    argument = "slots"
) {
    override val description = "Adds slots to a minion's storage."
    override val categories = setOf("minion")
    override val additionalInfo = listOf(
        "Only works on minions: in a minion type's effects or an upgrade's effects. Anywhere else it does nothing.",
        "Permanent, so it needs no triggers. It stacks with every other effect on the same stat."
    )
    override val arguments = arguments {
        require(
            "slots",
            "You must specify slots!",
            description = "How many storage slots to add, up to the minion's max-storage-slots.",
            type = ArgType.EXPRESSION,
            example = "%tier_value%"
        )
    }

    override fun afterChange(minion: Minion) {
        val overflow = minion.resizeStorage(minion.stats.storageSize)

        for (item in overflow) {
            minion.entity.world.dropItemNaturally(minion.entity.location, item)
        }
    }
}
