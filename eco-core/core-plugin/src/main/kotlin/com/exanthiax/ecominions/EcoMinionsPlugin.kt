package com.exanthiax.ecominions

import com.exanthiax.ecominions.api.EcoMinionsApi
import com.exanthiax.ecominions.commands.CommandMinions
import com.exanthiax.ecominions.commands.GiveSubcommand
import com.exanthiax.ecominions.commands.InfoSubcommand
import com.exanthiax.ecominions.commands.OpenSubcommand
import com.exanthiax.ecominions.commands.RemoveSubcommand
import com.exanthiax.ecominions.commands.ReloadSubcommand
import com.exanthiax.ecominions.compactor.integration.CompactorRecipeLoader
import com.exanthiax.ecominions.compactor.model.CompactorRecipe
import com.exanthiax.ecominions.compactor.service.CompactorService
import com.exanthiax.ecominions.core.config.SettingsHolder
import com.exanthiax.ecominions.core.error.LoadReport
import com.exanthiax.ecominions.core.lifecycle.LifecycleManager
import com.exanthiax.ecominions.core.persistence.MinionDataKeys
import com.exanthiax.ecominions.core.placeholders.PlaceholderRegistrar
import com.exanthiax.ecominions.core.placeholders.TemplateRenderer
import com.exanthiax.ecominions.core.registry.ConfigDrivenRegistry
import com.exanthiax.ecominions.core.text.Messages
import com.exanthiax.ecominions.core.world.WorldSettings
import com.exanthiax.ecominions.fuel.integration.FuelLoader
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.libreforge.LibreforgeRegistrations
import com.exanthiax.ecominions.minions.integration.GameDataLoader
import com.exanthiax.ecominions.minions.integration.MenuSettingsLoader
import com.exanthiax.ecominions.minions.integration.MinionChunkListener
import com.exanthiax.ecominions.minions.integration.MinionCustomItems
import com.exanthiax.ecominions.minions.integration.MinionOwnerListener
import com.exanthiax.ecominions.minions.integration.MinionPlaceholders
import com.exanthiax.ecominions.minions.integration.MinionPlacementListener
import com.exanthiax.ecominions.minions.integration.MinionRecipes
import com.exanthiax.ecominions.minions.integration.MinionSettingsLoader
import com.exanthiax.ecominions.minions.integration.MinionTypeLoader
import com.exanthiax.ecominions.minions.integration.ParticleSettingsLoader
import com.exanthiax.ecominions.minions.integration.shop.ShopIntegrationService
import com.exanthiax.ecominions.minions.model.MinionType
import com.exanthiax.ecominions.minions.service.DropWarningService
import com.exanthiax.ecominions.minions.service.MinionActivityService
import com.exanthiax.ecominions.minions.service.MinionFactory
import com.exanthiax.ecominions.minions.service.MinionFuelService
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionMiningLoop
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.MinionStateRepository
import com.exanthiax.ecominions.minions.service.MinionStatsService
import com.exanthiax.ecominions.minions.service.MinionUpgradeService
import com.exanthiax.ecominions.minions.service.MiningService
import com.exanthiax.ecominions.minions.service.OfflineProgressService
import com.exanthiax.ecominions.minions.service.PlacementLimits
import com.exanthiax.ecominions.minions.service.PlacementService
import com.exanthiax.ecominions.minions.service.SellPricingService
import com.exanthiax.ecominions.minions.service.SellService
import com.exanthiax.ecominions.minions.service.TreeFarmingService
import com.exanthiax.ecominions.minions.service.TreeService
import com.exanthiax.ecominions.minions.ui.MainMenu
import com.exanthiax.ecominions.minions.ui.MenuChrome
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
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import org.bukkit.Bukkit
import org.bukkit.event.Listener
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
    private val worlds = WorldSettings(configYml)

    // Settings
    private val minionSettingsLoader = MinionSettingsLoader(configYml)
    private val menuSettingsLoader = MenuSettingsLoader(configYml)
    private val upgradeMenuSettingsLoader = UpgradeMenuSettingsLoader(configYml)
    private val minionSettings = SettingsHolder(minionSettingsLoader.load())
    private val menuSettings = SettingsHolder(menuSettingsLoader.load())
    private val upgradeMenuSettings = SettingsHolder(upgradeMenuSettingsLoader.load())

    // Registries
    private val gameData = GameDataLoader().load()
    private val minionTypes = ConfigDrivenRegistry(
        "minion_type", "minions", "minion types",
        MinionTypeLoader(
            gameData,
            ParticleSettingsLoader(),
            { minionSettings.value.sellPricing.autoFromEcoShop }
        ),
        report
    )
    private val upgrades = ConfigDrivenRegistry<Upgrade>(
        "upgrade", "upgrades", "upgrades", UpgradeLoader(), report
    )
    private val fuels = ConfigDrivenRegistry<Fuel>(
        "fuel", "fuels", "fuels", FuelLoader(), report
    )
    private val compactorRecipes = ConfigDrivenRegistry<CompactorRecipe>(
        "compactor_recipe", "compactor-recipes", "compactor recipes",
        CompactorRecipeLoader(), report
    )

    // Services
    private val upgradeService = UpgradeService(upgrades)
    private val fuelService = FuelService(fuels)
    private val compactorService = CompactorService(compactorRecipes)
    private val statsService = MinionStatsService(upgradeService)
    private val repository = MinionStateRepository(dataKeys) { minionTypes.find(it) }
    private val minionFactory = MinionFactory(statsService)
    private val itemFactory = MinionItemFactory(repository, renderer)
    private val limits = PlacementLimits(minionSettings)
    private val offlineProgress = OfflineProgressService(minionSettings, fuelService)
    private val activityService = MinionActivityService(
        repository, offlineProgress, minionSettings
    ) { Bukkit.getPlayer(it) != null }
    private val minionService = MinionService(
        minionTypes, minionFactory, repository, statsService,
        itemFactory, limits, activityService, minionSettings
    )
    private val dropWarnings = DropWarningService(logger)
    private val treeFarming = TreeFarmingService(gameData, TreeService(), dropWarnings)
    private val shopIntegration = ShopIntegrationService(minionSettings)
    private val sellPricingSettings = SettingsHolder(minionSettings.value.sellPricing)
    private val sellPricing = SellPricingService(sellPricingSettings) { shopIntegration.unitsFor(it) }
    private val miningService = MiningService(
        minionSettings, worlds, gameData, treeFarming,
        compactorService, SellService(sellPricing, shopIntegration), dropWarnings,
        fuelService, sellPricing
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
    private val mainMenu = MainMenu(
        menuSettings, messages, chrome, MinionStorageView(messages), placementService,
        fuelService, minionFuelService, repository, upgradeListMenu
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
            )
        )
    )

    override fun loadConfigCategories(): List<ConfigCategory> =
        LibreforgeRegistrations.configCategories(minionTypes, upgrades, fuels, compactorRecipes)

    override fun handleEnable() {
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
        refreshSettings()
        registerPlaceholders()

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
                OpenSubcommand(this, typeSelectMenu),
                GiveSubcommand(this, minionService, itemFactory, messages),
                ReloadSubcommand(this, messages),
                RemoveSubcommand(this, minionService, messages),
                InfoSubcommand(this, minionService, messages)
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
                UpgradePlaceholders()
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

    private fun refreshSettings() {
        minionSettings.update(minionSettingsLoader.load())
        sellPricingSettings.update(minionSettings.value.sellPricing)
        menuSettings.update(menuSettingsLoader.load())
        upgradeMenuSettings.update(upgradeMenuSettingsLoader.load())
    }
}
