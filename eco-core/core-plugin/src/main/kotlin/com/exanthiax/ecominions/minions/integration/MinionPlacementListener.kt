package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.PlacementService
import com.exanthiax.ecominions.minions.ui.MainMenu
import com.willfp.eco.util.asAudience
import net.kyori.adventure.text.Component
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/** Turns clicks into placements and menu opens. */
class MinionPlacementListener(
    private val minions: MinionService,
    private val placement: PlacementService,
    private val itemFactory: MinionItemFactory,
    private val mainMenu: MainMenu,
    private val messages: Messages
) : Listener {

    @EventHandler
    fun onPlace(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) {
            return
        }
        val item = event.item ?: return
        if (!itemFactory.isMinionItem(item)) {
            return
        }
        val block = event.clickedBlock ?: return

        event.isCancelled = true
        val state = itemFactory.read(item) ?: return
        val player = event.player
        val location = block.location.clone().add(CENTRE, 1.0, CENTRE)

        when (val result = placement.place(player, state, location)) {
            is PlacementService.PlaceResult.WorldDisabled ->
                player.sendMessage(messages.line("messages.world-disabled"))

            is PlacementService.PlaceResult.LimitReached ->
                player.sendMessage(
                    messages.line("messages.limit-reached-player", TemplateContext.of("limit" to result.limit))
                )

            is PlacementService.PlaceResult.Placed -> {
                item.amount -= 1
                celebrate(result.minion)
                player.sendMessage(messages.line("messages.placed"))
            }
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractAtEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }
        val stand = event.rightClicked as? ArmorStand ?: return
        val minion = minions.getByEntity(stand) ?: return

        event.isCancelled = true
        sendSummary(event.player, minion)
        mainMenu.open(event.player, minion)
    }

    /** Minions are indestructible; hitting one opens it instead. */
    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val stand = event.entity as? ArmorStand ?: return
        val minion = minions.getByEntity(stand) ?: return

        event.isCancelled = true
        (event.damager as? Player)?.let { mainMenu.open(it, minion) }
    }

    private fun celebrate(minion: Minion) {
        val location = minion.entity.location
        val world = location.world ?: return
        world.spawnParticle(Particle.HAPPY_VILLAGER, location, PARTICLE_COUNT, 0.4, 0.6, 0.4, 0.0)
        world.playSound(location, Sound.ENTITY_ARMOR_STAND_PLACE, 1.0f, 1.0f)
    }

    private fun sendSummary(player: Player, minion: Minion) {
        player.asAudience().sendActionBar(
            Component.text(
                messages.line(
                    "action-bar.interact",
                    TemplateContext.of(MinionPlaceholders.MINION to minion)
                )
            )
        )
    }

    private companion object {
        const val CENTRE = 0.5
        const val PARTICLE_COUNT = 12
    }
}
