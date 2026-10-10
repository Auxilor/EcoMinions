package com.exanthiax.ecominions.minions.model

import com.exanthiax.ecominions.api.minion.MinionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MinionDefinitionFloorTest {
    @Test
    fun `block types keep their reach under the new floor`() {
        assertEquals(-1, MinionDefinition.defaultFloorOffset(MinionType.MINER))
        assertEquals(0, MinionDefinition.defaultFloorOffset(MinionType.FARMER))
        assertEquals(0, MinionDefinition.defaultFloorOffset(MinionType.WOODCUTTER))
        assertEquals("1", MinionDefinition.defaultHeight(MinionType.MINER))
        assertEquals("1", MinionDefinition.defaultHeight(MinionType.FARMER))
        assertEquals("5", MinionDefinition.defaultHeight(MinionType.WOODCUTTER))
    }

    @Test
    fun `collector box starts below the minion and is three high`() {
        assertEquals(-1, MinionDefinition.defaultFloorOffset(MinionType.COLLECTOR))
        assertEquals("3", MinionDefinition.defaultHeight(MinionType.COLLECTOR))
    }
}
