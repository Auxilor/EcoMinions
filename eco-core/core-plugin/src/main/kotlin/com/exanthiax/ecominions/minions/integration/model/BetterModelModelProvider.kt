package com.exanthiax.ecominions.minions.integration.model

import com.exanthiax.ecominions.minions.model.MinionModel
import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.api.animation.AnimationIterator
import kr.toxicity.model.api.animation.AnimationModifier
import kr.toxicity.model.api.bukkit.platform.BukkitAdapter
import org.bukkit.entity.ArmorStand

class BetterModelModelProvider : MinionModelProvider {
    override val provider = MinionModel.Provider.BETTER_MODEL

    private val loop = AnimationModifier.builder().type(AnimationIterator.Type.LOOP).build()

    override fun attach(stand: ArmorStand, id: String): Boolean {
        val model = BetterModel.modelOrNull(id) ?: return false
        model.getOrCreate(BukkitAdapter.adapt(stand))
        return true
    }

    override fun play(stand: ArmorStand, animation: String) {
        BetterModel.registryOrNull(stand.uniqueId)?.trackers()?.forEach { it.animate(animation, loop) }
    }

    override fun stop(stand: ArmorStand, animation: String) {
        BetterModel.registryOrNull(stand.uniqueId)?.trackers()?.forEach { it.stopAnimation(animation) }
    }
}
