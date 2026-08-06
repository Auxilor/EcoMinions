package com.exanthiax.ecominions.minions.integration

import com.exanthiax.ecominions.core.data.JsonData
import com.exanthiax.ecominions.minions.model.GameData
import com.exanthiax.ecominions.minions.model.GrowthRules
import com.exanthiax.ecominions.minions.model.WoodFamilies
import com.exanthiax.ecominions.minions.model.WoodFamily
import com.google.gson.JsonObject
import org.bukkit.Material

/**
 * Reads the bundled JSON data files into [GameData].
 *
 * These files ship with the plugin rather than being admin-authored, so an
 * unreadable entry is skipped quietly: it means a material this server's version
 * does not have, not a mistake anyone can fix.
 */
class GameDataLoader {

    fun load(): GameData = GameData(
        growthRules = loadGrowthRules(),
        woodFamilies = WoodFamilies(loadWoodFamilies()),
        cropSurfaces = loadMaterialMap("/data/crop-defaults.json"),
        smelting = loadMaterialMap("/data/smelting.json")
    )

    private fun loadGrowthRules(): GrowthRules {
        val root = JsonData.read("/data/growth-rules.json")
        return GrowthRules(
            promotions = root.getAsJsonObject("promotions").entrySet().mapNotNull { (from, to) ->
                val source = Material.matchMaterial(from) ?: return@mapNotNull null
                val target = Material.matchMaterial(to.asString) ?: return@mapNotNull null
                source to target
            }.toMap(),
            invalidTargets = root.materialSet("invalid-targets"),
            stackedColumns = root.materialSet("stacked-columns")
        )
    }

    private fun loadWoodFamilies(): List<WoodFamily> =
        JsonData.read("/data/wood-families.json")
            .getAsJsonArray("families")
            .mapNotNull { element ->
                val obj = element.asJsonObject
                WoodFamily(
                    id = obj["id"].asString,
                    sapling = Material.matchMaterial(obj["sapling"].asString) ?: return@mapNotNull null,
                    log = Material.matchMaterial(obj["log"].asString) ?: return@mapNotNull null,
                    leaves = Material.matchMaterial(obj["leaves"].asString) ?: return@mapNotNull null,
                    defaultSurface = obj["default-surface"]?.asString?.let { Material.matchMaterial(it) },
                    members = obj.materialSet("members")
                )
            }

    private fun loadMaterialMap(path: String): Map<Material, Material> =
        JsonData.read(path).entrySet().mapNotNull { (from, to) ->
            val source = Material.matchMaterial(from) ?: return@mapNotNull null
            val target = Material.matchMaterial(to.asString) ?: return@mapNotNull null
            source to target
        }.toMap()

    private fun JsonObject.materialSet(key: String): Set<Material> =
        getAsJsonArray(key).mapNotNull { Material.matchMaterial(it.asString) }.toSet()
}
