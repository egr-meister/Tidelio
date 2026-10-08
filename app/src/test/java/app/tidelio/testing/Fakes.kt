package app.tidelio.testing

import app.tidelio.domain.entries.EntryStore
import app.tidelio.domain.entries.WaterEntry
import app.tidelio.domain.time.TimeSource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class FakeTime(
    var instant: Instant,
    var zoneId: ZoneId = ZoneId.of("UTC"),
    var elapsed: Long = 0L,
) : TimeSource {
    override fun now(): Instant = instant
    override fun zone(): ZoneId = zoneId
    override fun elapsedMillis(): Long = elapsed

    companion object {
        fun at(dateTime: LocalDateTime, zone: ZoneId = ZoneId.of("UTC")) =
            FakeTime(dateTime.atZone(zone).toInstant(), zone)
    }
}

class FakeEntryStore : EntryStore {
    val rows = linkedMapOf<Long, WaterEntry>()
    private var nextId = 1L

    override suspend fun insert(entry: WaterEntry): Long {
        val id = nextId++
        rows[id] = entry.copy(id = id)
        return id
    }

    override suspend fun update(entry: WaterEntry) {
        check(rows.containsKey(entry.id))
        rows[entry.id] = entry
    }

    override suspend fun delete(id: Long): WaterEntry? = rows.remove(id)

    override suspend fun restore(entry: WaterEntry) {
        if (!rows.containsKey(entry.id)) rows[entry.id] = entry
    }

    override suspend fun get(id: Long): WaterEntry? = rows[id]

    fun onDate(date: LocalDate) = rows.values.filter { it.localDate == date }
}

fun entry(
    amount: Int,
    date: LocalDate,
    time: LocalTime,
    id: Long = 0,
    zone: ZoneId = ZoneId.of("UTC"),
): WaterEntry {
    val instant = LocalDateTime.of(date, time).atZone(zone).toInstant()
    return WaterEntry(id, amount, instant, date, time, zone.id, instant, instant)
}
