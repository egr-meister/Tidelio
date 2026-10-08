package app.tidelio.domain

import app.tidelio.domain.time.DateTimeResolver
import app.tidelio.domain.time.epochDayRange
import app.tidelio.domain.time.millisUntilNextMidnight
import app.tidelio.domain.time.monthGrid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

class TimeAndCalendarTest {

    @Test
    fun leapYearFebruaryHas29Days() {
        val grid = monthGrid(YearMonth.of(2028, 2), DayOfWeek.MONDAY)
        val days = grid.filterNotNull()
        assertEquals(29, days.size)
        assertEquals(LocalDate.of(2028, 2, 29), days.last())
        assertEquals(0, grid.size % 7)
        // Feb 1, 2028 is a Tuesday -> one leading blank with Monday first.
        assertNull(grid[0])
        assertEquals(LocalDate.of(2028, 2, 1), grid[1])
    }

    @Test
    fun nonLeapFebruaryHas28Days() {
        assertEquals(28, monthGrid(YearMonth.of(2027, 2), DayOfWeek.SUNDAY).count { it != null })
        assertEquals(28, YearMonth.of(2027, 2).epochDayRange().count())
    }

    @Test
    fun monthBoundariesAndFirstDayOfWeek() {
        // Oct 1, 2026 is a Thursday.
        val mon = monthGrid(YearMonth.of(2026, 10), DayOfWeek.MONDAY)
        assertEquals(3, mon.indexOfFirst { it != null })
        val sun = monthGrid(YearMonth.of(2026, 10), DayOfWeek.SUNDAY)
        assertEquals(4, sun.indexOfFirst { it != null })
        val range = YearMonth.of(2026, 12).epochDayRange()
        assertEquals(LocalDate.of(2026, 12, 1).toEpochDay(), range.first)
        assertEquals(LocalDate.of(2026, 12, 31).toEpochDay(), range.last)
        assertEquals(LocalDate.of(2027, 1, 1), LocalDate.ofEpochDay(range.last + 1))
    }

    @Test
    fun midnightRolloverDelay() {
        val zone = ZoneId.of("Europe/Minsk")
        val now = LocalDateTime.of(2026, 10, 7, 23, 59, 30).atZone(zone).toInstant()
        assertEquals(30_000L, millisUntilNextMidnight(now, zone))
    }

    @Test
    fun midnightDelayOnDayStartingAfterDstGap() {
        // Sao Paulo, 2018-11-04: clocks jumped from 00:00 to 01:00, so the day started at 01:00.
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = LocalDateTime.of(2018, 11, 3, 23, 0).atZone(zone).toInstant()
        assertEquals(60 * 60 * 1000L, millisUntilNextMidnight(now, zone))
    }

    @Test
    fun timeZoneChangeAffectsTodayButNotStoredDates() {
        val instant = LocalDateTime.of(2026, 10, 7, 22, 30).atZone(ZoneId.of("Europe/Minsk")).toInstant()
        assertEquals(LocalDate.of(2026, 10, 7), instant.atZone(ZoneId.of("Europe/Minsk")).toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 8), instant.atZone(ZoneId.of("Asia/Tokyo")).toLocalDate())
    }

    @Test
    fun ambiguousFallBackTimeResolvesToEarlierInstant() {
        val ny = ZoneId.of("America/New_York")
        val r = DateTimeResolver.resolve(LocalDate.of(2026, 11, 1), LocalTime.of(1, 30), ny)
        assertTrue(r is DateTimeResolver.Resolution.Valid)
        r as DateTimeResolver.Resolution.Valid
        assertTrue(r.wasAmbiguous)
        assertEquals(ZoneOffset.ofHours(-4), r.dateTime.offset) // EDT, before the transition
        assertEquals(LocalTime.of(1, 30), r.dateTime.toLocalTime())
    }

    @Test
    fun gapTimeIsNonexistent() {
        val ny = ZoneId.of("America/New_York")
        val r = DateTimeResolver.resolve(LocalDate.of(2026, 3, 8), LocalTime.of(2, 15), ny)
        assertTrue(r is DateTimeResolver.Resolution.Nonexistent)
    }

    @Test
    fun ordinaryTimeIsNotAmbiguous() {
        val r = DateTimeResolver.resolve(LocalDate.of(2026, 10, 7), LocalTime.of(9, 0), ZoneId.of("Europe/Minsk"))
        assertFalse((r as DateTimeResolver.Resolution.Valid).wasAmbiguous)
    }
}
