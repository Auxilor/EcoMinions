package com.exanthiax.ecominions.minions.integration.model

import com.exanthiax.ecominions.minions.model.MinionModel
import org.bukkit.entity.ArmorStand
import wiki.ecohub.provider.content.ExternalModelService
import wiki.ecohub.provider.content.ModelAnimation

/** Attaches and animates models through eco, for the model plugin [provider] names. */
class EcoModelProvider(
    override val provider: MinionModel.Provider
) : MinionModelProvider {
    private val loop = ModelAnimation.builder().loopMode(ModelAnimation.LoopMode.LOOP).build()

    override fun attach(stand: ArmorStand, id: String): Boolean {
        if (!ExternalModelService.attachModel(entity = stand, providerId = provider.configName, modelId = id)) {
            return false
        }

        ExternalModelService.setBaseEntityVisible(stand, false)
        return true
    }

    override fun play(stand: ArmorStand, animation: String) {
        ExternalModelService.playAnimation(stand, null, animation, loop)
    }

    override fun stop(stand: ArmorStand, animation: String) {
        ExternalModelService.stopAnimation(stand, null, animation, true)
    }
}
