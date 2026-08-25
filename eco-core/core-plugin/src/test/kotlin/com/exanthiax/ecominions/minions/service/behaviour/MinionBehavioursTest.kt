package com.exanthiax.ecominions.minions.service.behaviour

import com.exanthiax.ecominions.api.minion.MinionType
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class MinionBehavioursTest {

    private val miner = mockk<MinerBehaviour>()
    private val woodcutter = mockk<WoodcutterBehaviour>()
    private val farmer = mockk<FarmerBehaviour>()
    private val behaviours = MinionBehaviours(miner, woodcutter, farmer)

    @Test
    fun `maps each type to its behaviour`() {
        assertSame(miner, behaviours[MinionType.MINER])
        assertSame(woodcutter, behaviours[MinionType.WOODCUTTER])
        assertSame(farmer, behaviours[MinionType.FARMER])
    }

    @Test
    fun `every type has a behaviour`() {
        for (type in MinionType.entries) {
            assertNotNull(behaviours[type], "no behaviour for $type")
        }
    }
}
