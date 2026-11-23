package com.exanthiax.ecominions.core.error

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import wiki.ecohub.config.ViolationContext

class LoadContextTest {

    @Test
    fun `exposes the violation context libreforge compilation needs`() {
        val violations = mockk<ViolationContext>()
        val context = LoadContext(violations, LoadReport())

        assertSame(violations, context.violationContext)
    }
}
