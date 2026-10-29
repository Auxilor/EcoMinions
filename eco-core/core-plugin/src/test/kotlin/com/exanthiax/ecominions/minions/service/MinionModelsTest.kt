package com.exanthiax.ecominions.minions.service

import com.exanthiax.ecominions.minions.MinionDefinitionFixture
import com.exanthiax.ecominions.minions.integration.model.MinionModelProvider
import com.exanthiax.ecominions.minions.model.MinionModel
import com.exanthiax.ecominions.minions.model.MinionState
import com.exanthiax.ecominions.minions.model.MinionStats
import com.exanthiax.ecominions.minions.model.MinionStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bukkit.entity.ArmorStand
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MinionModelsTest {
    private val model = MinionModel(
        provider = MinionModel.Provider.MODEL_ENGINE,
        id = "robot",
        idleAnimation = "idle",
        workingAnimation = "mine",
        fullAnimation = null,
        offlineAnimation = "sleep",
        blockedAnimation = null
    )

    private val provider = mockk<MinionModelProvider>(relaxed = true) {
        every { provider } returns MinionModel.Provider.MODEL_ENGINE
        every { attach(any(), any()) } returns true
    }

    private val models = MinionModels().apply { register(provider) }

    private fun minion(model: MinionModel? = this.model): Minion {
        val definition = MinionDefinitionFixture.build(model = model)
        val stats = MinionStats(definition.baseStats, 1)
        return Minion(
            state = MinionState(definition = definition),
            entity = mockk<ArmorStand>(relaxed = true),
            storage = MinionStorage(stats.storageSize),
            stats = stats
        )
    }

    @Test
    fun `attach hides the stand`() {
        val minion = minion()

        models.attach(minion)

        assertEquals(model, minion.model)
        verify { minion.entity.isVisible = false }
        verify { minion.entity.equipment.clear() }
    }

    @Test
    fun `no model keeps the stand`() {
        val minion = minion(model = null)

        models.attach(minion)

        assertNull(minion.model)
        assertFalse(models.animate(minion, Minion.AnimationState.MINING))
        verify(exactly = 0) { minion.entity.isVisible = false }
    }

    @Test
    fun `missing plugin keeps the stand`() {
        val minion = minion(model = model.copy(provider = MinionModel.Provider.BETTER_MODEL))

        models.attach(minion)

        assertNull(minion.model)
    }

    @Test
    fun `unknown id keeps the stand`() {
        every { provider.attach(any(), "robot") } returns false
        val minion = minion()

        models.attach(minion)

        assertNull(minion.model)
        verify(exactly = 0) { minion.entity.isVisible = false }
    }

    @Test
    fun `late model attaches on animate`() {
        every { provider.attach(any(), "robot") } returns false
        val minion = minion().also(models::attach)
        every { provider.attach(any(), "robot") } returns true

        assertTrue(models.animate(minion, Minion.AnimationState.IDLE))

        assertEquals(model, minion.model)
        verify(exactly = 1) { provider.play(minion.entity, "idle") }
    }

    @Test
    fun `animation restarts on change`() {
        val minion = minion().also(models::attach)

        assertTrue(models.animate(minion, Minion.AnimationState.IDLE))
        models.animate(minion, Minion.AnimationState.BLOCKED)
        models.animate(minion, Minion.AnimationState.MINING)

        verify(exactly = 1) { provider.play(minion.entity, "idle") }
        verify(exactly = 1) { provider.stop(minion.entity, "idle") }
        verify(exactly = 1) { provider.play(minion.entity, "mine") }
        assertEquals("mine", minion.playingAnimation)
    }

    @Test
    fun `offline plays offline`() {
        val minion = minion().also(models::attach)

        models.animate(minion, Minion.AnimationState.OFFLINE)

        verify(exactly = 1) { provider.play(minion.entity, "sleep") }
        assertEquals("sleep", minion.playingAnimation)
    }

    @Test
    fun `blank blocked plays idle`() {
        val minion = minion().also(models::attach)

        models.animate(minion, Minion.AnimationState.BLOCKED)

        verify(exactly = 1) { provider.play(minion.entity, "idle") }
    }

    @Test
    fun `blank offline plays idle`() {
        val minion = minion(model = model.copy(offlineAnimation = null)).also(models::attach)

        models.animate(minion, Minion.AnimationState.OFFLINE)

        verify(exactly = 1) { provider.play(minion.entity, "idle") }
    }

    @Test
    fun `no animation stops the last`() {
        val minion = minion().also(models::attach)

        models.animate(minion, Minion.AnimationState.MINING)
        models.animate(minion, Minion.AnimationState.FULL)

        verify(exactly = 1) { provider.stop(minion.entity, "mine") }
        assertNull(minion.playingAnimation)
    }
}
