package app.tidelio.domain.goals

import app.tidelio.domain.entries.AmountParse
import app.tidelio.domain.entries.parseWholeMl
import java.time.LocalDate

/** The user chooses the goal. The range is an input limit, not a recommendation. */
object GoalLimits {
    const val MIN_ML = 100
    const val MAX_ML = 10_000

    /** Editable examples offered during setup — explicitly not personalized recommendations. */
    val EXAMPLES = listOf(1_500, 2_000, 2_500)
}

fun parseGoal(text: String): AmountParse = parseWholeMl(text, GoalLimits.MIN_ML, GoalLimits.MAX_ML)

/** One effective-dated goal record: applies from [effectiveDate] until the next record. */
data class GoalChange(val effectiveDate: LocalDate, val goalMl: Int)

enum class GoalApplyFrom { TODAY, TOMORROW }

fun GoalApplyFrom.effectiveDate(today: LocalDate): LocalDate = when (this) {
    GoalApplyFrom.TODAY -> today
    GoalApplyFrom.TOMORROW -> today.plusDays(1)
}

/**
 * Effective-dated goal history. A day uses the latest goal effective on or before it.
 * Days before the first record have no goal.
 */
class GoalTimeline(changes: List<GoalChange>) {
    private val sorted: List<GoalChange> =
        changes.groupBy { it.effectiveDate }.map { (_, sameDay) -> sameDay.last() }.sortedBy { it.effectiveDate }

    val changes: List<GoalChange> get() = sorted

    fun goalFor(date: LocalDate): Int? {
        var lo = 0
        var hi = sorted.size - 1
        var found: GoalChange? = null
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val change = sorted[mid]
            if (!change.effectiveDate.isAfter(date)) {
                found = change
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return found?.goalMl
    }

    /** The first change effective strictly after [date], if any (e.g. a goal scheduled for tomorrow). */
    fun nextChangeAfter(date: LocalDate): GoalChange? = sorted.firstOrNull { it.effectiveDate.isAfter(date) }

    companion object {
        val EMPTY = GoalTimeline(emptyList())
    }
}
