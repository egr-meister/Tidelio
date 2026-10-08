package app.tidelio.domain

import app.tidelio.domain.entries.AmountParse
import app.tidelio.domain.goals.GoalApplyFrom
import app.tidelio.domain.goals.GoalChange
import app.tidelio.domain.goals.GoalTimeline
import app.tidelio.domain.goals.effectiveDate
import app.tidelio.domain.goals.parseGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalTimelineTest {
    private val sep1 = LocalDate.of(2026, 9, 1)
    private val oct1 = LocalDate.of(2026, 10, 1)
    private val timeline = GoalTimeline(listOf(GoalChange(oct1, 2500), GoalChange(sep1, 2000)))

    @Test
    fun dayUsesLatestGoalEffectiveOnOrBeforeIt() {
        assertEquals(2000, timeline.goalFor(sep1))
        assertEquals(2000, timeline.goalFor(LocalDate.of(2026, 9, 30)))
        assertEquals(2500, timeline.goalFor(oct1))
        assertEquals(2500, timeline.goalFor(LocalDate.of(2027, 1, 1)))
    }

    @Test
    fun daysBeforeFirstGoalHaveNoGoal() {
        assertNull(timeline.goalFor(LocalDate.of(2026, 8, 31)))
        assertNull(GoalTimeline.EMPTY.goalFor(oct1))
    }

    @Test
    fun applyingFromTomorrowDoesNotChangeToday() {
        val today = LocalDate.of(2026, 10, 7)
        val updated = GoalTimeline(timeline.changes + GoalChange(GoalApplyFrom.TOMORROW.effectiveDate(today), 1800))
        assertEquals(2500, updated.goalFor(today))
        assertEquals(1800, updated.goalFor(today.plusDays(1)))
        assertEquals(2000, updated.goalFor(sep1))
        assertEquals(LocalDate.of(2026, 10, 8), updated.nextChangeAfter(today)!!.effectiveDate)
    }

    @Test
    fun applyingFromTodayChangesTodayButNotEarlierDates() {
        val today = LocalDate.of(2026, 10, 7)
        val updated = GoalTimeline(timeline.changes + GoalChange(GoalApplyFrom.TODAY.effectiveDate(today), 3000))
        assertEquals(3000, updated.goalFor(today))
        assertEquals(2500, updated.goalFor(today.minusDays(1)))
    }

    @Test
    fun oneRecordPerDateLastOneWins() {
        val t = GoalTimeline(listOf(GoalChange(oct1, 2000), GoalChange(oct1, 2200)))
        assertEquals(1, t.changes.size)
        assertEquals(2200, t.goalFor(oct1))
    }

    @Test
    fun goalRangeIsValidated() {
        assertTrue(parseGoal("100") is AmountParse.Valid)
        assertTrue(parseGoal("10,000") is AmountParse.Valid)
        assertTrue(parseGoal("99") is AmountParse.Invalid)
        assertTrue(parseGoal("10001") is AmountParse.Invalid)
        assertTrue(parseGoal("2000.5") is AmountParse.Invalid)
    }
}
