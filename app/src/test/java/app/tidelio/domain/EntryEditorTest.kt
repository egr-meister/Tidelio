package app.tidelio.domain

import app.tidelio.domain.entries.EntryEditor
import app.tidelio.domain.entries.OneShotUndo
import app.tidelio.domain.statistics.WaveCalculator
import app.tidelio.testing.FakeEntryStore
import app.tidelio.testing.FakeTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class EntryEditorTest {
    private val zone = ZoneId.of("Europe/Minsk")
    private val time = FakeTime.at(LocalDateTime.of(2026, 10, 7, 9, 15, 42), zone)
    private val store = FakeEntryStore()
    private val editor = EntryEditor(store, time)

    @Test
    fun quickAddCreatesOneEntryAtCurrentLocalDateTime() = runBlocking {
        val e = editor.quickAdd(250)
        assertEquals(1, store.rows.size)
        assertEquals(LocalDate.of(2026, 10, 7), e.localDate)
        assertEquals(LocalTime.of(9, 15, 42), e.localTime)
        assertEquals("Europe/Minsk", e.zoneId)
        assertEquals(time.instant, e.timestamp)
    }

    @Test
    fun editingAmountKeepsTimestampAndZone() = runBlocking {
        val e = editor.quickAdd(250)
        time.zoneId = ZoneId.of("Asia/Tokyo") // user travelled
        val dt = LocalDateTime.of(e.localDate, e.localTime).atZone(ZoneId.of(e.zoneId))
        val edited = editor.edit(e, 400, dt)
        assertEquals(400, edited.amountMl)
        assertEquals(e.timestamp, edited.timestamp)
        assertEquals("Europe/Minsk", edited.zoneId)
        assertEquals(e.localDate, edited.localDate)
    }

    @Test
    fun movingEntryBetweenDatesUpdatesBothDates() = runBlocking {
        val a = editor.quickAdd(300)
        editor.quickAdd(200)
        val oct7 = LocalDate.of(2026, 10, 7)
        val oct5 = LocalDate.of(2026, 10, 5)
        assertEquals(500L, store.onDate(oct7).sumOf { it.amountMl.toLong() })

        editor.edit(a, 300, LocalDateTime.of(oct5, LocalTime.of(19, 0)).atZone(zone))

        assertEquals(200L, store.onDate(oct7).sumOf { it.amountMl.toLong() })
        assertEquals(300L, store.onDate(oct5).sumOf { it.amountMl.toLong() })
        val wave5 = WaveCalculator.build(store.onDate(oct5), 2000)
        assertEquals(300L, wave5.totalMl)
        assertEquals(LocalTime.of(19, 0), wave5.steps.single().time)
    }

    @Test
    fun deleteThenUndoRestoresTheExactEntryOnce() = runBlocking {
        val e = editor.quickAdd(350)
        val removed = editor.delete(e.id)!!
        assertTrue(store.rows.isEmpty())

        val undo = OneShotUndo { editor.restore(removed) }
        assertTrue(undo.run())
        assertEquals(e, store.rows[e.id])
        assertFalse("undo must run only once", undo.run())
        assertEquals(1, store.rows.size)
    }

    @Test
    fun undoOfQuickAddRemovesOnlyThatEntry() = runBlocking {
        val keep = editor.quickAdd(150)
        val added = editor.quickAdd(500)
        val undo = OneShotUndo { editor.delete(added.id) }
        undo.run()
        assertNull(store.rows[added.id])
        assertEquals(keep, store.rows[keep.id])
        assertFalse(undo.run())
    }

    @Test
    fun disarmedUndoNeverRuns() = runBlocking {
        val e = editor.quickAdd(150)
        val undo = OneShotUndo { editor.delete(e.id) }
        undo.disarm()
        assertFalse(undo.run())
        assertEquals(1, store.rows.size)
    }
}
