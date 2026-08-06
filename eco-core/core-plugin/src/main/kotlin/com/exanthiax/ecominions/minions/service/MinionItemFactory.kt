package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.placeholders.TemplateRenderer
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionType
import com.exanthiax.ecominions.minions.model.SpawnItemSpec
import com.willfp.eco.core.items.Items
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.profile.PlayerProfile
import java.net.URI
import java.util.Base64
import java.util.UUID

/** Builds and reads the placeable item that carries a minion between worlds. */
class MinionItemFactory(
    private val repository: MinionStateRepository,
    private val renderer: TemplateRenderer
) {
    fun build(state: MinionState): ItemStack {
        val type = state.type
        val item = when (type.spawnItem.mode) {
            SpawnItemSpec.Mode.HEAD -> buildSkull(type.spawnItem.texture)
            SpawnItemSpec.Mode.ITEM -> Items.lookup(type.spawnItem.itemLookup).item.clone()
        }

        val meta = item.itemMeta ?: return item
        val context = TemplateContext.of(
            "level" to state.level,
            "minion_name" to type.name
        )

        meta.setDisplayName(renderer.render(type.spawnItem.displayName, context))
        meta.lore = renderer.render(type.spawnItem.lore, context)
        repository.write(meta.persistentDataContainer, state)

        item.itemMeta = meta
        return item
    }

    /** @return The state stored on [item], or null if it is not a minion item. */
    fun read(item: ItemStack): MinionState? =
        item.itemMeta?.let { repository.read(it.persistentDataContainer) }

    fun isMinionItem(item: ItemStack?): Boolean =
        item?.itemMeta?.let { repository.isMinion(it.persistentDataContainer) } ?: false

    /** The head shown on a stand that has no configured helmet. */
    fun headFor(type: MinionType): ItemStack = build(MinionState(type = type))

    private fun buildSkull(texture: String): ItemStack {
        val skull = ItemStack(Material.PLAYER_HEAD)
        if (texture.isBlank()) {
            return skull
        }
        val meta = skull.itemMeta as? SkullMeta ?: return skull

        runCatching {
            val profile: PlayerProfile = Bukkit.createPlayerProfile(
                UUID.nameUUIDFromBytes(texture.toByteArray()),
                SKULL_PROFILE_NAME
            )
            skinUrlOf(texture)?.let { url ->
                val textures = profile.textures
                textures.skin = URI.create(url).toURL()
                profile.setTextures(textures)
            }
            meta.ownerProfile = profile
        }

        skull.itemMeta = meta
        return skull
    }

    /** Textures are base64 JSON; the skin URL is the only field we need. */
    private fun skinUrlOf(texture: String): String? {
        val decoded = runCatching { String(Base64.getDecoder().decode(texture)) }.getOrNull() ?: return null
        val start = decoded.indexOf("http")
        if (start < 0) {
            return null
        }
        val end = decoded.indexOfAny(charArrayOf('"', '}'), start)
        return if (end > start) decoded.substring(start, end) else null
    }

    private companion object {
        const val SKULL_PROFILE_NAME = "minion"
    }
}
