package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.registry.ConfigLoader
import com.exanthiax.ecominions.minions.model.CraftingSpec
import com.exanthiax.ecominions.minions.model.GameData
import com.exanthiax.ecominions.minions.model.MinionArmor
import com.exanthiax.ecominions.minions.model.MinionArmorSlot
import com.exanthiax.ecominions.minions.model.MinionType
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.minions.model.SpawnItemSpec
import com.exanthiax.ecominions.minions.model.WoodFamily
import com.willfp.eco.core.config.interfaces.Config
import org.bukkit.Color
import org.bukkit.Material

/** Parses one file in `minions/` into a validated [MinionType]. */
class MinionTypeLoader(
    private val gameData: GameData,
    private val particles: ParticleSettingsLoader,
    /** Whether config.yml lets prices come from EcoShop, making sell-prices optional. */
    private val autoPricingEnabled: () -> Boolean
) : ConfigLoader<MinionType> {

    override fun load(id: String, config: Config, context: LoadContext): MinionType? {
        val configured = readTargetBlocks(config, context)
        if (configured.isEmpty()) {
            context.violation("target-blocks", "no valid blocks - the minion would never mine")
            return null
        }

        val woodFamily = gameData.woodFamilies.detect(configured)
        val targets = resolveTargets(configured, woodFamily)
        if (targets.isEmpty()) {
            context.violation("target-blocks", "every configured block is an invalid mining target")
            return null
        }

        val spawnItem = readSpawnItem(config, context) ?: return null
        val allowedUpgrades = config.getStrings("allowed-upgrades")
        val sellPrices = readSellPrices(config, context)

        if (AUTO_SELL in allowedUpgrades && sellPrices.isEmpty() && !autoPricingEnabled()) {
            context.violation(
                "sell-prices",
                "'$AUTO_SELL' is allowed but no prices are configured, and sell-pricing." +
                    "auto-from-ecoshop is off - auto sell would pay nothing"
            )
        }

        return MinionType(
            id = id,
            name = config.getFormattedString("name"),
            targetBlocks = targets,
            allowedUpgradeIds = allowedUpgrades,
            woodFamily = woodFamily,
            baseStats = readBaseStats(config),
            spawnItem = spawnItem,
            crafting = readCrafting(config, context),
            armor = readArmor(config, context),
            particles = particles.load(config.getSubsection("particles"), context),
            placement = readPlacement(id, config, configured, woodFamily, context),
            sellPrices = sellPrices
        )
    }

    private fun readTargetBlocks(config: Config, context: LoadContext): List<Material> =
        config.getStrings("target-blocks").mapNotNull { name ->
            Material.matchMaterial(name)
                ?: context.invalid("target-blocks", name, "a material on this server version")
        }

    /** Tree types mine only logs; everything else promotes seeds to their crop. */
    private fun resolveTargets(configured: List<Material>, woodFamily: WoodFamily?): Set<Material> {
        if (woodFamily != null) {
            return setOf(woodFamily.log)
        }
        return configured
            .map { gameData.growthRules.promote(it) }
            .filter { gameData.growthRules.isValidTarget(it) }
            .toSet()
    }

    private fun readBaseStats(config: Config) = MinionType.BaseStats(
        speedTicks = config.getInt("base-stats.speed-ticks").coerceAtLeast(1),
        efficiencyTicks = config.getInt("base-stats.efficiency-ticks").coerceAtLeast(0),
        radius = config.getInt("base-stats.radius").coerceAtLeast(1),
        inventoryRows = config.getInt("base-stats.inventory-rows").coerceIn(1, MAX_ROWS),
        regenDelayTicks = config.getInt("base-stats.regen-delay-ticks").coerceAtLeast(1)
    )

    private fun readSpawnItem(config: Config, context: LoadContext): SpawnItemSpec? {
        val rawMode = config.getString("spawn-item.mode").uppercase()
        val mode = SpawnItemSpec.Mode.entries.firstOrNull { it.name == rawMode }
            ?: return context.unknown(
                "spawn-item.mode",
                rawMode,
                SpawnItemSpec.Mode.entries.map { it.name }
            )

        return SpawnItemSpec(
            mode = mode,
            texture = config.getStringOrNull("spawn-item.texture") ?: "",
            itemLookup = config.getStringOrNull("spawn-item.item") ?: DEFAULT_SPAWN_ITEM,
            displayName = config.getString("spawn-item.display-name"),
            lore = config.getStrings("spawn-item.lore")
        )
    }

    private fun readCrafting(config: Config, context: LoadContext): CraftingSpec? {
        if (!config.getBool("spawn-item.crafting.enabled")) {
            return null
        }

        val shape = config.getStrings("spawn-item.crafting.shape")
        if (shape.size != CRAFTING_ROWS) {
            context.violation(
                "spawn-item.crafting.shape",
                "a shaped recipe needs exactly $CRAFTING_ROWS rows, found ${shape.size}"
            )
            return null
        }

        val section = config.getSubsection("spawn-item.crafting.ingredients")
        val ingredients = section.getKeys(false)
            .filter { it.length == 1 }
            .associate { it[0] to section.getString(it) }

        if (ingredients.isEmpty()) {
            context.violation("spawn-item.crafting.ingredients", "no ingredients - the recipe cannot be crafted")
            return null
        }

        return CraftingSpec(
            shape = shape,
            ingredients = ingredients,
            requiresCraftingTable = config.getBool("spawn-item.crafting.requires-crafting-table"),
            permission = config.getString("spawn-item.crafting.permission")
        )
    }

    private fun readArmor(config: Config, context: LoadContext): MinionArmor {
        val section = config.getSubsection("armor") ?: return MinionArmor.EMPTY
        return MinionArmor(
            helmet = readArmorSlot(section, "helmet", context),
            chestplate = readArmorSlot(section, "chestplate", context),
            leggings = readArmorSlot(section, "leggings", context),
            boots = readArmorSlot(section, "boots", context)
        )
    }

    private fun readArmorSlot(section: Config, key: String, context: LoadContext): MinionArmorSlot? {
        val slot = section.getSubsection(key) ?: return null
        val material = Material.matchMaterial(slot.getString("material"))
            ?: return context.invalid("armor.$key.material", slot.getString("material"), "a material")

        val red = slot.getIntOrNull("color.r")
        val green = slot.getIntOrNull("color.g")
        val blue = slot.getIntOrNull("color.b")
        val color = if (red != null && green != null && blue != null) {
            Color.fromRGB(red, green, blue)
        } else {
            null
        }
        return MinionArmorSlot(material, color)
    }

    /**
     * Lift-regen defaults from the block being farmed: trees want their sapling's
     * surface, crops want farmland, and anything else regenerates in place.
     */
    private fun readPlacement(
        id: String,
        config: Config,
        configured: List<Material>,
        woodFamily: WoodFamily?,
        context: LoadContext
    ): PlacementSettings {
        val section = config.getSubsection("placement")
        if (section != null && section.getBoolOrNull("lift-regen") != null) {
            return readExplicitPlacement(id, section, context)
        }
        woodFamily?.defaultSurface?.let { return PlacementSettings(true, it) }
        gameData.anyCropSurfaceIn(configured)?.let { return PlacementSettings(true, it) }
        return PlacementSettings.NONE
    }

    private fun readExplicitPlacement(id: String, section: Config, context: LoadContext): PlacementSettings {
        if (!section.getBool("lift-regen")) {
            return PlacementSettings.NONE
        }

        val raw = section.getStringOrNull("surface-block")
        if (raw.isNullOrBlank()) {
            context.violation("placement.surface-block", "lift-regen is on but no surface block is set")
            return PlacementSettings.NONE
        }

        val material = Material.matchMaterial(raw.uppercase())
        if (material == null || !material.isBlock) {
            context.violation("placement.surface-block", "'$raw' is not a placeable block")
            return PlacementSettings.NONE
        }
        return PlacementSettings(true, material)
    }

    private fun readSellPrices(config: Config, context: LoadContext): Map<Material, Double> {
        val section = config.getSubsection("sell-prices")
        return section.getKeys(false).mapNotNull { key ->
            val material = Material.matchMaterial(key)
                ?: return@mapNotNull context.invalid<Pair<Material, Double>>("sell-prices", key, "a material")
            material to section.getDouble(key)
        }.toMap()
    }

    private companion object {
        const val AUTO_SELL = "auto_sell"
        const val DEFAULT_SPAWN_ITEM = "ARMOR_STAND"
        const val MAX_ROWS = 6
        const val CRAFTING_ROWS = 3
    }
}
