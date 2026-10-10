package com.exanthiax.ecominions

import com.exanthiax.ecominions.api.EcoMinionsApi
import com.exanthiax.ecominions.api.impl.FuelsManagerImpl
import com.exanthiax.ecominions.api.impl.MenusManagerImpl
import com.exanthiax.ecominions.api.impl.MinionsManagerImpl
import com.exanthiax.ecominions.api.impl.UpgradesManagerImpl
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
import com.exanthiax.ecominions.core.event.BukkitEventDispatcher
import com.exanthiax.ecominions.core.lifecycle.LifecycleManager
import com.exanthiax.ecominions.core.persistence.MinionDataKeys
import com.exanthiax.ecominions.core.placeholders.PlaceholderRegistrar
import com.exanthiax.ecominions.core.placeholders.TemplateContext
import com.exanthiax.ecominions.core.placeholders.TemplateRenderer
import com.exanthiax.ecominions.core.registry.ConfigDrivenRegistry
import com.exanthiax.ecominions.core.scheduling.Regions
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
import com.exanthiax.ecominions.minions.integration.MinionPickListener
import com.exanthiax.ecominions.minions.integration.MinionPistonListener
import com.exanthiax.ecominions.minions.integration.MinionPlaceholders
import com.exanthiax.ecominions.minions.integration.MinionPlacementListener
import com.exanthiax.ecominions.minions.integration.PaperMinionAttackListener
import com.exanthiax.ecominions.minions.integration.PaperMinionPickListener
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
import com.exanthiax.ecominions.minions.service.MinionCollectService
import com.exanthiax.ecominions.minions.service.MinionCompactService
import com.exanthiax.ecominions.minions.service.MinionDropTables
import com.exanthiax.ecominions.minions.service.MinionFactory
import com.exanthiax.ecominions.minions.service.MinionFuelService
import com.exanthiax.ecominions.minions.service.MinionItemFactory
import com.exanthiax.ecominions.minions.service.MinionItemService
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionMiningLoop
import com.exanthiax.ecominions.minions.service.MinionService
import com.exanthiax.ecominions.minions.service.MinionStateRepository
import com.exanthiax.ecominions.minions.service.MinionTicker
import com.exanthiax.ecominions.minions.service.MinionUpgradeService
import com.exanthiax.ecominions.minions.service.MiningTargetFinder
import com.exanthiax.ecominions.minions.service.OfflineProgressService
import com.exanthiax.ecominions.minions.service.PlacementLimits
import com.exanthiax.ecominions.minions.service.PlacedMinionRegistry
import com.exanthiax.ecominions.minions.service.PlacementService
import com.exanthiax.ecominions.minions.service.RegenService
import com.exanthiax.ecominions.minions.service.SeedingService
import com.exanthiax.ecominions.minions.service.activity.BlockActivity
import com.exanthiax.ecominions.minions.service.activity.MinionActivities
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
import com.willfp.eco.core.data.PlayerProfile
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.items.Items
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ExperienceOrb
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.ServicePriority

/**
 * Composition root: the only class that constructs anything.
 *
 * Wiring only. If a decision about how minions behave appears below, it is in the
 * wrong file.
 */
class EcoMinionsPlugin : LibreforgePlugin() {

