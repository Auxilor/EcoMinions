package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.fuel.model.Fuel

/** A feeder's fuel filter. An empty [fuelIds] accepts every fuel. */
data class FeederSettings(val fuelIds: List<String>)

fun FeederSettings.accepts(fuel: Fuel): Boolean = fuelIds.isEmpty() || fuel.id in fuelIds
