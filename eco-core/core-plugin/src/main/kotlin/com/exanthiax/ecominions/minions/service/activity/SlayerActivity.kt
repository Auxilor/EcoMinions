package com.exanthiax.ecominions.minions.service.activity

import com.exanthiax.ecominions.api.event.MinionKillEvent
import com.exanthiax.ecominions.api.minion.MinionType
import com.exanthiax.ecominions.core.event.EventDispatcher
import com.exanthiax.ecominions.libreforge.MinionTriggerDispatch
import com.exanthiax.ecominions.libreforge.ownerPlayer
import com.exanthiax.ecominions.libreforge.triggers.TriggerMinionKillMob
import com.exanthiax.ecominions.minions.model.KillRecord
import com.exanthiax.ecominions.minions.model.KillSample
import com.exanthiax.ecominions.minions.model.SlayerSettings
import com.exanthiax.ecominions.minions.service.ExperienceSink
import com.exanthiax.ecominions.minions.service.Minion
import com.exanthiax.ecominions.minions.service.MinionWorkArea
import com.exanthiax.ecominions.minions.service.WorkAreaEntities
import com.willfp.eco.core.entities.TestableEntity
import com.willfp.libreforge.triggers.event.DropCause
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.entity.Tameable
import org.bukkit.event.HandlerList
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Kills mobs in its box, and spawns frozen ones to kill when it self-seeds.
 *
 * A kill is `health = 0`, not damage: no damage event means eco never builds an
 * `EntityDeathByEntityEvent`, so libreforge does not add mob loot a second time.
 */
