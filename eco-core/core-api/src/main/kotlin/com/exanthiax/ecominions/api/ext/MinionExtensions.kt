package com.exanthiax.ecominions.api.ext

import com.exanthiax.ecominions.api.EcoMinionsApi
import com.exanthiax.ecominions.api.minion.DestroyCause
import com.exanthiax.ecominions.api.minion.Minion
import com.exanthiax.ecominions.api.minion.MinionStorage

/** A handle on this minion's storage. */
val Minion.storage: MinionStorage
    get() = EcoMinionsApi.get().minions().storage(this)

/** @return Whether this minion was removed. */
fun Minion.destroy(cause: DestroyCause = DestroyCause.API): Boolean =
    EcoMinionsApi.get().minions().destroy(this, cause)
