package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.minions.model.ExperienceMode
import com.exanthiax.ecominions.minions.model.MinionSettings

/** The one place a minion's earned XP goes, so every type honours `minion.xp.mode`. */
class ExperienceSink(
    private val settings: SettingsHolder<MinionSettings>,
    private val spawnOrb: (Minion, Int) -> Unit
) {
    fun add(minion: Minion, amount: Int) {
        if (amount <= 0) {
            return
        }
        if (settings.value.experienceMode == ExperienceMode.ORB) {
            spawnOrb(minion, amount)
            return
        }
        store(minion, amount)
    }

    /** Stores whatever the mode, for XP the minion picked up rather than earned. */
    fun store(minion: Minion, amount: Int) {
        if (amount <= 0) {
            return
        }
        minion.state = minion.state.withStoredExperience(minion.state.storedExperience + amount)
    }
}
