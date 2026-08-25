package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.api.minion.MinionType

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
    }
}
