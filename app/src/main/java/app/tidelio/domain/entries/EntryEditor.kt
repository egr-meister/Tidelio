package app.tidelio.domain.entries

import app.tidelio.domain.time.TimeSource
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicBoolean

/** Persistence port for entries, implemented by the Room repository and by test fakes. */
interface EntryStore {
    suspend fun insert(entry: WaterEntry): Long
    suspend fun update(entry: WaterEntry)
    suspend fun delete(id: Long): WaterEntry?
    suspend fun restore(entry: WaterEntry)
    suspend fun get(id: Long): WaterEntry?
}

/**
 * Entry use cases. Totals are never stored: every total, wave and calendar indicator is
 * derived from the entries themselves, so add/edit/delete automatically update all of them.
 */
class EntryEditor(
    private val store: EntryStore,
    private val time: TimeSource,
) {
    /** Quick add: one entry at the current local date and time. */
    suspend fun quickAdd(amountMl: Int): WaterEntry {
        require(amountMl in EntryLimits.MIN_ML..EntryLimits.MAX_ML)
        val now = time.now()
        val zone = time.zone()
        val local = now.atZone(zone)
        val draft = WaterEntry(
            id = 0,
            amountMl = amountMl,
            timestamp = now,
            localDate = local.toLocalDate(),
            localTime = local.toLocalTime().withNano(0),
            zoneId = zone.id,
            createdAt = now,
            updatedAt = now,
        )
        val id = store.insert(draft)
        return draft.copy(id = id)
    }

    /** Custom entry from an already-validated amount and date/time. */
    suspend fun add(amountMl: Int, dateTime: ZonedDateTime): WaterEntry {
        val now = time.now()
        val draft = WaterEntry(
            id = 0,
            amountMl = amountMl,
            timestamp = dateTime.toInstant(),
            localDate = dateTime.toLocalDate(),
            localTime = dateTime.toLocalTime().withNano(0),
            zoneId = dateTime.zone.id,
            createdAt = now,
            updatedAt = now,
        )
        val id = store.insert(draft)
        return draft.copy(id = id)
    }

    /**
     * Edits amount and/or date/time. If the local date/time is unchanged the original
     * timestamp and zone are kept; otherwise the entry is regrouped by the new local values.
     */
    suspend fun edit(original: WaterEntry, amountMl: Int, dateTime: ZonedDateTime): WaterEntry {
        val newDate = dateTime.toLocalDate()
        val newTime = dateTime.toLocalTime().withNano(0)
        val moved = newDate != original.localDate || newTime != original.localTime
        val updated = original.copy(
            amountMl = amountMl,
            timestamp = if (moved) dateTime.toInstant() else original.timestamp,
            localDate = newDate,
            localTime = newTime,
            zoneId = if (moved) dateTime.zone.id else original.zoneId,
            updatedAt = time.now(),
        )
        store.update(updated)
        return updated
    }

    suspend fun delete(id: Long): WaterEntry? = store.delete(id)

    /** Re-inserts the exact entry (same id and values) after a deletion. */
    suspend fun restore(entry: WaterEntry) = store.restore(entry)
}

/** An undo action that can run at most once. */
class OneShotUndo(private val action: suspend () -> Unit) {
    private val used = AtomicBoolean(false)

    val isConsumed: Boolean get() = used.get()

    /** Runs the action if it has not run before; returns whether it ran. */
    suspend fun run(): Boolean {
        if (!used.compareAndSet(false, true)) return false
        action()
        return true
    }

    /** Marks the action as used without running it (e.g. after all data was cleared). */
    fun disarm() {
        used.set(true)
    }
}
