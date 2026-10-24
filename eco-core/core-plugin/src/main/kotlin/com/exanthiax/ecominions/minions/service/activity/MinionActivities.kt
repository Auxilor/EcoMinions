package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.minion.MinionType

/**
 * Every activity, addressed by type.
 *
 * An exhaustive `when` rather than a map, so adding a type is a compile error
 * here until its activity exists.
 */
class MinionActivities(
    private val block: BlockActivity,
    private val collection: CollectionActivity,
    private val slayer: SlayerActivity,
    private val fishing: FishingActivity,
    private val feeder: FeederActivity
) {
    operator fun get(type: MinionType): MinionActivity = when (type) {
        MinionType.MINER, MinionType.FARMER, MinionType.WOODCUTTER -> block
        MinionType.SLAYER -> slayer
        MinionType.FISHING -> fishing
        MinionType.COLLECTOR -> collection
        MinionType.FEEDER -> feeder
    }
}
