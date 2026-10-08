package app.tidelio.domain

import app.tidelio.domain.statistics.WaveCalculator
import app.tidelio.testing.entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class WaveCalculatorTest {
    private val d = LocalDate.of(2026, 10, 7)

    @Test
    fun emptyDayStartsAtZero() {
        val w = WaveCalculator.build(emptyList(), 2000)
        assertTrue(w.steps.isEmpty())
        assertEquals(0L, w.totalMl)
        assertEquals(0L, w.percent)
        assertEquals(0L, w.cumulativeAt(0.99f))
        assertEquals(0f, w.heightFraction(0), 0f)
    }

    @Test
    fun cumulativeIsOrderedByTimeAndNondecreasing() {
        val entries = listOf(
            entry(500, d, LocalTime.of(18, 0)),
            entry(250, d, LocalTime.of(7, 30)),
            entry(300, d, LocalTime.of(12, 0)),
        )
        val w = WaveCalculator.build(entries, 2000)
        assertEquals(listOf(250L, 550L, 1050L), w.steps.map { it.cumulativeMl })
        w.steps.zipWithNext().forEach { (a, b) ->
            assertTrue(b.x > a.x)
            assertTrue(b.cumulativeMl >= a.cumulativeMl)
        }
    }

    @Test
    fun markersAreAtActualTimes() {
        val w = WaveCalculator.build(listOf(entry(250, d, LocalTime.of(6, 0)), entry(250, d, LocalTime.of(18, 0))), null)
        assertEquals(0.25f, w.steps[0].x, 1e-6f)
        assertEquals(0.75f, w.steps[1].x, 1e-6f)
    }

    @Test
    fun noIntakeIsImpliedBeforeFirstEntry() {
        val w = WaveCalculator.build(listOf(entry(400, d, LocalTime.of(10, 0))), 2000)
        assertEquals(0L, w.cumulativeAt(WaveCalculator.xOf(LocalTime.of(9, 59, 59))))
        assertEquals(400L, w.cumulativeAt(WaveCalculator.xOf(LocalTime.of(10, 0))))
    }

    @Test
    fun identicalTimestampsAggregateIntoOneRise() {
        val t = LocalTime.of(8, 0)
        val w = WaveCalculator.build(listOf(entry(200, d, t), entry(300, d, t), entry(100, d, LocalTime.of(9, 0))), 2000)
        assertEquals(2, w.steps.size)
        assertEquals(500L, w.steps[0].addedMl)
        assertEquals(2, w.steps[0].entryCount)
        assertEquals(600L, w.steps[1].cumulativeMl)
    }

    @Test
    fun overflowKeepsActualTotalsButCapsDrawingHeight() {
        val w = WaveCalculator.build(listOf(entry(1200, d, LocalTime.of(9, 0)), entry(1000, d, LocalTime.of(15, 0))), 2000)
        assertEquals(2200L, w.totalMl)
        assertEquals(110L, w.percent)
        assertEquals(200L, w.overflowMl)
        assertTrue(w.goalReached)
        assertEquals(1f, w.heightFraction(w.totalMl), 0f)
        assertEquals(0.6f, w.heightFraction(1200), 1e-6f)
    }

    @Test
    fun percentIsFlooredSoItNeverClaimsTheGoalEarly() {
        val w = WaveCalculator.build(listOf(entry(1999, d, LocalTime.of(9, 0))), 2000)
        assertEquals(99L, w.percent)
        assertFalse(w.goalReached)
    }

    @Test
    fun missingGoalUsesLabelledVolumeScaleAndNoPercent() {
        val w = WaveCalculator.build(listOf(entry(1234, d, LocalTime.of(9, 0))), null)
        assertNull(w.percent)
        assertEquals(0L, w.overflowMl)
        assertEquals(1500L, w.scaleMaxMl)
        assertFalse(w.hasGoal)
        assertEquals(500L, WaveCalculator.volumeScale(0))
        assertEquals(500L, WaveCalculator.volumeScale(500))
        assertEquals(1000L, WaveCalculator.volumeScale(501))
    }
}
