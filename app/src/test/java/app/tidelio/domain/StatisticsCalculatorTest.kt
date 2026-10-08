package app.tidelio.domain

import app.tidelio.domain.entries.DayPeriod
import app.tidelio.domain.goals.GoalChange
import app.tidelio.domain.goals.GoalTimeline
import app.tidelio.domain.statistics.StatisticsCalculator
import app.tidelio.testing.entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class StatisticsCalculatorTest {
    private val end = LocalDate.of(2026, 10, 7)

    @Test
    fun missingRecordDaysAreNotTreatedAsZero() {
        val entries = listOf(
            entry(1000, end, LocalTime.of(9, 0)),
            entry(2000, end.minusDays(3), LocalTime.of(13, 0)),
        )
        val r = StatisticsCalculator.compute(end, 7, entries, GoalTimeline.EMPTY)
        assertEquals(7, r.days.size)
        assertEquals(end.minusDays(6), r.startDate)
        assertEquals(2, r.daysWithRecords)
        assertEquals(3000L, r.totalMl)
        // Average over recorded days only: 3000 / 2, not 3000 / 7.
        assertEquals(1500L, r.averageOnDaysWithEntriesMl)
        assertNull(r.days.first().totalMl)
        assertTrue(r.days.count { it.totalMl == null } == 5)
    }

    @Test
    fun goalAchievementCountsOnlyRecordedDaysWithAGoal() {
        val goals = GoalTimeline(listOf(GoalChange(end.minusDays(2), 1500)))
        val entries = listOf(
            entry(3000, end.minusDays(4), LocalTime.of(9, 0)), // recorded, no goal yet
            entry(1600, end.minusDays(2), LocalTime.of(9, 0)), // goal met
            entry(1000, end, LocalTime.of(9, 0)), // goal not met
        )
        val r = StatisticsCalculator.compute(end, 7, entries, goals)
        assertEquals(2, r.recordedDaysWithGoal)
        assertEquals(1, r.goalMetDays)
    }

    @Test
    fun periodDistributionUsesRecordedLocalTime() {
        val entries = listOf(
            entry(100, end, LocalTime.of(0, 0)),
            entry(300, end, LocalTime.of(12, 0)),
            entry(600, end.minusDays(1), LocalTime.of(23, 59)),
        )
        val r = StatisticsCalculator.compute(end, 7, entries, GoalTimeline.EMPTY)
        assertEquals(100L, r.periodTotals[DayPeriod.MORNING])
        assertEquals(300L, r.periodTotals[DayPeriod.DAY])
        assertEquals(600L, r.periodTotals[DayPeriod.EVENING])
        assertEquals(60.0, r.periodShare(DayPeriod.EVENING)!!, 1e-9)
    }

    @Test
    fun emptyRangeHasNoAverageOrShares() {
        val r = StatisticsCalculator.compute(end, 30, emptyList(), GoalTimeline.EMPTY)
        assertEquals(30, r.days.size)
        assertEquals(0, r.daysWithRecords)
        assertNull(r.averageOnDaysWithEntriesMl)
        assertNull(r.periodShare(DayPeriod.DAY))
    }

    @Test
    fun entriesOutsideRangeAreIgnored() {
        val entries = listOf(entry(999, end.minusDays(7), LocalTime.of(9, 0)), entry(1, end.plusDays(1), LocalTime.of(9, 0)))
        val r = StatisticsCalculator.compute(end, 7, entries, GoalTimeline.EMPTY)
        assertEquals(0L, r.totalMl)
    }

    @Test
    fun groupingUsesStoredLocalDateNotCurrentZone() {
        // Logged at 23:30 on Oct 6 in Minsk. Absolute instant is already Oct 7 in Tokyo,
        // but the entry stays on Oct 6 because grouping uses the stored local date.
        val e = entry(500, end.minusDays(1), LocalTime.of(23, 30), zone = ZoneId.of("Europe/Minsk"))
        val r = StatisticsCalculator.compute(end, 7, listOf(e), GoalTimeline.EMPTY)
        assertEquals(500L, r.days.single { it.date == end.minusDays(1) }.totalMl)
        assertNull(r.days.single { it.date == end }.totalMl)
        assertEquals(DayPeriod.EVENING, e.period)
    }
}
