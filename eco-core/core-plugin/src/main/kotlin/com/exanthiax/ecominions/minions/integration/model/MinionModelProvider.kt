package com.exanthiax.ecominions.minions.integration.model

import com.exanthiax.ecominions.minions.model.MinionModel
import org.bukkit.entity.ArmorStand

/** Attaches and animates models from one model plugin. */
interface MinionModelProvider {
    val provider: MinionModel.Provider

    /** @return If [stand] now carries the model [id], whether it was just added or already there. */
    fun attach(stand: ArmorStand, id: String): Boolean

    fun play(stand: ArmorStand, animation: String)

    fun stop(stand: ArmorStand, animation: String)
}
