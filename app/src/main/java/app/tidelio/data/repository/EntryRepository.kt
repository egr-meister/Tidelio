package app.tidelio.data.repository

import androidx.room.withTransaction
import app.tidelio.data.local.DailyTotalRow
import app.tidelio.data.local.TidelioDatabase
import app.tidelio.data.local.WaterEntryEntity
import app.tidelio.domain.entries.EntryStore
import app.tidelio.domain.entries.WaterEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class DailyTotal(val date: LocalDate, val totalMl: Long, val entryCount: Int)

/** Room-backed entry storage. All queries are scoped to the dates a screen needs. */
class EntryRepository(private val db: TidelioDatabase) : EntryStore {

    private val dao = db.entryDao()

    fun observeDay(date: LocalDate): Flow<List<WaterEntry>> =
        dao.observeDay(date.toEpochDay()).map { list -> list.map { it.toDomain() } }

    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<WaterEntry>> =
        dao.observeRange(start.toEpochDay(), end.toEpochDay()).map { list -> list.map { it.toDomain() } }

    fun observeDailyTotals(start: LocalDate, end: LocalDate): Flow<List<DailyTotal>> =
        dao.observeDailyTotals(start.toEpochDay(), end.toEpochDay()).map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(id: Long): WaterEntry? = dao.get(id)?.toDomain()

    override suspend fun insert(entry: WaterEntry): Long =
        db.withTransaction { dao.insert(entry.toEntity(keepId = false)) }

    override suspend fun update(entry: WaterEntry) {
        db.withTransaction {
            dao.update(entry.toEntity(keepId = true))
            Unit
        }
    }

    override suspend fun delete(id: Long): WaterEntry? = db.withTransaction {
        val existing = dao.get(id) ?: return@withTransaction null
        dao.delete(id)
        existing.toDomain()
    }

    override suspend fun restore(entry: WaterEntry) {
        db.withTransaction {
            if (dao.get(entry.id) == null) {
                dao.insert(entry.toEntity(keepId = true))
            }
            Unit
        }
    }
}

internal fun WaterEntryEntity.toDomain() = WaterEntry(
    id = id,
    amountMl = amountMl,
    timestamp = Instant.ofEpochMilli(timestamp),
    localDate = LocalDate.ofEpochDay(loggedLocalDate),
    localTime = LocalTime.ofSecondOfDay(loggedLocalTime.toLong()),
    zoneId = zoneId,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun WaterEntry.toEntity(keepId: Boolean) = WaterEntryEntity(
    id = if (keepId) id else 0,
    amountMl = amountMl,
    timestamp = timestamp.toEpochMilli(),
    loggedLocalDate = localDate.toEpochDay(),
    loggedLocalTime = localTime.toSecondOfDay(),
    zoneId = zoneId,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

private fun DailyTotalRow.toDomain() = DailyTotal(LocalDate.ofEpochDay(day), totalMl, entryCount)
