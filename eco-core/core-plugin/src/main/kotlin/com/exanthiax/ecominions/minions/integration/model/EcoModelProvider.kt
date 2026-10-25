package com.exanthiax.ecominions.minions.integration.model

import com.exanthiax.ecominions.minions.model.MinionModel
import com.willfp.eco.core.integrations.model.ModelAnimation
import com.willfp.eco.core.integrations.model.ModelManager
import org.bukkit.entity.ArmorStand

/** Attaches and animates models through eco, for the model plugin [provider] names. */
class EcoModelProvider(
    override val provider: MinionModel.Provider
) : MinionModelProvider {
    private val loop = ModelAnimation.builder().loopMode(ModelAnimation.LoopMode.LOOP).build()

    override fun attach(stand: ArmorStand, id: String): Boolean {
        if (!ModelManager.attachModel(stand, provider.configName, id)) {
            return false
        }

        ModelManager.setBaseEntityVisible(stand, false)
        return true
    }

    override fun play(stand: ArmorStand, animation: String) {
        ModelManager.playAnimation(stand, null, animation, loop)
    }

    override fun stop(stand: ArmorStand, animation: String) {
        ModelManager.stopAnimation(stand, null, animation, true)
    }
}
