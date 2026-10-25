package com.exanthiax.ecominions.minions.ui

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.integration.MinionPlaceholders
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionActivityService
import com.willfp.eco.core.Prerequisite
import com.willfp.eco.util.StringUtils
import net.kyori.adventure.text.Component
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay

/** The floating label above a minion. */
class MinionHologram(
    private val messages: Messages,
    private val settings: SettingsHolder<MinionSettings>,
    private val activity: MinionActivityService
) {
    /**
     * Folia has no synchronous teleport, so the label is moved with `teleportAsync` there.
     *
     * A label removed with its chunk is spawned again rather than moved, or the
     * minion would carry a dead label and show none.
     */
    fun refresh(minion: Minion) {
        val display = minion.hologram?.takeIf { it.isValid } ?: spawn(minion) ?: return
        if (Prerequisite.HAS_FOLIA.isMet) {
            display.teleportAsync(anchorOf(minion))
        } else {
            display.teleport(anchorOf(minion))
        }
        display.text(StringUtils.toComponent(textFor(minion)))
    }

    fun remove(minion: Minion) {
        minion.hologram?.remove()
        minion.hologram = null
    }

    private fun spawn(minion: Minion): TextDisplay? {
        if (minion.entity.isDead) {
            return null
        }
        val display = minion.entity.world.spawn(anchorOf(minion), TextDisplay::class.java) {
            // Never looked up by identity - always re-spawned from minion.hologram in
            // memory. Persisting it just leaves an orphan behind on an ungraceful
            // stop (crash, kill), which stacks up over repeated restarts.
            it.isPersistent = false
            it.billboard = Display.Billboard.CENTER
            it.text(Component.empty())
        }
        minion.hologram = display
        return display
    }

    private fun anchorOf(minion: Minion) =
        minion.entity.location.clone().add(0.0, settings.value.hologramOffset, 0.0)

    private fun textFor(minion: Minion): String {
        val context = TemplateContext.of(MinionPlaceholders.MINION to minion)
        val lines = mutableListOf(
            messages.line("hologram.line-1", context),
            messages.line("hologram.line-2", context)
        )
        generateSequence(3) { it + 1 }
            .map { "hologram.line-$it" }
            .takeWhile { messages.has(it) }
            .forEach { lines += messages.line(it, context) }

        if (activity.isLimited(minion)) {
            lines += messages.line("hologram.limited", context)
            return lines.joinToString("\n")
        }

        if (!activity.isActive(minion)) {
            lines += messages.line("hologram.idle", context)
            return lines.joinToString("\n")
        }

        when {
            minion.animation == Minion.AnimationState.FULL -> lines += messages.line("hologram.full", context)
            minion.animation == Minion.AnimationState.BLOCKED || minion.blockedReason != Minion.NO_TARGET ->
                lines += messages.line(statusPath(minion), context)
        }

        return lines.joinToString("\n")
    }

    /** A reason with no lang line falls back to the original "No Blocks to Mine" text. */
    private fun statusPath(minion: Minion): String =
        "hologram.${minion.blockedReason}".takeIf { messages.has(it) } ?: "hologram.no-target"
}
