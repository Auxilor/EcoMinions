package com.exanthiax.ecominions.core.registry

import com.exanthiax.ecominions.core.error.LoadContext
import com.exanthiax.ecominions.core.error.LoadReport
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.RegistrableCategory

/**
 * A folder of config files loaded into model objects.
 *
 * One generic implementation, instantiated per domain at the composition root,
 * rather than a hand-written singleton per domain. The [entry] wrapper keeps
 * eco's registry contract out of the model layer.
 */
class ConfigDrivenRegistry<T : Any>(
    id: String,
    directory: String,
    private val displayName: String,
    private val loader: ConfigLoader<T>,
    private val report: LoadReport,
    /** Runs before this folder reloads; the first category uses it to refresh settings the loaders read. */
    private val onBeforeReload: () -> Unit = {}
) : RegistrableCategory<ConfigDrivenRegistry.Entry<T>>(id, directory) {

    /** Adapts a model object to eco's registry, which requires an id-bearing type. */
    class Entry<T : Any>(override val id: String, val value: T) : KRegistrable

    override fun beforeReload(plugin: LibreforgePlugin) {
        onBeforeReload()
    }

    override fun clear(plugin: LibreforgePlugin) {
        registry.clear()
    }

    override fun acceptConfig(plugin: LibreforgePlugin, id: String, config: Config) {
        val context = LoadContext(ViolationContext(plugin, "$directory/$id.yml"), report)
        val loaded = loader.load(id, config, context) ?: return
        registry.register(Entry(id, loaded))
    }

    override fun afterReload(plugin: LibreforgePlugin) {
        report.recordLoaded(displayName, values().size)
    }

    /** Sorted by id: eco's registry has no order, and menus built from it must not shuffle. */
    fun all(): Collection<T> = values().sortedBy { it.id }.map { it.value }

    fun ids(): Collection<String> = values().map { it.id }.sorted()

    fun find(id: String?): T? = getByID(id)?.value
}
