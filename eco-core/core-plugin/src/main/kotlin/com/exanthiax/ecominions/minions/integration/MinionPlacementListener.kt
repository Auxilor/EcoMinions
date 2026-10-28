package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.islands.MinionAccess
import com.exanthiax.ecominions.libreforge.triggers.TriggerClickMinion
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.PlacementService
import com.exanthiax.ecominions.minions.ui.MainMenu
import com.exanthiax.ecominions.minions.ui.displayName
import com.exanthiax.ecominions.minions.ui.limitName
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.asAudience
import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.TriggerData
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
    private val messages: Messages,
    private val access: MinionAccess
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

            is PlacementService.PlaceResult.Protected ->
                player.sendMessage(messages.line("messages.cannot-place-here"))

            is PlacementService.PlaceResult.NoIslandPermission ->
                player.sendMessage(messages.line("messages.no-island-permission"))

            is PlacementService.PlaceResult.LimitReached ->
                player.sendMessage(
                    messages.line("messages.limit-reached-player", TemplateContext.of("limit" to result.limit))
                )

            is PlacementService.PlaceResult.IslandLimitReached ->
                player.sendMessage(
                    messages.line("messages.limit-reached-island", TemplateContext.of("limit" to result.limit))
                )

            is PlacementService.PlaceResult.TypeLimitReached ->
                player.sendMessage(
                    messages.line(
                        "messages.limit-reached-type",
                        TemplateContext.of("type" to result.type.displayName(), "limit" to result.limit)
                    )
                )

            is PlacementService.PlaceResult.MinionLimitReached ->
                player.sendMessage(
                    messages.line(
                        "messages.limit-reached-minion",
                        TemplateContext.of("minion_name" to result.definition.name, "limit" to result.limit)
                    )
                )

            is PlacementService.PlaceResult.Placed -> {
                item.amount -= 1
                celebrate(result.minion)
                player.sendMessage(messages.line("messages.placed"))
                result.limited?.let { breach ->
                    player.sendMessage(
                        messages.line(
                            "messages.placed-inactive",
                            TemplateContext.of("limit_name" to messages.limitName(breach.scope, state.definition))
                        )
                    )
                }
            }

            is PlacementService.PlaceResult.Cancelled -> Unit
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractAtEntityEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }
        if (event.player.inventory.itemInMainHand.isNamedNameTag()) {
            return
        }
        val stand = event.rightClicked as? ArmorStand ?: return
        val minion = minions.getByEntity(stand) ?: return

        if (event.player.isSneaking) {
            clickIfAllowed(event, stand, minion)
            event.isCancelled = true
            return
        }
        event.isCancelled = true
        sendSummary(event.player, minion)
        openIfAllowed(event.player, minion)
    }

    /**
     * Shift-right-click fires [TriggerClickMinion] for the player instead of opening the menu.
     *
     * Dispatched before the event is cancelled, as libreforge skips effects on a cancelled event.
     */
    private fun clickIfAllowed(event: PlayerInteractAtEntityEvent, stand: ArmorStand, minion: Minion) {
        val player = event.player
        if (!access.canUse(player, minion)) {
            player.sendMessage(messages.useDenial(access, player, minion))
            return
        }
        TriggerClickMinion.dispatch(
            player.toDispatcher(),
            TriggerData(
                player = player,
                victim = stand,
                location = minion.location,
                event = event
            )
        )
    }

    /** Minions are indestructible; hitting one opens it instead. */
    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val stand = event.entity as? ArmorStand ?: return
        val minion = minions.getByEntity(stand) ?: return

        event.isCancelled = true
        (event.damager as? Player)?.let { openIfAllowed(it, minion) }
    }

    fun openIfAllowed(player: Player, minion: Minion) {
        if (!access.canUse(player, minion)) {
            player.sendMessage(messages.useDenial(access, player, minion))
            return
        }
        mainMenu.open(player, minion)
    }

    private fun celebrate(minion: Minion) {
        val location = minion.entity.location
        val world = location.world ?: return
        world.spawnParticle(Particle.HAPPY_VILLAGER, location, PARTICLE_COUNT, 0.4, 0.6, 0.4, 0.0)
        world.playSound(location, Sound.ENTITY_ARMOR_STAND_PLACE, 1.0f, 1.0f)
    }

    private fun sendSummary(player: Player, minion: Minion) {
        player.asAudience().sendActionBar(
            StringUtils.toComponent(
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