class SlayerActivity(
    private val entities: WorkAreaEntities,
    private val spawner: (TestableEntity, Location) -> LivingEntity?,
    private val dispatch: MinionTriggerDispatch,
    private val events: EventDispatcher,
    private val experience: ExperienceSink,
    private val spawnedByKey: NamespacedKey,
    private val ownerOf: (Minion) -> Player? = { it.ownerPlayer() },
    private val listening: (HandlerList) -> Boolean = { it.registeredListeners.isNotEmpty() },
    private val rewardOf: (LivingEntity) -> Int = { (it as? Mob)?.possibleExperienceReward ?: 0 },
    private val random: Random = Random.Default,
    private val canWork: (Minion, Block) -> Boolean = { _, _ -> true }
) : MinionActivity {

    private val pendingDeaths: MutableMap<UUID, Minion> = ConcurrentHashMap()

    override fun onTick(minion: Minion, now: Long) {
        val target = minion.mining.pendingKill ?: return
        if (now < minion.mining.breakAt) {
            return
        }
        minion.mining.pendingKill = null
        if (!target.isValid || target.isDead) {
            return
        }
        pendingDeaths[target.uniqueId] = minion
        if (minion.state.definition.slayer?.creditOwner == true) {
            ownerOf(minion)?.let(target::setKiller)
        }
        target.health = 0.0
    }

    override fun act(minion: Minion, now: Long): ActivityResult {
        val settings = minion.state.definition.slayer ?: return ActivityResult.BLOCKED
        val target = targets(minion, settings).randomOrNull(random)
        if (target != null) {
            minion.mining.pendingKill = target
            minion.mining.breakAt = now + minion.stats.efficiencyTicks
            return ActivityResult.WORKED
        }

        minion.blockedReason = NO_MOBS
        if (!minion.state.definition.placement.selfSeed) {
            return ActivityResult.BLOCKED
        }
        if (minion.mining.spawnedMobs.size < maxMobs(minion, settings) && spawn(minion, settings)) {
            return ActivityResult.SEEDED
        }
        return ActivityResult.BUSY
    }

    override fun hasWork(minion: Minion): Boolean {
        val settings = minion.state.definition.slayer ?: return false
        return minion.state.definition.placement.selfSeed || targets(minion, settings).isNotEmpty()
    }

    override fun offlineCycle(minion: Minion): OfflineYield? =
        minion.state.killSample.randomOrNull(random)?.let { record ->
            OfflineYield(record.drops.map { it.clone() }, record.experience)
        }

    /** @return true if this was a minion kill, which this call has now stored. */
    fun onDeath(event: EntityDeathEvent): Boolean {
        val entity = event.entity
        val minion = pendingDeaths.remove(entity.uniqueId) ?: return false
        minion.mining.spawnedMobs -= entity.uniqueId
        val settings = minion.state.definition.slayer ?: return true

        val configured = settings.drops.takeUnless { it.isEmpty }?.roll(random)
        val kill = MinionKillEvent(
            minion,
            entity,
            (configured?.drops ?: event.drops.map { it.clone() }).toMutableList(),
            configured?.experience ?: event.droppedExp.takeIf { it > 0 } ?: rewardOf(entity)
        )
        event.drops.clear()
        event.droppedExp = 0
        if (listening(MinionKillEvent.getHandlerList()) && !events.dispatch(kill)) {
            return true
        }

        val result = dispatch.drops(
            minion = minion,
            trigger = TriggerMinionKillMob,
            drops = kill.drops,
            block = null,
            location = entity.location,
            cause = DropCause.ENTITY,
            entity = entity
        )
        dispatch.storeAndDispatchCollect(minion, result.results.map { (item, _) -> item })
        experience.add(minion, kill.experience + result.results.sumOf { it.xp } + result.contributedExperience)
        minion.state = minion.state.withKillSample(
            KillSample.append(minion.state.killSample, KillRecord(kill.drops.map { it.clone() }, kill.experience))
        )
        return true
    }

    /** Rebuilds [minion]'s live count from the tagged mobs in its box. */
    fun recount(minion: Minion) {
        if (minion.state.definition.type != MinionType.SLAYER) {
            return
        }
        val own = minion.uuid.toString()
        minion.mining.spawnedMobs.clear()
        minion.mining.spawnedMobs += entities.find(minion, LivingEntity::class.java)
            .filter { it.persistentDataContainer.get(spawnedByKey, PersistentDataType.STRING) == own }
            .map { it.uniqueId }
    }

    /** Drops a tagged mob that died or left the world some other way, so its slayer spawns again. */
    fun forget(entity: Entity, minionOf: (UUID) -> Minion?) {
        if (entity.uniqueId in pendingDeaths) {
            return
        }
        spawnerOf(entity, minionOf)?.mining?.spawnedMobs?.remove(entity.uniqueId)
    }

    /** Counts a tagged mob that came back into the world with its chunk. */
    fun track(entity: Entity, minionOf: (UUID) -> Minion?) {
        spawnerOf(entity, minionOf)?.mining?.spawnedMobs?.add(entity.uniqueId)
    }

    fun isSpawned(entity: Entity): Boolean = entity.persistentDataContainer.has(spawnedByKey)

    private fun spawnerOf(entity: Entity, minionOf: (UUID) -> Minion?): Minion? =
        entity.persistentDataContainer.get(spawnedByKey, PersistentDataType.STRING)
            ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?.let(minionOf)

    private fun targets(minion: Minion, settings: SlayerSettings): List<LivingEntity> =
        entities.find(minion, LivingEntity::class.java).filter { isTarget(minion, it, settings) }

    private fun isTarget(minion: Minion, entity: LivingEntity, settings: SlayerSettings): Boolean {
        if (!entity.isValid || entity.isDead || entity.uniqueId in pendingDeaths) {
            return false
        }
        if (entity is Player || entity is ArmorStand || (entity is Tameable && entity.ownerUniqueId != null)) {
            return false
        }
        val spawnedBy = entity.persistentDataContainer.get(spawnedByKey, PersistentDataType.STRING)
        if (spawnedBy != null && spawnedBy != minion.uuid.toString()) {
            return false
        }
        if (spawnedBy == null && entity.customName() != null) {
            return false
        }
        return settings.targets.any { it.matches(entity) } && canWork(minion, entity.location.block)
    }

    private fun maxMobs(minion: Minion, settings: SlayerSettings): Int =
        minion.state.definition.baseStats.evaluate(settings.maxMobs, minion.level).coerceAtLeast(1)

    private fun spawn(minion: Minion, settings: SlayerSettings): Boolean {
        val spot = MinionWorkArea.blocks(minion).filter { isOpenSpot(it) && canWork(minion, it) }.randomOrNull(random) ?: return false
        val mob = spawner(settings.targets.random(random), spot.location.add(0.5, 0.0, 0.5)) ?: return false
        mob.setAI(false)
        mob.isPersistent = true
        mob.removeWhenFarAway = false
        mob.persistentDataContainer.set(spawnedByKey, PersistentDataType.STRING, minion.uuid.toString())
        minion.mining.spawnedMobs += mob.uniqueId
        return true
    }

    private fun isOpenSpot(block: Block): Boolean =
        block.isPassable &&
            block.world.getBlockAt(block.x, block.y + 1, block.z).isPassable &&
            block.world.getBlockAt(block.x, block.y - 1, block.z).isSolid

    private companion object {
        const val NO_MOBS = "no-mobs"
    }
}
