package com.exanthiax.ecominions.minions.model

/** A custom model shown in place of a minion's stand, with the animation to loop for each state. */
data class MinionModel(
    val provider: Provider,
    val id: String,
    val idleAnimation: String?,
    val workingAnimation: String?,
    val fullAnimation: String?,
    val offlineAnimation: String?,
    val blockedAnimation: String?
) {
    enum class Provider(val configName: String) {
        MODEL_ENGINE("model-engine"),
        BETTER_MODEL("better-model")
    }
}
