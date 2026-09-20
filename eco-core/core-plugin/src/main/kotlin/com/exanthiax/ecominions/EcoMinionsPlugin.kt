package com.exanthiax.ecominions

import com.exanthiax.ecominions.api.EcoMinionsApi
import com.exanthiax.ecominions.commands.CommandGive
import com.exanthiax.ecominions.commands.CommandInfo
import com.exanthiax.ecominions.commands.CommandMinions
import com.exanthiax.ecominions.commands.CommandOpen
import com.exanthiax.ecominions.commands.CommandReload
import com.exanthiax.ecominions.commands.CommandRemove
import com.exanthiax.ecominions.compactor.integration.CompactorRecipeLoader
import com.exanthiax.ecominions.compactor.service.CompactorService
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.error.LoadReport
import com.exanthiax.ecominions.core.lifecycle.LifecycleManager
import com.exanthiax.ecominions.core.persistence.MinionDataKeys
import com.exanthiax.ecominions.core.placeholders.PlaceholderRegistrar
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.placeholders.TemplateRenderer
import com.exanthiax.ecominions.core.registry.ConfigDrivenRegistry
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.core.world.EcoBlockLookup
import com.exanthiax.ecominions.core.world.EcoItemLookup
import com.exanthiax.ecominions.core.world.WorldSettings
import com.exanthiax.ecominions.fuel.integration.FuelLoader
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.levelling.service.MinionXpService
import com.exanthiax.ecominions.libreforge.LibreforgeRegistrations
import com.exanthiax.ecominions.libreforge.ownerPlayer
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.minions.integration.MenuSettingsLoader
import com.exanthiax.ecominions.minions.integration.MinionChunkListener
import com.exanthiax.ecominions.minions.integration.MinionCustomItems
import com.exanthiax.ecominions.minions.integration.MinionOwnerListener
import com.exanthiax.ecominions.minions.integration.MinionPlaceholders
import com.exanthiax.ecominions.minions.integration.MinionPlacementListener
import com.exanthiax.ecominions.minions.integration.MinionRecipes
import com.exanthiax.ecominions.minions.integration.MinionBlockLoader
import com.exanthiax.ecominions.minions.integration.VanillaBlockDefaults
import com.exanthiax.ecominions.minions.integration.MinionSettingsLoader
import com.exanthiax.ecominions.levelling.integration.LevellingLoader
import com.exanthiax.ecominions.levelling.integration.LevellingPlaceholders
import com.exanthiax.ecominions.minions.integration.MinionDefinitionLoader
import com.exanthiax.ecominions.minions.integration.ParticleSettingsLoader
import com.exanthiax.ecominions.minions.integration.shop.ShopIntegrationService
import com.exanthiax.ecominions.minions.model.MinionBlock
import com.exanthiax.ecominions.minions.model.MinionDefinition
import com.exanthiax.ecominions.minions.service.MinionActivityService
import com.exanthiax.ecominions.minions.service.MinionBlockIndex
import com.exanthiax.ecominions.minions.service.MinionDropTables
import com.exanthiax.ecominions.minions.service.MinionFactory
import com.exanthiax.ecominions.minions.service.MinionFuelService
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionMiningLoop
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.MinionStateRepository
import com.exanthiax.ecominions.minions.service.MinionUpgradeService
import com.exanthiax.ecominions.minions.service.MiningService
import com.exanthiax.ecominions.minions.service.MiningTargetFinder
import com.exanthiax.ecominions.minions.service.OfflineProgressService
import com.exanthiax.ecominions.minions.service.PlacementLimits
import com.exanthiax.ecominions.minions.service.PlacementService
import com.exanthiax.ecominions.minions.service.RegenService
import com.exanthiax.ecominions.minions.service.SeedingService
import com.exanthiax.ecominions.minions.service.behaviour.FarmerBehaviour
import com.exanthiax.ecominions.minions.service.behaviour.MinerBehaviour
import com.exanthiax.ecominions.minions.service.behaviour.MinionBehaviours
import com.exanthiax.ecominions.minions.service.behaviour.WoodcutterBehaviour
import com.exanthiax.ecominions.minions.service.SellPricingService
import com.exanthiax.ecominions.minions.service.SellService
import com.exanthiax.ecominions.minions.service.TreeService
import com.exanthiax.ecominions.minions.ui.MainMenu
import com.exanthiax.ecominions.minions.ui.MenuChrome
import com.exanthiax.ecominions.minions.ui.MenuViewers
import com.exanthiax.ecominions.minions.ui.MinionAnimator
import com.exanthiax.ecominions.minions.ui.MinionHologram
import com.exanthiax.ecominions.minions.ui.MinionRenderLoop
import com.exanthiax.ecominions.minions.ui.MinionStorageView
import com.exanthiax.ecominions.minions.ui.ParticleEmitter
import com.exanthiax.ecominions.minions.ui.TypeSelectMenu
import com.exanthiax.ecominions.upgrades.integration.UpgradeLoader
import com.exanthiax.ecominions.upgrades.integration.UpgradeMenuSettingsLoader
import com.exanthiax.ecominions.upgrades.integration.UpgradePlaceholders
import com.exanthiax.ecominions.upgrades.model.Upgrade
import com.exanthiax.ecominions.upgrades.service.UpgradeService
import com.exanthiax.ecominions.upgrades.ui.CostSelectMenu
import com.exanthiax.ecominions.upgrades.ui.UpgradeListMenu
import com.exanthiax.ecominions.upgrades.ui.UpgradeMenu
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.items.Items
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootContext
import org.bukkit.plugin.ServicePriority

