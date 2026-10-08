package app.tidelio.domain.statistics

import app.tidelio.domain.entries.DayPeriod
import app.tidelio.domain.entries.WaterEntry
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.min

/** A rise of the wave: at [x] (fraction of the day) the cumulative volume becomes [cumulativeMl]. */
data class WaveStep(
    val x: Float,
    val time: LocalTime,
    val addedMl: Long,
    val cumulativeMl: Long,
    val entryCount: Int,
    val period: DayPeriod,
)

data class WaveModel(
    val steps: List<WaveStep>,
    val totalMl: Long,
    val goalMl: Int?,
    /** Volume represented by the full drawing height: the goal, or a labelled volume scale. */
    val scaleMaxMl: Long,
    /** floor(total / goal × 100); null without a goal. Never capped at 100. */
    val percent: Long?,
    /** Volume above the goal; 0 when below or without a goal. */
    val overflowMl: Long,
) {
    val hasGoal: Boolean get() = goalMl != null
    val goalReached: Boolean get() = goalMl != null && totalMl >= goalMl

    /** Drawing height as a fraction (0..1). Goal-relative drawing is capped at the goal line. */
    fun heightFraction(cumulativeMl: Long): Float =
        if (scaleMaxMl <= 0) 0f else min(1f, cumulativeMl.toFloat() / scaleMaxMl.toFloat())

    /** Cumulative volume at a given fraction of the day (a step function, 0 before the first entry). */
    fun cumulativeAt(x: Float): Long = steps.lastOrNull { it.x <= x }?.cumulativeMl ?: 0L
}

object WaveCalculator {

    private const val SECONDS_PER_DAY = 24 * 60 * 60

    fun xOf(time: LocalTime): Float = time.toSecondOfDay().toFloat() / SECONDS_PER_DAY

    /**
     * Cumulative volume ordered by entry time. Entries with identical local times are
     * aggregated into one rise. Starts at zero and is nondecreasing by construction
     * (amounts are always positive).
     */
    fun build(entries: List<WaterEntry>, goalMl: Int?): WaveModel {
        val grouped = entries
            .groupBy { it.localTime.withNano(0) }
            .toSortedMap()
        var cumulative = 0L
        val steps = grouped.map { (time, list) ->
            val added = list.sumOf { it.amountMl.toLong() }
            cumulative += added
            WaveStep(
                x = xOf(time),
                time = time,
                addedMl = added,
                cumulativeMl = cumulative,
                entryCount = list.size,
                period = DayPeriod.of(time),
            )
        }
        val total = cumulative
        val scale = goalMl?.toLong() ?: volumeScale(total)
        return WaveModel(
            steps = steps,
            totalMl = total,
            goalMl = goalMl,
            scaleMaxMl = scale,
            percent = goalMl?.let { if (it > 0) total * 100 / it else null },
            overflowMl = goalMl?.let { max(0L, total - it) } ?: 0L,
        )
    }

    /** Without a goal: round the day's total up to the next 500 mL (minimum 500 mL). */
    fun volumeScale(totalMl: Long): Long {
        if (totalMl <= 500) return 500
        return ((totalMl + 499) / 500) * 500
    }
}
