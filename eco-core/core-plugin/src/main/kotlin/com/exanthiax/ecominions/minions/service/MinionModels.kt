package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.integration.model.MinionModelProvider
import com.exanthiax.ecominions.minions.model.MinionModel

/**
 * Puts a type's custom model on its stand and loops the animation for what the
 * minion is doing.
 *
 * A type whose model plugin is missing, or whose model id is unknown, keeps the
 * plain armour stand look.
 */
class MinionModels {
    private val providers = mutableMapOf<MinionModel.Provider, MinionModelProvider>()

    fun register(provider: MinionModelProvider) {
        providers[provider.provider] = provider
    }

    fun attach(minion: Minion) {
        val model = minion.state.definition.model ?: return
        val stand = minion.entity
        if (providers[model.provider]?.attach(stand, model.id) != true) {
            return
        }

        stand.isVisible = false
        stand.equipment.clear()
        minion.model = model
        minion.playingAnimation = null
    }

    /**
     * Attaches again while the type has a model but none is attached yet, as
     * minions loaded at startup register before the model plugin has its models.
     *
     * @return If [minion] is drawn by a model rather than its stand.
     */
    fun animate(minion: Minion, state: Minion.AnimationState): Boolean {
        if (minion.model == null) {
            attach(minion)
        }
        val model = minion.model ?: return false

        val next = when (state) {
            Minion.AnimationState.IDLE, Minion.AnimationState.BLOCKED -> model.idleAnimation
            Minion.AnimationState.MINING -> model.workingAnimation
            Minion.AnimationState.FULL -> model.fullAnimation
        }
        if (next != minion.playingAnimation) {
            val provider = providers.getValue(model.provider)
            minion.playingAnimation?.let { provider.stop(minion.entity, it) }
            next?.let { provider.play(minion.entity, it) }
            minion.playingAnimation = next
        }
        return true
    }
}
