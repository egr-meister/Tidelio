package app.tidelio.domain

import app.tidelio.domain.entries.DayPeriod
import app.tidelio.domain.entries.PeriodSummaries
import app.tidelio.testing.entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DayPeriodTest {

    @Test
    fun boundariesAreFixedAndNonOverlapping() {
        assertEquals(DayPeriod.MORNING, DayPeriod.of(LocalTime.MIDNIGHT))
        assertEquals(DayPeriod.MORNING, DayPeriod.of(LocalTime.of(11, 59, 59)))
        assertEquals(DayPeriod.DAY, DayPeriod.of(LocalTime.NOON))
        assertEquals(DayPeriod.DAY, DayPeriod.of(LocalTime.of(17, 59, 59)))
        assertEquals(DayPeriod.EVENING, DayPeriod.of(LocalTime.of(18, 0)))
        assertEquals(DayPeriod.EVENING, DayPeriod.of(LocalTime.of(23, 59, 59)))
    }

    @Test
    fun everyMinuteOfTheDayBelongsToExactlyOnePeriod() {
        val counts = mutableMapOf<DayPeriod, Int>()
        for (minute in 0 until 24 * 60) {
            val p = DayPeriod.of(LocalTime.of(minute / 60, minute % 60))
            counts[p] = (counts[p] ?: 0) + 1
            val matching = DayPeriod.entries.count { (minute / 60) in it.startHour..it.endHour }
            assertEquals(1, matching)
        }
        assertEquals(12 * 60, counts[DayPeriod.MORNING])
        assertEquals(6 * 60, counts[DayPeriod.DAY])
        assertEquals(6 * 60, counts[DayPeriod.EVENING])
    }

    @Test
    fun rangeLabels() {
        assertEquals("00:00–11:59", DayPeriod.MORNING.rangeLabel)
        assertEquals("12:00–17:59", DayPeriod.DAY.rangeLabel)
        assertEquals("18:00–23:59", DayPeriod.EVENING.rangeLabel)
    }

    @Test
    fun periodSummariesCoverEveryEntryOnceAndShareSumsTo100() {
        val d = LocalDate.of(2026, 10, 7)
        val entries = listOf(
            entry(250, d, LocalTime.of(7, 0)),
            entry(250, d, LocalTime.of(11, 59)),
            entry(500, d, LocalTime.of(12, 0)),
            entry(1000, d, LocalTime.of(21, 15)),
        )
        val s = PeriodSummaries.of(entries)
        assertEquals(entries.size, s.sumOf { it.entryCount })
        assertEquals(2000L, s.sumOf { it.volumeMl })
        assertEquals(25.0, s[0].sharePercent!!, 1e-9)
        assertEquals(25.0, s[1].sharePercent!!, 1e-9)
        assertEquals(50.0, s[2].sharePercent!!, 1e-9)
        assertEquals(100.0, s.sumOf { it.sharePercent!! }, 1e-9)
    }

    @Test
    fun zeroVolumeDayHasNoPercentages() {
        val s = PeriodSummaries.of(emptyList())
        s.forEach {
            assertEquals(0, it.entryCount)
            assertNull(it.sharePercent)
        }
    }
}
