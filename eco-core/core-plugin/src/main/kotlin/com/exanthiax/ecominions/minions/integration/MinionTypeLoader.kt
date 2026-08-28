package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.compactor.model.CompactorRecipeDefinition
import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.placeholders.LevelInjectable
import com.exanthiax.ecominions.core.registry.ConfigLoader
import com.exanthiax.ecominions.levelling.integration.LevellingLoader
import com.exanthiax.ecominions.minions.model.CraftingSpec
import com.exanthiax.ecominions.minions.model.GameData
import com.exanthiax.ecominions.minions.model.MinionEquipment
import com.exanthiax.ecominions.minions.model.MinionType
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.minions.model.SpawnItemSpec
import com.exanthiax.ecominions.minions.model.WoodFamily
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.placeholder.context.placeholderContext
import com.willfp.eco.util.NamespacedKeyUtils
import com.willfp.eco.util.evaluateExpression
import com.willfp.libreforge.Holder
import com.willfp.libreforge.SimpleHolder
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects
import org.bukkit.Material
import kotlin.math.roundToInt

/** Parses one file in `minions/` into a validated [MinionType]. */
class MinionTypeLoader(
    private val gameData: GameData,
    private val particles: ParticleSettingsLoader,
    private val levelling: LevellingLoader,
    /** Whether config.yml lets prices come from EcoShop, making sell-prices optional. */
    private val autoPricingEnabled: () -> Boolean,
    /** config.yml's ceiling on any type's declared slots. Read per load, so reloads apply. */
    private val hardSlotCap: () -> Int,
    /** Looks one entry out of the `compactor-recipes/` catalog. */
    private val compactorRecipe: (String) -> CompactorRecipeDefinition?,
    /** Every catalog id, for the "known: [...]" half of an unresolvable-id violation. */
    private val compactorRecipeIds: () -> Collection<String>
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

        val spawnItem = readSpawnItem(config)
        val compactorRecipes = readCompactorRecipes(config, context)
        val allowedUpgrades = config.getStrings("allowed-upgrades") + compactorRecipes.map { it.upgrade.id }
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
            equipment = readEquipment(config),
            particles = particles.load(config.getSubsection("particles"), context),
            placement = readPlacement(id, config, configured, targets, woodFamily, context),
            sellPrices = sellPrices,
            levelling = levelling.load(config, context),
            holder = compileHolder(id, config, context),
            compactorRecipes = compactorRecipes
        )
    }

    /** Resolves this type's `compactor-recipes:` ids; an unresolvable one is dropped, not fatal. */
    private fun readCompactorRecipes(config: Config, context: LoadContext): List<CompactorRecipeDefinition> =
        config.getStrings("compactor-recipes").mapNotNull { recipeId ->
            compactorRecipe(recipeId) ?: context.unknown("compactor-recipes", recipeId, compactorRecipeIds())
        }

    private fun compileHolder(id: String, config: Config, context: LoadContext): Holder? {
        val effectConfigs = config.getSubsections("effects")
        if (effectConfigs.isEmpty()) {
            return null
        }

        val violations = context.violationContext

        return SimpleHolder(
            NamespacedKeyUtils.create(NAMESPACE, "type_$id"),
            Effects.compile(effectConfigs, violations.with("effects")),
            Conditions.compile(config.getSubsections("conditions"), violations.with("conditions"))
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

    /**
     * `max-storage-slots`, like every other stat here, may be a `%level%`
     * expression - it is just rounded rather than truncated, and always clamped
     * to `minion.hard-slot-cap` on every read, in [MinionType.BaseStats.maxStorageSlotsAt].
     */
    private fun readBaseStats(config: Config): MinionType.BaseStats =
        MinionType.BaseStats(
            speedTicks = config.getString("base-stats.speed-ticks"),
            efficiencyTicks = config.getString("base-stats.efficiency-ticks"),
            radius = config.getString("base-stats.radius"),
            height = config.getStringOrNull("base-stats.height")?.takeIf { it.isNotBlank() }
                ?: MinionType.DEFAULT_HEIGHT,
            storageSlots = config.getString("base-stats.storage-slots"),
            maxStorageSlots = config.getString("base-stats.max-storage-slots"),
            regenDelayTicks = config.getString("base-stats.regen-delay-ticks"),
            hardSlotCap = hardSlotCap(),
            evaluate = { expression, level ->
                evaluateExpression(
                    expression,
                    placeholderContext(injectable = LevelInjectable(level))
                ).toInt()
            },
            evaluateRounded = { expression, level ->
                evaluateExpression(
                    expression,
                    placeholderContext(injectable = LevelInjectable(level))
                ).roundToInt()
            }
        )

    private fun readSpawnItem(config: Config): SpawnItemSpec =
        SpawnItemSpec(
            itemLookup = config.getStringOrNull("spawn-item.item") ?: DEFAULT_SPAWN_ITEM,
            displayName = config.getString("spawn-item.display-name"),
            lore = config.getStrings("spawn-item.lore")
        )

    private fun readCrafting(config: Config, context: LoadContext): CraftingSpec? {
        if (!config.getBool("spawn-item.crafting.enabled")) {
            return null
        }

        val recipe = CraftingSpec.normalise(config.getStrings("spawn-item.crafting.recipe"))
        if (recipe == null) {
            context.violation(
                "spawn-item.crafting.recipe",
                "a recipe needs between one and ${CraftingSpec.GRID_SIZE} entries, " +
                    "at least one of them an item"
            )
            return null
        }

        return CraftingSpec(
            recipe = recipe,
            shapeless = config.getBool("spawn-item.crafting.shapeless"),
            permission = config.getStringOrNull("spawn-item.crafting.permission")?.takeIf { it.isNotBlank() },
            giveAmount = (config.getIntOrNull("spawn-item.crafting.give-amount") ?: 1).coerceAtLeast(1)
        )
    }

    private fun readEquipment(config: Config): MinionEquipment {
        val section = config.getSubsection("equipment") ?: return MinionEquipment.EMPTY
        return MinionEquipment(
            helmet = section.getStringOrNull("helmet"),
            chestplate = section.getStringOrNull("chestplate"),
            leggings = section.getStringOrNull("leggings"),
            boots = section.getStringOrNull("boots"),
            hand = section.getStringOrNull("hand")
        )
    }

    /**
     * Lift-regen defaults from the block being farmed: trees want their sapling's
     * surface, crops want farmland, and anything else regenerates in place.
     *
     * Self-seeding is off unless asked for, except on tree types, which have
     * always planted their own saplings.
     */
    private fun readPlacement(
        id: String,
        config: Config,
        configured: List<Material>,
        targets: Set<Material>,
        woodFamily: WoodFamily?,
        context: LoadContext
    ): PlacementSettings {
        val section = config.getSubsection("placement")
        val regen = readRegenPlacement(id, section, configured, woodFamily, context)

        if (section.getBoolOrNull("self-seed") ?: (woodFamily != null)) {
            readSeedBlock(section, targets, woodFamily, context)
                ?.let { return regen.copy(selfSeed = true, seedBlock = it) }
        }
        return regen
    }

    private fun readRegenPlacement(
        id: String,
        section: Config?,
        configured: List<Material>,
        woodFamily: WoodFamily?,
        context: LoadContext
    ): PlacementSettings {
        if (section != null && section.getBoolOrNull("lift-regen") != null) {
            return readExplicitPlacement(id, section, context)
        }
        woodFamily?.defaultSurface?.let { return PlacementSettings(true, it) }
        gameData.anyCropSurfaceIn(configured)?.let { return PlacementSettings(true, it) }
        return PlacementSettings.NONE
    }

    /** What self-seeding places: whatever was asked for, else the block the minion mines. */
    private fun readSeedBlock(
        section: Config,
        targets: Set<Material>,
        woodFamily: WoodFamily?,
        context: LoadContext
    ): Material? {
        val raw = section.getStringOrNull("seed-block")
        if (!raw.isNullOrBlank()) {
            val material = Material.matchMaterial(raw.uppercase())
            if (material == null || !material.isBlock) {
                return context.invalid("placement.seed-block", raw, "a placeable block")
            }
            return material
        }

        val fallback = woodFamily?.sapling ?: targets.firstOrNull()
        if (fallback == null) {
            context.violation(
                "placement.self-seed",
                "self-seed is on but there is nothing to seed - set placement.seed-block"
            )
        }
        return fallback
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
        const val NAMESPACE = "ecominions"
    }
}
