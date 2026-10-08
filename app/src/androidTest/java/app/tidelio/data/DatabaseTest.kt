package app.tidelio.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.tidelio.data.local.GoalChangeEntity
import app.tidelio.data.local.TidelioDatabase
import app.tidelio.data.repository.EntryRepository
import app.tidelio.domain.entries.WaterEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Instrumented persistence tests (run with ./gradlew connectedDebugAndroidTest on a device/emulator). */
@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: TidelioDatabase
    private lateinit var repo: EntryRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TidelioDatabase::class.java).build()
        repo = EntryRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun entry(amount: Int, date: LocalDate, time: LocalTime) = WaterEntry(
        id = 0, amountMl = amount, timestamp = Instant.parse("2026-10-07T10:00:00Z"),
        localDate = date, localTime = time, zoneId = "Europe/Minsk",
        createdAt = Instant.parse("2026-10-07T10:00:00Z"), updatedAt = Instant.parse("2026-10-07T10:00:00Z"),
    )

    @Test
    fun insertQueryDeleteRestore() = runTest {
        val d = LocalDate.of(2026, 10, 7)
        val id = repo.insert(entry(250, d, LocalTime.of(9, 0)))
        repo.insert(entry(500, d.minusDays(1), LocalTime.of(20, 0)))
        assertEquals(1, repo.observeDay(d).first().size)
        val totals = repo.observeDailyTotals(d.minusDays(1), d).first()
        assertEquals(750L, totals.sumOf { it.totalMl })

        val removed = repo.delete(id)!!
        assertTrue(repo.observeDay(d).first().isEmpty())
        repo.restore(removed)
        repo.restore(removed) // second restore is a no-op
        assertEquals(listOf(removed), repo.observeDay(d).first())
    }

    @Test
    fun goalReplaceKeepsOneRecordPerDate() = runTest {
        val dao = db.goalDao()
        val day = LocalDate.of(2026, 10, 7).toEpochDay()
        dao.replaceForDate(GoalChangeEntity(effectiveLocalDate = day - 30, goalMl = 2000, createdAt = 1))
        dao.replaceForDate(GoalChangeEntity(effectiveLocalDate = day, goalMl = 2500, createdAt = 2))
        dao.replaceForDate(GoalChangeEntity(effectiveLocalDate = day, goalMl = 2600, createdAt = 3))
        val all = dao.getAll()
        assertEquals(2, all.size)
        assertEquals(2000, all.first().goalMl)
        assertEquals(2600, all.last().goalMl)
    }

    @Test
    fun resetRemovesEverything() = runTest {
        repo.insert(entry(250, LocalDate.of(2026, 10, 7), LocalTime.of(9, 0)))
        db.goalDao().replaceForDate(GoalChangeEntity(effectiveLocalDate = 1, goalMl = 2000, createdAt = 1))
        db.withTransaction {
            db.entryDao().deleteAll()
            db.goalDao().deleteAll()
        }
        assertEquals(0, db.entryDao().count())
        assertTrue(db.goalDao().getAll().isEmpty())
        assertNull(db.entryDao().get(1))
    }
}
