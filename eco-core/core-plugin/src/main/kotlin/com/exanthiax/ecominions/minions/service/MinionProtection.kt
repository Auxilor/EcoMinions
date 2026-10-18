package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.libreforge.ownerPlayer
import com.willfp.eco.core.integrations.antigrief.AntigriefManager
import org.bukkit.block.Block
import org.bukkit.entity.Player

/**
 * Whether a minion may work a block: its owner must be able to build there,
 * unless its type bypasses antigrief.
 *
 * Protection plugins are slow to ask and a work area is scanned every cycle,
 * so answers are cached per minion and asked again after [RECHECK_MILLIS], in
 * case a claim beside the minion changed.
 */
class MinionProtection(
    private val ownerOf: (Minion) -> Player? = { it.ownerPlayer() },
    private val canBuild: (Player, Block) -> Boolean = { player, block ->
        AntigriefManager.canBreakBlock(player, block) && AntigriefManager.canPlaceBlock(player, block)
    },
    private val clock: () -> Long = System::currentTimeMillis
) {
    fun allows(minion: Minion, block: Block): Boolean {
        if (minion.state.definition.bypassAntigrief) {
            return true
        }
        val mining = minion.mining
        val now = clock()
        if (now - mining.protectionCheckedAt >= RECHECK_MILLIS) {
            mining.protectedBlocks.clear()
            mining.protectionCheckedAt = now
        }
        val position = BlockPositions.pack(block.location)
        mining.protectedBlocks[position]?.let { return it }
        val owner = ownerOf(minion) ?: return true
        return canBuild(owner, block).also { mining.protectedBlocks[position] = it }
    }

    private companion object {
        const val RECHECK_MILLIS = 60_000L
    }
}
