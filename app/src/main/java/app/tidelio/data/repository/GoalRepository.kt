package app.tidelio.data.repository

import app.tidelio.data.local.GoalChangeEntity
import app.tidelio.data.local.TidelioDatabase
import app.tidelio.domain.goals.GoalChange
import app.tidelio.domain.goals.GoalTimeline
import app.tidelio.domain.time.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class GoalRepository(
    db: TidelioDatabase,
    private val time: TimeSource,
) {
    private val dao = db.goalDao()

    val timeline: Flow<GoalTimeline> = dao.observeAll().map { rows ->
        GoalTimeline(rows.map { GoalChange(LocalDate.ofEpochDay(it.effectiveLocalDate), it.goalMl) })
    }

    /**
     * Stores a goal effective from [effectiveDate]. Earlier dates keep their goals; an existing
     * record for the same date is replaced in one transaction.
     */
    suspend fun setGoal(goalMl: Int, effectiveDate: LocalDate) {
        dao.replaceForDate(
            GoalChangeEntity(
                effectiveLocalDate = effectiveDate.toEpochDay(),
                goalMl = goalMl,
                createdAt = time.now().toEpochMilli(),
            ),
        )
    }
}
