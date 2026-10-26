package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.islands.MinionAccess
import com.exanthiax.ecominions.minions.model.MinionSettings
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionService
import com.willfp.eco.util.StringUtils
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

/** Renames a minion when it is right-clicked with a named name tag. */
class MinionRenameListener(
    private val minions: MinionService,
    private val access: MinionAccess,
    private val settings: SettingsHolder<MinionSettings>,
    private val messages: Messages
) : Listener {

    @EventHandler
    fun onRename(event: PlayerInteractAtEntityEvent) {
        val item = event.player.inventory.itemInMainHand
        val minion = (event.rightClicked as? ArmorStand)
            ?.takeIf { event.hand == EquipmentSlot.HAND && item.isNamedNameTag() }
            ?.let { minions.getByEntity(it) }
            ?: return

        event.isCancelled = true
        rename(event.player, minion, item)?.let(event.player::sendMessage)
    }

    /** Stops vanilla naming the stand itself, which it does on the plain interact that follows. */
    @EventHandler
    fun onVanillaNameTag(event: PlayerInteractEntityEvent) {
        val stand = event.rightClicked as? ArmorStand ?: return
        if (event.player.inventory.getItem(event.hand).isNamedNameTag() && minions.getByEntity(stand) != null) {
            event.isCancelled = true
        }
    }

    private fun rename(player: Player, minion: Minion, item: ItemStack): String? {
        val formatted = StringUtils.format(item.itemMeta.displayName.replace(oldValue = "%", newValue = ""))
        val plain = PlainTextComponentSerializer.plainText().serialize(StringUtils.toComponent(formatted)).trim()
        val maxLength = settings.value.renameMaxLength

        return when {
            !player.hasPermission(RENAME_PERMISSION) -> messages.line("messages.rename-no-permission")
            !access.canUse(player, minion) -> messages.useDenial(access, player, minion)
            plain.isEmpty() -> null
            plain.length > maxLength -> messages.line(
                "messages.rename-too-long",
                TemplateContext.of("max" to maxLength)
            )
            player.hasPermission(RENAME_COLOR_PERMISSION) -> applyName(player, minion, item, formatted)
            else -> applyName(player, minion, item, plain)
        }
    }

    private fun applyName(player: Player, minion: Minion, item: ItemStack, name: String): String {
        minions.updateState(minion, minion.state.copy(customName = name))
        if (player.gameMode != GameMode.CREATIVE) {
            item.amount -= 1
        }
        return messages.line("messages.rename-success", TemplateContext.of(MinionPlaceholders.MINION to minion))
    }

    companion object {
        const val RENAME_PERMISSION = "ecominions.rename"
        const val RENAME_COLOR_PERMISSION = "ecominions.rename.color"
    }
}

/** Whether this is a name tag that was given a name in an anvil. */
fun ItemStack?.isNamedNameTag(): Boolean =
    this?.type == Material.NAME_TAG && itemMeta?.hasDisplayName() == true
