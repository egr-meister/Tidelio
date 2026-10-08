package app.tidelio.domain.statistics

import app.tidelio.domain.entries.DayPeriod
import app.tidelio.domain.entries.WaterEntry
import app.tidelio.domain.goals.GoalTimeline
import java.time.LocalDate

/** One date in a statistics range. [totalMl] is null when nothing was recorded (not "zero intake"). */
data class DayRecord(
    val date: LocalDate,
    val totalMl: Long?,
    val goalMl: Int?,
) {
    val hasRecords: Boolean get() = totalMl != null
    val goalMet: Boolean get() = totalMl != null && goalMl != null && totalMl >= goalMl
}

data class StatisticsResult(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val days: List<DayRecord>,
    val totalMl: Long,
    val daysWithRecords: Int,
    /** total / days with records; null when there are no records. */
    val averageOnDaysWithEntriesMl: Long?,
    val periodTotals: Map<DayPeriod, Long>,
    /** Recorded days that had a goal and reached it. */
    val goalMetDays: Int,
    /** Recorded days that had a goal (denominator for goal achievement). */
    val recordedDaysWithGoal: Int,
) {
    val hasAnyRecords: Boolean get() = daysWithRecords > 0

    fun periodShare(period: DayPeriod): Double? =
        if (totalMl > 0) (periodTotals[period] ?: 0L) * 100.0 / totalMl else null
}

object StatisticsCalculator {

    /**
     * @param endDate last date of the range (the current date)
     * @param rangeDays 7 or 30
     * @param entries entries whose local date falls in the range (others are ignored)
     */
    fun compute(
        endDate: LocalDate,
        rangeDays: Int,
        entries: List<WaterEntry>,
        goals: GoalTimeline,
    ): StatisticsResult {
        require(rangeDays >= 1)
        val start = endDate.minusDays((rangeDays - 1).toLong())
        val inRange = entries.filter { !it.localDate.isBefore(start) && !it.localDate.isAfter(endDate) }
        val byDate = inRange.groupBy { it.localDate }
        val days = (0 until rangeDays).map { offset ->
            val date = start.plusDays(offset.toLong())
            val list = byDate[date]
            DayRecord(
                date = date,
                totalMl = list?.sumOf { it.amountMl.toLong() },
                goalMl = goals.goalFor(date),
            )
        }
        val recorded = days.filter { it.hasRecords }
        val total = recorded.sumOf { it.totalMl ?: 0L }
        val periodTotals = DayPeriod.entries.associateWith { period ->
            inRange.filter { it.period == period }.sumOf { it.amountMl.toLong() }
        }
        val withGoal = recorded.filter { it.goalMl != null }
        return StatisticsResult(
            startDate = start,
            endDate = endDate,
            days = days,
            totalMl = total,
            daysWithRecords = recorded.size,
            averageOnDaysWithEntriesMl = if (recorded.isEmpty()) null else (total + recorded.size / 2) / recorded.size,
            periodTotals = periodTotals,
            goalMetDays = withGoal.count { it.goalMet },
            recordedDaysWithGoal = withGoal.size,
        )
    }
}