/**
 * Composition root: the only class that constructs anything.
 *
 * Wiring only. If a decision about how minions behave appears below, it is in the
 * wrong file.
 */
class EcoMinionsPlugin : LibreforgePlugin() {

    // Core
    private val report = LoadReport()
    private val dataKeys = MinionDataKeys(this)
    private val placeholderRegistrar = PlaceholderRegistrar(this)
    private val renderer = TemplateRenderer(placeholderRegistrar)
    private val messages = Messages(langYml, renderer)
    private val levellingPlaceholders = LevellingPlaceholders { langYml.getFormattedString("infinity") }
    private val worlds = WorldSettings(configYml)

    // Settings
    private val minionSettingsLoader = MinionSettingsLoader(configYml)
    private val menuSettingsLoader = MenuSettingsLoader(configYml)
    private val upgradeMenuSettingsLoader = UpgradeMenuSettingsLoader(configYml)
    private val minionSettings = SettingsHolder(minionSettingsLoader.load())
    private val menuSettings = SettingsHolder(menuSettingsLoader.load())
    private val upgradeMenuSettings = SettingsHolder(upgradeMenuSettingsLoader.load())

    // Registries
    private val compactorRecipes = ConfigDrivenRegistry(
        "compactor_recipe", "compactor-recipes", "compactor recipes",
        CompactorRecipeLoader(), report
    )
    private val blockLookup = EcoBlockLookup()
    private val itemLookup = EcoItemLookup()
    private val blockDefinitions = ConfigDrivenRegistry(
        "minion_block", "blocks", "block definitions",
        MinionBlockLoader(blockLookup), report
    )
    private val vanillaBlocks = VanillaBlockDefaults(blockLookup)
    private val blockIndex = MinionBlockIndex({ blockDefinitions.all() }, vanillaBlocks)
    private val minionTypes = ConfigDrivenRegistry(
        "minion_type", "minions", "minion types",
        MinionDefinitionLoader(
            blockLookup,
            blockIndex::forLookup,
            itemLookup,
            ParticleSettingsLoader(),
            LevellingLoader(),
            { minionSettings.value.sellPricing.autoFromEcoShop },
            { minionSettings.value.hardSlotCap },
            compactorRecipes::find,
            compactorRecipes::ids
        ),
        report
    )
    private val upgrades = ConfigDrivenRegistry<Upgrade>(
        "upgrade", "upgrades", "upgrades", UpgradeLoader(), report
    )
    private val fuels = ConfigDrivenRegistry<Fuel>(
        "fuel", "fuels", "fuels", FuelLoader(), report
    )

