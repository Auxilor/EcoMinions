package com.exanthiax.ecominions.api.impl

import com.exanthiax.ecominions.api.fuel.FuelsManager
import com.exanthiax.ecominions.fuel.model.Fuel
import com.exanthiax.ecominions.fuel.service.FuelService
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionFuelService
import com.exanthiax.ecominions.minions.service.MinionService
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import com.exanthiax.ecominions.api.fuel.Fuel as ApiFuel
import com.exanthiax.ecominions.api.minion.Minion as ApiMinion

/** The public fuels API, over the fuel registry and the fuelling flow. */
class FuelsManagerImpl(
    private val fuels: FuelService,
    private val fuelling: MinionFuelService,
    private val minions: MinionService
) : FuelsManager {

    override fun all(): Collection<ApiFuel> = fuels.all()

    override fun getById(id: String): ApiFuel? = fuels.getById(id)

    override fun matching(item: ItemStack): ApiFuel? = fuels.matching(item)

    override fun activeFuel(minion: ApiMinion): ApiFuel? = fuels.activeFuel(minion)

    override fun remainingMillis(minion: ApiMinion): Long = fuels.remainingMillis(minion)

    override fun applyFuel(minion: ApiMinion, fuel: ApiFuel): Boolean {
        val internal = resolve(fuel)
        return fuelling.applyDirect(resolve(minion), internal, fuelling.unitMillis(internal))
    }

    override fun applyFuel(minion: ApiMinion, fuel: ApiFuel, durationMillis: Long): Boolean =
        fuelling.applyDirect(resolve(minion), resolve(fuel), durationMillis)

    override fun applyFrom(player: Player, minion: ApiMinion): ApiFuel? =
        (fuelling.fuelFrom(player, resolve(minion)) as? MinionFuelService.Outcome.Applied)?.application?.fuel

    override fun clearFuel(minion: ApiMinion) {
        fuelling.clear(resolve(minion))
    }

    private fun resolve(minion: ApiMinion): Minion =
        minions.getByUuid(minion.uuid)
            ?: throw IllegalArgumentException("Minion ${minion.uuid} is not loaded")

    private fun resolve(fuel: ApiFuel): Fuel =
        fuels.getById(fuel.id) ?: throw IllegalArgumentException("Unknown fuel: ${fuel.id}")
}
