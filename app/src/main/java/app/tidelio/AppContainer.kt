package app.tidelio

import android.content.Context
import androidx.room.withTransaction
import app.tidelio.data.local.TidelioDatabase
import app.tidelio.data.repository.EntryRepository
import app.tidelio.data.repository.GoalRepository
import app.tidelio.data.repository.PreferencesRepository
import app.tidelio.domain.entries.DuplicateActionGuard
import app.tidelio.domain.entries.EntryEditor
import app.tidelio.domain.time.SystemTimeSource
import app.tidelio.domain.time.TimeSource
import app.tidelio.domain.time.TodayProvider
import app.tidelio.ui.common.UndoController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency injection: one container per process. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val time: TimeSource = SystemTimeSource

    val database: TidelioDatabase = TidelioDatabase.build(context)
    val entries = EntryRepository(database)
    val goals = GoalRepository(database, time)
    val preferences = PreferencesRepository(context)

    val editor = EntryEditor(entries, time)
    val quickAddGuard = DuplicateActionGuard(windowMillis = 800L, clock = time::elapsedMillis)
    val todayProvider = TodayProvider(time, appScope)
    val undo = UndoController(appScope)

    /** Removes entries, goals and preferences. Room work runs in a single transaction. */
    suspend fun clearAllData() {
        undo.clear()
        database.withTransaction {
            database.entryDao().deleteAll()
            database.goalDao().deleteAll()
        }
        preferences.clear()
    }
}