    // Core
    private val report = LoadReport().apply { quiet = true }
    private val events = BukkitEventDispatcher
    private val dataKeys = MinionDataKeys(this)
    private val placeholderRegistrar = PlaceholderRegistrar(this)
    private val renderer = TemplateRenderer(placeholderRegistrar)
    private val messages = Messages(langYml, renderer)
    private val levellingPlaceholders = LevellingPlaceholders { langYml.getFormattedString("infinity") }
    private val worlds = WorldSettings(this)
    private val regions = Regions(this)

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
            compactorRecipes::ids,
            { upgrades.ids() }
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
        events = events,
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
    private val compactService = MinionCompactService(compactorService, events)
    private val repository = MinionStateRepository(dataKeys) { minionTypes.find(it) }
    private val minionFactory = MinionFactory()
    private val itemFactory = MinionItemFactory(repository, renderer)
    private val limits = PlacementLimits(minionSettings)
    private val shopIntegration = ShopIntegrationService(minionSettings)
    private val sellPricingSettings = SettingsHolder(minionSettings.value.sellPricing)
    private val sellPricing = SellPricingService(sellPricingSettings) { shopIntegration.unitsFor(it) }
    private val sellService = SellService(sellPricing, shopIntegration, events, compactService::isInput)
    private val dropTables = MinionDropTables(
        resolve = { material, location -> vanillaDrops(material, location) },
        resolveItem = { id -> Items.lookup(id).item.takeIf { !Items.isEmpty(it) }?.clone() }
    )
    private val seedingService = SeedingService()
    private val regenService = RegenService(minionSettings, triggerDispatch, events)
    private val behaviours = MinionBehaviours(
        miner = MinerBehaviour(regenService, seedingService),
        woodcutter = WoodcutterBehaviour(TreeService()),
        farmer = FarmerBehaviour(regenService, seedingService)
    )
    private val miningTargets = MiningTargetFinder()
    private val blockActivity = BlockActivity(
        miningTargets, blockIndex, regenService, behaviours, triggerDispatch, events, dropTables
    ) { minion, amount -> spawnXpOrb(minion, amount) }
    private val activities = MinionActivities(blockActivity)
    private val offlineProgress = OfflineProgressService(
        minionSettings, fuelService, triggerDispatch, events, activities::get
    )
    private val ticker = MinionTicker(worlds, fuelService, triggerDispatch, events, activities::get)
    private val activityService = MinionActivityService(
        repository, offlineProgress, minionSettings
    ) { Bukkit.getPlayer(it) != null }
    private val placedMinionsKey = PersistentDataKey(
        namespacedKeyFactory.create("placed_minions"),
        PersistentDataKeyType.STRING_LIST,
        emptyList()
    )
    private val placedMinions = PlacedMinionRegistry(
        read = { PlayerProfile.load(it).read(placedMinionsKey) },
        write = { owner, entries -> PlayerProfile.load(owner).write(placedMinionsKey, entries) }
    )
    private val minionService = MinionService(
        minionTypes, minionFactory, repository,
        limits, activityService, minionSettings, events, regions, placedMinions
    )
    private val placementService = PlacementService(
        minionService, minionFactory, itemFactory, repository, limits, worlds, minionSettings,
        triggerDispatch, events
    )
    private val minionFuelService = MinionFuelService(fuelService, repository, triggerDispatch, events)
    private val purchases = MinionUpgradeService(minionService, triggerDispatch, events)
    private val minionItems = MinionItemService(itemFactory, events)
    private val collector = MinionCollectService(repository, events)

    // Presentation
    private val chrome = MenuChrome(messages)
    private val costSelectMenu = CostSelectMenu(upgradeMenuSettings, messages, chrome, purchases, events, regions)
    private val upgradeMenu = UpgradeMenu(
        upgradeMenuSettings, messages, chrome, upgradeService, purchases, costSelectMenu, events, regions
    )
    private val upgradeListMenu = UpgradeListMenu(
        upgradeMenuSettings, messages, chrome, upgradeService, upgradeMenu, events
    )
    private val menuViewers = MenuViewers { menu, player -> regions.on(player) { menu.refresh(player) } }
    private val mainMenu = MainMenu(
        menuSettings, messages, chrome, MinionStorageView(messages), placementService,
        fuelService, minionFuelService, collector, upgradeListMenu, menuViewers, events, regions
    )
    private val typeSelectMenu = TypeSelectMenu(menuSettings, messages, chrome, minionService, itemFactory, minionItems)

    // API
    private val api = EcoMinionsApiImpl(
        MinionsManagerImpl(
            minionService, placementService, itemFactory, minionItems, collector,
            sellService, repository, activityService, xpService, compactService, events, regions
        ) { Bukkit.getOfflinePlayer(it) },
        UpgradesManagerImpl(upgradeService, purchases, minionService),
        FuelsManagerImpl(fuelService, minionFuelService, minionService),
        MenusManagerImpl(mainMenu, upgradeListMenu, typeSelectMenu, minionService, regions)
    )

