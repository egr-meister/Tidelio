package app.tidelio.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stored entry. Dates are stored as epoch days and local times as second-of-day so they
 * sort and range-query correctly in SQLite.
 */
@Entity(
    tableName = "water_entries",
    indices = [
        Index(value = ["loggedLocalDate", "loggedLocalTime"]),
        Index(value = ["timestamp"]),
    ],
)
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "amountMl") val amountMl: Int,
    /** Absolute instant, epoch milliseconds. */
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    /** Local date at recording (or as edited), epoch day. Used for grouping. */
    @ColumnInfo(name = "loggedLocalDate") val loggedLocalDate: Long,
    /** Local time at recording (or as edited), second of day. Used for period classification. */
    @ColumnInfo(name = "loggedLocalTime") val loggedLocalTime: Int,
    @ColumnInfo(name = "zoneId") val zoneId: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long,
)

/** One goal record per effective date (unique). */
@Entity(
    tableName = "goal_changes",
    indices = [Index(value = ["effectiveLocalDate"], unique = true)],
)
data class GoalChangeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch day from which this goal applies. */
    @ColumnInfo(name = "effectiveLocalDate") val effectiveLocalDate: Long,
    @ColumnInfo(name = "goalMl") val goalMl: Int,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
)

/** Aggregated row for calendar indicators; volumes are Long. */
data class DailyTotalRow(
    @ColumnInfo(name = "day") val day: Long,
    @ColumnInfo(name = "totalMl") val totalMl: Long,
    @ColumnInfo(name = "entryCount") val entryCount: Int,
)
