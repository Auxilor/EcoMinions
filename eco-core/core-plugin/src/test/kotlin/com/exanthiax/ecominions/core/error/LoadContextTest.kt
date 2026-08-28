package com.exanthiax.ecominions.core.error

import com.willfp.libreforge.ViolationContext
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class LoadContextTest {

    @Test
    fun `exposes the violation context libreforge compilation needs`() {
        val violations = mockk<ViolationContext>()
        val context = LoadContext(violations, LoadReport())

        assertSame(violations, context.violationContext)
    }
}