    // Lifecycle
    private val placementListener = MinionPlacementListener(minionService, placementService, itemFactory, mainMenu, messages)
    private val customItems = MinionCustomItems(this, minionService, itemFactory)
    private val recipes = MinionRecipes(this, minionService, itemFactory)
    private val lifecycle = LifecycleManager(
        this,
        listOf(
            MinionMiningLoop(minionService, ticker, activityService, regions),
            MinionRenderLoop(
                minionService, MinionAnimator(),
                MinionHologram(messages, minionSettings, activityService),
                ParticleEmitter(), minionSettings, activityService, regions
            ),
            menuViewers
        )
    )

    override fun loadConfigCategories(): List<ConfigCategory> =
        LibreforgeRegistrations.configCategories(blockDefinitions, minionTypes, upgrades, fuels, compactorRecipes)

    override fun handleEnable() {
        LibreforgeRegistrations.registerAll(upgrades, worlds, sellService, compactService, xpService)
        shopIntegration.init()
        wireMenuNavigation()
        registerPlaceholders()
        registerApi()

        customItems.registerAll()
        recipes.registerAll()
        lifecycle.start()
    }

    override fun handleReload() {
        blockIndex.invalidate()
        refreshSettings()
        registerPlaceholders()
        dropTables.clear()

        customItems.registerAll()
        recipes.registerAll()
        minionService.refreshAll()
        minionService.hydrateLoadedWorlds()
        lifecycle.restart()

        if (!report.quiet) {
            logger.info(report.summary())
        }
        report.reset()
    }

    override fun handleAfterLoad() {
        report.quiet = false
    }

    override fun handleDisable() {
        lifecycle.stop()
        minionService.shutdownAll()
    }

    override fun loadListeners(): List<Listener> = listOfNotNull(
        placementListener,
        if (PaperMinionAttackListener.isSupported) PaperMinionAttackListener(minionService, placementListener) else null,
        MinionPickListener(minionService),
        if (PaperMinionPickListener.isSupported) PaperMinionPickListener(minionService) else null,
        MinionChunkListener(minionService),
        MinionPistonListener(minionService),
        MinionOwnerListener(minionService, activityService, regions)
    )

    override fun loadPluginCommands(): List<PluginCommand> = listOf(
        CommandMinions(
            this,
            typeSelectMenu,
            listOf(
                CommandOpen(this, typeSelectMenu),
                CommandGive(this, minionService, minionItems, messages, regions),
                CommandReload(this, regions) { minionTypes.values().size },
                CommandRemove(this, minionService, placementService, messages, regions),
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
                MinionPlaceholders(minionService, fuelService),
                UpgradePlaceholders(),
                levellingPlaceholders
            )
        )
    }

    private fun registerApi() {
        Bukkit.getServicesManager().register(
            EcoMinionsApi::class.java,
            api,
            this,
            ServicePriority.Normal
        )
    }

    /**
     * What a vanilla block drops when broken, rolled the way live mining rolls it.
     *
     * A `blocks/` loot table cannot be populated through Bukkit's loot context, so
     * the drops come from a snapshot of the block at [location] retyped to
     * [material]; the world itself is never changed.
     */
    private fun vanillaDrops(material: Material, location: Location): List<ItemStack> =
        runCatching { location.block.state.apply { type = material }.drops.toList() }
            .getOrDefault(emptyList())

    /** Adds to an orb already at the minion, so an unattended minion keeps one orb rather than a pile. */
    private fun spawnXpOrb(minion: Minion, amount: Int) {
        val location = minion.entity.location.clone().add(0.0, 0.5, 0.0)
        location.getNearbyEntitiesByType(ExperienceOrb::class.java, XP_MERGE_RADIUS)
            .firstOrNull { it.isValid }
            ?.let {
                it.experience += amount
                return
            }
        minion.entity.world.spawn(location, ExperienceOrb::class.java).experience = amount
    }

    private fun refreshSettings() {
        minionSettings.update(minionSettingsLoader.load())
        sellPricingSettings.update(minionSettings.value.sellPricing)
        menuSettings.update(menuSettingsLoader.load())
        upgradeMenuSettings.update(upgradeMenuSettingsLoader.load())
    }

    private companion object {
        const val XP_MERGE_RADIUS = 1.5
    }
}
