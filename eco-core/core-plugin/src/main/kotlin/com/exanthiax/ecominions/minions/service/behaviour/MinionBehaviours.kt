package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.RestoreMode

/**
 * Every behaviour, addressed by type.
 *
 * An exhaustive `when` rather than a map, so adding a type is a compile error
 * here until its behaviour exists.
 */
class MinionBehaviours(
    private val miner: MinerBehaviour,
    private val woodcutter: WoodcutterBehaviour,
    private val farmer: FarmerBehaviour
) {
    operator fun get(type: MinionType): MinionBehaviour = when (type) {
        MinionType.MINER -> miner
        MinionType.WOODCUTTER -> woodcutter
        MinionType.FARMER -> farmer
        MinionType.COLLECTOR -> error("$type has no block behaviour")
    }

    /**
     * @return The behaviour for [type], unless [block] overrides it.
     *
     * A block-level `restore:` wins, so one awkward block can be given its own
     * handling without a whole minion type existing for it.
     */
    fun forBlock(type: MinionType, block: MinionBlock?): MinionBehaviour =
        when (block?.restore) {
            RestoreMode.REGEN -> miner
            RestoreMode.REPLANT -> farmer
            RestoreMode.TREE -> woodcutter
            null -> this[type]
        }
}
