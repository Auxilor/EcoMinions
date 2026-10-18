package com.exanthiax.ecominions.core.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoadReportTest {

    @Test
    fun `a clean load reports no violations`() {
        val report = LoadReport()
        report.recordLoaded("minion types", 15)
        report.recordLoaded("upgrades", 10)

        assertEquals("Loaded 15 minion types, 10 upgrades", report.summary())
    }

    @Test
    fun `violations are counted and surfaced`() {
        val report = LoadReport()
        report.recordLoaded("minion types", 14)
        report.recordViolation()
        report.recordViolation()

        assertEquals(2, report.violationCount)
        assertEquals("Loaded 14 minion types (2 violations - see above)", report.summary())
    }

    @Test
    fun `a single violation is singular`() {
        val report = LoadReport()
        report.recordLoaded("minion types", 14)
        report.recordViolation()

        assertEquals("Loaded 14 minion types (1 violation - see above)", report.summary())
    }

    @Test
    fun `a reload starts the counts again`() {
        val report = LoadReport()
        report.recordViolation()
        report.recordLoaded("upgrades", 3)

        report.reset()
        report.recordLoaded("upgrades", 4)

        assertEquals(0, report.violationCount)
        assertEquals("Loaded 4 upgrades", report.summary())
    }

    @Test
    fun `an empty load says so rather than printing nothing`() {
        assertEquals("Loaded nothing", LoadReport().summary())
    }
}
