package com.exanthiax.ecominions.minions.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KillSampleTest {
    @Test
    fun `the sample keeps the newest ten`() {
        val sample = (1..12).fold(emptyList<KillRecord>()) { kept, experience ->
            KillSample.append(kept, KillRecord(emptyList(), experience))
        }

        assertEquals((3..12).toList(), sample.map { it.experience })
    }
}
