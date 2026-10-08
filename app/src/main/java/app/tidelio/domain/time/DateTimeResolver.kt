package app.tidelio.domain.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Converts a user-entered local date and time into an absolute instant.
 *
 * Daylight-saving rules (documented in the README):
 * - **Nonexistent** local times (inside a spring-forward gap, e.g. 02:30 when clocks jump
 *   from 02:00 to 03:00) are rejected with a helpful message. They are never shifted silently.
 * - **Ambiguous** local times (inside a fall-back overlap, e.g. 01:30 occurring twice) resolve
 *   deterministically to the **earlier** instant, i.e. the offset that was in force before the
 *   transition. The entry's stored local date and time stay exactly as the user entered them,
 *   so grouping and period classification are not affected by this choice.
 */
object DateTimeResolver {

    sealed interface Resolution {
        data class Valid(val dateTime: ZonedDateTime, val wasAmbiguous: Boolean) : Resolution
        data class Nonexistent(val message: String) : Resolution
    }

    fun resolve(date: LocalDate, time: LocalTime, zone: ZoneId): Resolution {
        val local = LocalDateTime.of(date, time)
        val offsets = zone.rules.getValidOffsets(local)
        return when (offsets.size) {
            0 -> Resolution.Nonexistent(
                "This time does not exist on this date because the clocks moved forward. " +
                    "Please choose a time after the change.",
            )
            1 -> Resolution.Valid(ZonedDateTime.ofStrict(local, offsets[0], zone), wasAmbiguous = false)
            else -> {
                // The larger offset is the one before the fall-back transition = earlier instant.
                val earlier = offsets.maxByOrNull { it.totalSeconds }!!
                Resolution.Valid(ZonedDateTime.ofStrict(local, earlier, zone), wasAmbiguous = true)
            }
        }
    }
}
