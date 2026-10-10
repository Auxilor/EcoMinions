package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.compactor.model.CompactorRecipeDefinition
import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.placeholders.LevelInjectable
import com.exanthiax.ecominions.core.registry.ConfigLoader
import com.exanthiax.ecominions.core.world.BlockLookup
import com.exanthiax.ecominions.core.world.ItemLookup
import com.exanthiax.ecominions.levelling.integration.LevellingLoader
import com.exanthiax.ecominions.minions.model.CraftingSpec
import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.MinionEquipment
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.model.PlacementSettings
import com.exanthiax.ecominions.minions.model.PriceList
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.minions.model.SpawnItemSpec
import com.exanthiax.ecominions.minions.model.TargetSet
import com.willfp.eco.core.blocks.TestableBlock
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.placeholder.context.placeholderContext
import com.willfp.eco.util.NamespacedKeyUtils
import com.willfp.eco.util.evaluateExpression
import com.willfp.eco.util.evaluateExpressionOrNull
import com.willfp.libreforge.Holder
import com.willfp.libreforge.SimpleHolder
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.Effects
import org.bukkit.Material
import kotlin.math.roundToInt

/** Parses one file in `minions/` into a validated [MinionDefinition]. */
class MinionDefinitionLoader(
    /** Resolves a configured id to a block on this server, vanilla or custom. */
    private val blocks: BlockLookup,
    /**
     * Looks one entry out of the `blocks/` catalog **by its block id**, not by
     * its file name: `target-blocks` names `OAK_LOG`, while the file that
     * defines it is `oak.yml`.
     */
    private val blockDefinition: (String) -> MinionBlock?,
    /** Resolves a `sell-prices:` key to an item on this server, vanilla or custom. */
    private val items: ItemLookup,
    private val particles: ParticleSettingsLoader,
    private val levelling: LevellingLoader,
    /** Whether config.yml lets prices come from EcoShop, making sell-prices optional. */
    private val autoPricingEnabled: () -> Boolean,
    /** config.yml's ceiling on any type's declared slots. Read per load, so reloads apply. */
    private val hardSlotCap: () -> Int,
    /** Looks one entry out of the `compactor-recipes/` catalog. */
    private val compactorRecipe: (String) -> CompactorRecipeDefinition?,
    /** Every catalog id, for the "known: [...]" half of an unresolvable-id violation. */
    private val compactorRecipeIds: () -> Collection<String>,
    /** Every upgrade id, so `allowed-upgrades` can reject one that does not exist. */
    private val upgradeIds: () -> Collection<String>
) : ConfigLoader<MinionDefinition> {

    override fun load(id: String, config: Config, context: LoadContext): MinionDefinition? {
        val type = MinionType.parse(config.getStringOrNull("type"))
        if (type == null) {
            context.violation(
                "type",
                "must be one of ${MinionType.entries.joinToString(", ") { it.id }}"
            )
            return null
        }

        val configured = readTargetBlocks(config)
        if (configured.isEmpty()) {
            context.violation("target-blocks", "no valid blocks - the minion would never mine")
            return null
        }

        val targets = resolveTargets(configured, context)
        if (targets.isEmpty) {
            context.violation("target-blocks", "every configured block is an invalid mining target")
            return null
        }
        if (!rejectUnfellableTargets(type, targets, context)) {
            return null
        }

        val levelling = levelling.load(config, context)
        val statsValid = validateBaseStats(config, levelling.maxLevel, context)

        val spawnItem = readSpawnItem(config)
        val compactorRecipes = readCompactorRecipes(config, context)
        val allowedUpgrades = readAllowedUpgrades(config, context) + compactorRecipes.map { it.upgrade.id }
        val sellPrices = readSellPrices(config, context)

        if (AUTO_SELL in allowedUpgrades && sellPrices.isEmpty && !autoPricingEnabled()) {
            context.violation(
                "sell-prices",
                "'$AUTO_SELL' is allowed but no prices are configured, and sell-pricing." +
                    "auto-from-ecoshop is off - auto sell would pay nothing"
            )
        }

        val placement = readPlacement(id, config, targets, type, context)
        if (!statsValid) {
            return null
        }

        return MinionDefinition(
            id = id,
            name = config.getFormattedString("name"),
            targetBlocks = targets,
            allowedUpgradeIds = allowedUpgrades,
            type = type,
            baseStats = readBaseStats(config, type),
            spawnItem = spawnItem,
            crafting = readCrafting(config, context),
            equipment = readEquipment(config),
            particles = particles.load(config.getSubsection("particles"), context),
            placement = placement,
            prices = sellPrices,
            levelling = levelling,
            holder = compileHolder(id, config, context),
            compactorRecipes = compactorRecipes
        )
    }

    /** An id that names no upgrade is dropped with a violation, as it could never be bought. */
    private fun readAllowedUpgrades(config: Config, context: LoadContext): List<String> {
        val known = upgradeIds()
        return config.getStrings("allowed-upgrades").filter { upgradeId ->
            upgradeId in known || context.unknown<String>("allowed-upgrades", upgradeId, known) != null
        }
    }

    /**
     * Checks every `base-stats` value at level 1 and at the type's top level.
     *
     * A value that does not evaluate rejects the type: it would read as 0, and a
     * speed of 0 is clamped to mining every tick. A value out of range only warns,
     * because reads clamp it as they always have. Every value is checked before
     * rejecting, so one reload reports every problem in the file.
     */
    private fun validateBaseStats(config: Config, maxLevel: Int, context: LoadContext): Boolean {
        val stats = context.with("base-stats")
        val levels = setOf(1, maxLevel)
        val evaluated = BASE_STAT_MINIMUMS.mapValues { (key, _) ->
            val raw = config.getStringOrNull("base-stats.$key")?.takeIf { it.isNotBlank() }
            if (raw == null) {
                if (key != "height") {
                    stats.violation(key, "is not set")
                }
                return@mapValues emptyList()
            }
            levels.map { level ->
                evaluateExpressionOrNull(raw, placeholderContext(injectable = LevelInjectable(level)))
                    ?: return@mapValues stats.invalid<List<Double>>(key, raw, "a number or %level% expression")
            }
        }
        for ((key, values) in evaluated) {
            val minimum = BASE_STAT_MINIMUMS.getValue(key)
            values.orEmpty().firstOrNull { it < minimum }?.let {
                stats.violation(key, "evaluates to ${it.toInt()}, below the minimum of $minimum - it is raised to $minimum")
            }
        }

        val slots = evaluated.getValue("storage-slots").orEmpty()
        val maxSlots = evaluated.getValue("max-storage-slots").orEmpty()
        slots.zip(maxSlots).firstOrNull { (current, max) -> current > max }?.let { (current, max) ->
            stats.violation(
                "storage-slots",
                "${current.toInt()} is above max-storage-slots (${max.toInt()}) - it is lowered to ${max.toInt()}"
            )
        }
        maxSlots.firstOrNull { it > hardSlotCap() }?.let {
            stats.violation(
                "max-storage-slots",
                "${it.toInt()} is above minion.hard-slot-cap (${hardSlotCap()}) - it is lowered to ${hardSlotCap()}"
            )
        }
        return evaluated.values.none { it == null }
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

    /**
     * @return false if this is a woodcutter aimed at something that is not a tree.
     *
     * Fatal rather than a warning, because a woodcutter clears what it breaks
     * instead of regenerating it. Loading one anyway would let it strip a block
     * it can never replant - it would eat a player's build and then sit blocked.
     */
    private fun rejectUnfellableTargets(
        type: MinionType,
        targets: TargetSet,
        context: LoadContext
    ): Boolean {
        if (type != MinionType.WOODCUTTER) {
            return true
        }

        val unfellable = targets.definitions.filter { it.plant == null || it.leaves == null }
        if (unfellable.isEmpty()) {
            return true
        }

        context.violation(
            "target-blocks",
            unfellable.joinToString(", ") { it.lookup } +
                " is not a tree - a woodcutter fells what it breaks and never puts it back, so it " +
                "would destroy this permanently. Give it a 'plant' and 'leaves' in blocks/, or use " +
                "type: miner."
        )
        return false
    }

    /** Ids as authored - eco resolves them, so a custom block id is as valid as a material. */
    private fun readTargetBlocks(config: Config): List<String> =
        config.getStrings("target-blocks").filter { it.isNotBlank() }

    /**
     * Every configured id, resolved to its `blocks/` definition.
     *
     * An id with no definition file gets a plain regen default rather than
     * being dropped, which is the behaviour `blocks/_example.yml` has always
     * promised. A definition that promotes - a seed to its crop - is followed
     * once, and one that is not mineable is dropped with a violation, so a
     * config asking a minion to farm dripstone says why it will not.
     */
    private fun resolveTargets(configured: List<String>, context: LoadContext): TargetSet {
        val resolved = configured.mapNotNull { id ->
            val definition = resolve(id, context) ?: return@mapNotNull null
            val promoted = definition.promotesTo?.let { resolve(it, context) } ?: definition

            if (!promoted.mineable) {
                return@mapNotNull context.invalid<MinionBlock>(
                    "target-blocks", id, "a block a minion can mine"
                )
            }
            promoted
        }
        return TargetSet(resolved.distinctBy { it.lookup })
    }

    /** @return [id]'s definition, or a plain regen default if no file defines it. */
    private fun resolve(id: String, context: LoadContext): MinionBlock? =
        blockDefinition(id)
            ?: blocks.lookup(id)?.let {
                MinionBlock.regenDefault(id = id.lowercase(), lookup = id, block = it)
                    .copy(vanillaMaterial = Material.matchMaterial(id)?.takeIf { m -> m.isBlock })
            }
            ?: context.invalid("target-blocks", id, "a block or custom block id")

    /**
     * `max-storage-slots`, like every other stat here, may be a `%level%`
     * expression - it is just rounded rather than truncated, and always clamped
     * to `minion.hard-slot-cap` on every read, in [MinionDefinition.BaseStats.maxStorageSlotsAt].
     */
    private fun readBaseStats(config: Config, type: MinionType): MinionDefinition.BaseStats =
        MinionDefinition.BaseStats(
            speedTicks = config.getString("base-stats.speed-ticks"),
            efficiencyTicks = config.getString("base-stats.efficiency-ticks"),
            radius = config.getString("base-stats.radius"),
            height = config.getStringOrNull("base-stats.height")?.takeIf { it.isNotBlank() }
                ?: MinionDefinition.defaultHeight(type),
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
     * Lift-regen and self-seeding defaults, taken from the blocks being farmed.
     *
     * A target that declares a `surface` in `blocks/` wants to be lifted onto
     * it - that is how a crop lands on farmland without anyone configuring it.
     * Self-seeding is off unless asked for, except on woodcutters, which have
     * always planted their own saplings.
     */
    private fun readPlacement(
        id: String,
        config: Config,
        targets: TargetSet,
        type: MinionType,
        context: LoadContext
    ): PlacementSettings {
        val section = config.getSubsection("placement")
        val regen = readRegenPlacement(id, section, targets, context)
        val spacing = readSpacing(section, context)

        if (section.getBoolOrNull("self-seed") ?: (type == MinionType.WOODCUTTER)) {
            readSeedBlocks(section, targets, type, context)
                .takeIf { it.isNotEmpty() }
                ?.let { return regen.copy(selfSeed = true, seedBlocks = it, spacing = spacing) }
        }
        return regen
    }

    /** Empty blocks between self-seeded plants; 0 packs them side by side. */
    private fun readSpacing(section: Config, context: LoadContext): Int {
        val spacing = section.getIntOrNull("spacing") ?: return 0
        if (spacing < 0) {
            return context.invalid<Int>("placement.spacing", spacing.toString(), "0 or more") ?: 0
        }
        return spacing
    }

    private fun readRegenPlacement(
        id: String,
        section: Config?,
        targets: TargetSet,
        context: LoadContext
    ): PlacementSettings {
        if (section != null && section.getBoolOrNull("lift-regen") != null) {
            return readExplicitPlacement(id, section, context)
        }
        targets.definitions.firstNotNullOfOrNull { it.surface }
            ?.let { return PlacementSettings(true, it) }
        return PlacementSettings.NONE
    }

    /**
     * What self-seeding places: whatever was asked for, else what each target
     * declares as its `plant`, else the target blocks themselves. Every target
     * is seeded, so a coal minion places deepslate coal as well as coal.
     *
     * Seeding the target block back is the point of a self-seeding miner - it
     * supplies its own ore instead of waiting on a player. What keeps that from
     * running away is [com.exanthiax.ecominions.minions.service.SeedingService]
     * seeding only inside the box the minion mines.
     *
     * A woodcutter must plant a sapling rather than a log, so a target with no
     * `plant` gives it nothing to seed and is reported.
     */
    private fun readSeedBlocks(
        section: Config,
        targets: TargetSet,
        type: MinionType,
        context: LoadContext
    ): List<TestableBlock> {
        val raw = section.getStringOrNull("seed-block")
        if (!raw.isNullOrBlank()) {
            return listOfNotNull(
                blocks.lookup(raw)
                    ?: context.invalid("placement.seed-block", raw, "a block or custom block id")
            )
        }

        val plants = targets.definitions.mapNotNull { it.plant }
        val fallback = plants.ifEmpty {
            if (type == MinionType.WOODCUTTER) emptyList() else targets.definitions.map { it.block }
        }

        if (fallback.isEmpty()) {
            context.violation(
                "placement.self-seed",
                "self-seed is on but there is nothing to seed - give the target block a " +
                    "'plant' in blocks/, or set placement.seed-block"
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

        val surface = blocks.lookup(raw)
        if (surface == null) {
            context.violation("placement.surface-block", "'$raw' is not a block or custom block id")
            return PlacementSettings.NONE
        }
        return PlacementSettings(true, surface)
    }

    /** Keys are item ids, so a custom drop is as priceable as a vanilla one. */
    private fun readSellPrices(config: Config, context: LoadContext): PriceList {
        val section = config.getSubsection("sell-prices")
        return PriceList(
            section.getKeys(false).mapNotNull { key ->
                val item = items.lookup(key)
                    ?: return@mapNotNull context.invalid<PriceList.Entry>(
                        "sell-prices", key, "an item or custom item id"
                    )
                PriceList.Entry(
                    item = item,
                    price = section.getDouble(key),
                    vanillaMaterial = Material.matchMaterial(key)
                )
            }
        )
    }

    private companion object {
        const val AUTO_SELL = "auto_sell"
        const val DEFAULT_SPAWN_ITEM = "ARMOR_STAND"
        const val NAMESPACE = "ecominions"

        val BASE_STAT_MINIMUMS = mapOf(
            "speed-ticks" to 1,
            "efficiency-ticks" to 0,
            "radius" to 1,
            "height" to 1,
            "storage-slots" to 1,
            "max-storage-slots" to 1,
            "regen-delay-ticks" to 1
        )
    }
}