    // Services
    private val xpService: MinionXpService = MinionXpService(
        applyState = { minion, state -> minionService.updateState(minion, state) },
        onLevelChanged = { minion -> minionService.onLevelChanged(minion) },
        notifyLevelUp = { minion, level ->
            minion.ownerPlayer()?.sendMessage(
                messages.line(
                    "messages.level-up",
                    TemplateContext.of(
                        "minion_name" to minion.state.definition.name,
                        "level" to level
                    )
                )
            )
        }
    )
    private val triggerDispatch = MinionTriggerDispatch(xpService)
    private val upgradeService = UpgradeService(upgrades)
    private val fuelService = FuelService(fuels)
    private val compactorService = CompactorService()
    private val repository = MinionStateRepository(dataKeys) { minionTypes.find(it) }
    private val minionFactory = MinionFactory()
    private val itemFactory = MinionItemFactory(repository, renderer)
    private val limits = PlacementLimits(minionSettings)
    private val shopIntegration = ShopIntegrationService(minionSettings)
    private val sellPricingSettings = SettingsHolder(minionSettings.value.sellPricing)
    private val sellPricing = SellPricingService(sellPricingSettings) { shopIntegration.unitsFor(it) }
    private val sellService = SellService(sellPricing, shopIntegration)
    private val dropTables = MinionDropTables(
        resolve = { material, location -> vanillaDrops(material, location) },
        resolveItem = { id -> Items.lookup(id).item.takeIf { !Items.isEmpty(it) }?.clone() }
    )
    private val offlineProgress = OfflineProgressService(
        minionSettings, fuelService, dropTables, triggerDispatch
    )
    private val activityService = MinionActivityService(
        repository, offlineProgress, minionSettings
    ) { Bukkit.getPlayer(it) != null }
    private val minionService = MinionService(
        minionTypes, minionFactory, repository,
        itemFactory, limits, activityService, minionSettings
    )
    private val seedingService = SeedingService()
    private val regenService = RegenService(minionSettings, triggerDispatch)
    private val behaviours = MinionBehaviours(
        miner = MinerBehaviour(regenService, seedingService),
        woodcutter = WoodcutterBehaviour(TreeService()),
        farmer = FarmerBehaviour(regenService, seedingService)
    )
    private val miningTargets = MiningTargetFinder()
    private val miningService = MiningService(
        minionSettings, worlds, miningTargets, blockIndex, regenService, behaviours,
        fuelService, triggerDispatch
    )
    private val placementService = PlacementService(
        minionService, minionFactory, itemFactory, repository, limits, worlds, minionSettings
    )
    private val minionFuelService = MinionFuelService(fuelService, repository)
    private val purchases = MinionUpgradeService(minionService)

    // Presentation
    private val chrome = MenuChrome(messages)
    private val costSelectMenu = CostSelectMenu(upgradeMenuSettings, messages, chrome, purchases)
    private val upgradeMenu = UpgradeMenu(
        upgradeMenuSettings, messages, chrome, upgradeService, purchases, costSelectMenu
    )
    private val upgradeListMenu = UpgradeListMenu(
        upgradeMenuSettings, messages, chrome, upgradeService, upgradeMenu
    )
    private val menuViewers = MenuViewers()
    private val mainMenu = MainMenu(
        menuSettings, messages, chrome, MinionStorageView(messages), placementService,
        fuelService, minionFuelService, repository, upgradeListMenu, menuViewers
    )
    private val typeSelectMenu = TypeSelectMenu(menuSettings, messages, chrome, minionService, itemFactory)

