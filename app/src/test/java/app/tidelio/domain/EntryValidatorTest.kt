package app.tidelio.domain

import app.tidelio.domain.entries.AmountParse
import app.tidelio.domain.entries.EntryValidation
import app.tidelio.domain.entries.EntryValidator
import app.tidelio.domain.entries.parseWholeMl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class EntryValidatorTest {
    private val utc = ZoneId.of("UTC")
    private val now = LocalDateTime.of(2026, 10, 7, 14, 30).atZone(utc).toInstant()

    private fun validate(amount: String, date: LocalDate, time: LocalTime, zone: ZoneId = utc) =
        EntryValidator.validate(amount, date, time, zone, now)

    @Test
    fun amountLimitsAreInclusive() {
        assertTrue(parseWholeMl("1", 1, 5000) is AmountParse.Valid)
        assertTrue(parseWholeMl("5000", 1, 5000) is AmountParse.Valid)
        assertTrue(parseWholeMl("5,000", 1, 5000) is AmountParse.Valid)
        assertTrue(parseWholeMl("0", 1, 5000) is AmountParse.Invalid)
        assertTrue(parseWholeMl("5001", 1, 5000) is AmountParse.Invalid)
        assertTrue(parseWholeMl("", 1, 5000) is AmountParse.Invalid)
        assertTrue(parseWholeMl("-5", 1, 5000) is AmountParse.Invalid)
        assertTrue(parseWholeMl("99999999999", 1, 5000) is AmountParse.Invalid)
    }

    @Test
    fun decimalsAreRejected() {
        val r = parseWholeMl("250.5", 1, 5000)
        assertTrue(r is AmountParse.Invalid)
        assertEquals("Use whole milliliters only.", (r as AmountParse.Invalid).message)
    }

    @Test
    fun validPastAndPresentEntries() {
        val r1 = validate("250", LocalDate.of(2026, 10, 7), LocalTime.of(14, 30))
        assertTrue(r1 is EntryValidation.Valid)
        val r2 = validate("250", LocalDate.of(2026, 9, 1), LocalTime.of(23, 59))
        assertTrue(r2 is EntryValidation.Valid)
        assertEquals(250, (r2 as EntryValidation.Valid).amountMl)
    }

    @Test
    fun futureDateIsRejected() {
        val r = validate("250", LocalDate.of(2026, 10, 8), LocalTime.of(8, 0))
        assertNotNull((r as EntryValidation.Invalid).errors.date)
    }

    @Test
    fun futureTimeTodayIsRejected() {
        val r = validate("250", LocalDate.of(2026, 10, 7), LocalTime.of(14, 31))
        assertEquals("Future times are not allowed.", (r as EntryValidation.Invalid).errors.time)
    }

    @Test
    fun allErrorsAreReportedTogether() {
        val r = validate("0", LocalDate.of(2026, 10, 9), LocalTime.of(8, 0)) as EntryValidation.Invalid
        assertNotNull(r.errors.amount)
        assertNotNull(r.errors.date)
    }

    @Test
    fun nonexistentDstTimeIsRejectedWithMessage() {
        val ny = ZoneId.of("America/New_York")
        // 2026-03-08 02:30 does not exist in New York (clocks jump 02:00 -> 03:00).
        val r = EntryValidator.validate(
            "250", LocalDate.of(2026, 3, 8), LocalTime.of(2, 30), ny,
            LocalDateTime.of(2026, 3, 9, 12, 0).atZone(ny).toInstant(),
        )
        val err = (r as EntryValidation.Invalid).errors.time
        assertNotNull(err)
        assertTrue(err!!.contains("does not exist"))
    }
}
