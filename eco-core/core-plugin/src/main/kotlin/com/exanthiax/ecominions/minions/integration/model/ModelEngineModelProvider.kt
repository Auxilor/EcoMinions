package com.exanthiax.ecominions.minions.integration.model

import com.exanthiax.ecominions.minions.model.MinionModel
import com.willfp.eco.core.integrations.modelengine.ModelEngineManager
import org.bukkit.entity.ArmorStand

class ModelEngineModelProvider : MinionModelProvider {
    override val provider = MinionModel.Provider.MODEL_ENGINE

    override fun attach(stand: ArmorStand, id: String): Boolean {
        if (ModelEngineManager.getModeledEntity(stand)?.models?.containsKey(id) == true) {
            return true
        }

        val model = ModelEngineManager.createActiveModel(id) ?: return false
        val modeled = ModelEngineManager.createModeledEntity(stand) ?: return false
        modeled.addModel(model)
        modeled.isBaseEntityVisible = false
        return true
    }

    override fun play(stand: ArmorStand, animation: String) {
        for (model in ModelEngineManager.getModeledEntity(stand)?.models?.values ?: return) {
            val handler = model.animationHandler
            if (handler.isPlayingAnimation(animation)) {
                continue
            }
            handler.getAnimation(animation)?.let { handler.playAnimation(it, false) }
        }
    }

    override fun stop(stand: ArmorStand, animation: String) {
        for (model in ModelEngineManager.getModeledEntity(stand)?.models?.values ?: return) {
            model.animationHandler.stopAnimation(animation)
        }
    }
}
