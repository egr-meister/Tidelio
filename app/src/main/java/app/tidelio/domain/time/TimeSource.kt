package app.tidelio.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Injectable clock and time-zone provider. Production code uses [SystemTimeSource];
 * tests supply fixed or mutable implementations.
 */
interface TimeSource {
    fun now(): Instant
    fun zone(): ZoneId

    /** Monotonic milliseconds, used only for duplicate-tap protection. */
    fun elapsedMillis(): Long
}

object SystemTimeSource : TimeSource {
    override fun now(): Instant = Instant.now()
    override fun zone(): ZoneId = ZoneId.systemDefault()
    override fun elapsedMillis(): Long = System.nanoTime() / 1_000_000L
}

fun TimeSource.today(): LocalDate = now().atZone(zone()).toLocalDate()

fun TimeSource.localNow(): LocalDateTime = now().atZone(zone()).toLocalDateTime()

/**
 * Milliseconds from [now] until the next local midnight in [zone].
 * Uses [LocalDate.atStartOfDay] with the zone so days that do not start at 00:00
 * (rare DST transitions at midnight) are still handled correctly.
 */
fun millisUntilNextMidnight(now: Instant, zone: ZoneId): Long {
    val today = now.atZone(zone).toLocalDate()
    val nextStart: ZonedDateTime = today.plusDays(1).atStartOfDay(zone)
    return (nextStart.toInstant().toEpochMilli() - now.toEpochMilli()).coerceAtLeast(1L)
}
