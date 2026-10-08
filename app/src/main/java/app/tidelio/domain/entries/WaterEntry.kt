package app.tidelio.domain.entries

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * One recorded amount of water.
 *
 * [localDate] and [localTime] are the wall-clock values at recording time (or as edited by
 * the user) and are the ONLY values used for grouping by date and period. [timestamp] and
 * [zoneId] preserve the absolute moment, so travel never silently moves past entries.
 */
data class WaterEntry(
    val id: Long,
    val amountMl: Int,
    val timestamp: Instant,
    val localDate: LocalDate,
    val localTime: LocalTime,
    val zoneId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val period: DayPeriod get() = DayPeriod.of(localTime)
}

enum class DayPeriod(val label: String, val startHour: Int, val endHour: Int) {
    MORNING("Morning", 0, 11),
    DAY("Day", 12, 17),
    EVENING("Evening", 18, 23),
    ;

    /** e.g. "00:00–11:59". */
    val rangeLabel: String get() = String.format(Locale.US, "%02d:00–%02d:59", startHour, endHour)

    /** Start of the period as a fraction of the day (0..1). */
    val startFraction: Float get() = startHour / 24f

    /** End of the period as a fraction of the day (0..1). */
    val endFraction: Float get() = (endHour + 1) / 24f

    companion object {
        /** Fixed, non-overlapping classification. Every local time maps to exactly one period. */
        fun of(time: LocalTime): DayPeriod = when (time.hour) {
            in 0..11 -> MORNING
            in 12..17 -> DAY
            else -> EVENING
        }
    }
}