    // Lifecycle
    private val customItems = MinionCustomItems(this, minionService, itemFactory)
    private val recipes = MinionRecipes(this, minionService, itemFactory)
    private val lifecycle = LifecycleManager(
        this,
        listOf(
            MinionMiningLoop(minionService, miningService, activityService),
            MinionRenderLoop(
                minionService, MinionAnimator(),
                MinionHologram(messages, minionSettings, activityService),
                ParticleEmitter(), minionSettings, activityService
            ),
            menuViewers
        )
    )

    override fun loadConfigCategories(): List<ConfigCategory> =
        LibreforgeRegistrations.configCategories(blockDefinitions, minionTypes, upgrades, fuels, compactorRecipes)

    override fun handleEnable() {
        LibreforgeRegistrations.registerAll(upgrades, sellService, compactorService, triggerDispatch, xpService)
        shopIntegration.init()
        wireMenuNavigation()
        registerPlaceholders()
        registerApi()

        customItems.registerAll()
        recipes.registerAll()
        minionService.hydrateLoadedWorlds()
        lifecycle.start()

        logger.info(report.summary())
    }

    override fun handleReload() {
        blockIndex.invalidate()
        refreshSettings()
        registerPlaceholders()
        dropTables.clear()

        customItems.registerAll()
        recipes.registerAll()
        minionService.refreshAll()
        lifecycle.restart()

        logger.info(report.summary())
    }

    override fun handleDisable() {
        lifecycle.stop()
        minionService.shutdownAll()
    }

    override fun loadListeners(): List<Listener> = listOf(
        MinionPlacementListener(minionService, placementService, itemFactory, mainMenu, messages),
        MinionChunkListener(minionService),
        MinionOwnerListener(minionService, activityService)
    )

    override fun loadPluginCommands(): List<PluginCommand> = listOf(
        CommandMinions(
            this,
            typeSelectMenu,
            listOf(
                CommandOpen(this, typeSelectMenu),
                CommandGive(this, minionService, itemFactory, messages),
                CommandReload(this) { minionTypes.values().size },
                CommandRemove(this, minionService, messages),
                CommandInfo(this, minionService, messages)
            )
        )
    )

    /** Menus form a cycle, so navigation is wired here rather than injected. */
    private fun wireMenuNavigation() {
        upgradeListMenu.onBack = { player, minion -> mainMenu.open(player, minion) }
        upgradeMenu.onBack = { player, minion -> upgradeListMenu.open(player, minion) }
        costSelectMenu.onPurchased = { player, minion, upgrade -> upgradeMenu.open(player, minion, upgrade) }
    }

    private fun registerPlaceholders() {
        placeholderRegistrar.registerAll(
            listOf(
                MinionPlaceholders(minionService),
                UpgradePlaceholders(),
                levellingPlaceholders
            )
        )
    }

    private fun registerApi() {
        Bukkit.getServicesManager().register(
            EcoMinionsApi::class.java,
            EcoMinionsApiImpl(minionService, upgradeService, fuelService),
            this,
            ServicePriority.Normal
        )
    }

    /**
     * What a vanilla block drops when broken, or nothing if this server will not say.
     *
     * A `blocks/` loot table requires the tool and the block state that broke the
     * block, and neither can be carried by Bukkit's [LootContext], so populating
     * one throws on a modern server rather than returning empty. Catch-up then
     * falls back to the block itself, which is what a block's `drops:` list is
     * there to correct.
     */
    private fun vanillaDrops(material: Material, location: Location): List<ItemStack> {
        val key = NamespacedKey.minecraft("blocks/${material.key.key}")
        val table = Bukkit.getLootTable(key) ?: return emptyList()
        val context = LootContext.Builder(location).build()

        return runCatching { table.populateLoot(java.util.Random(), context).toList() }
            .getOrDefault(emptyList())
    }

    private fun refreshSettings() {
        minionSettings.update(minionSettingsLoader.load())
        sellPricingSettings.update(minionSettings.value.sellPricing)
        menuSettings.update(menuSettingsLoader.load())
        upgradeMenuSettings.update(upgradeMenuSettingsLoader.load())
    }
}
