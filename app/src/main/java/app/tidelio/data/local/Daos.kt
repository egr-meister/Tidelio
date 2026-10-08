package app.tidelio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterEntryDao {

    @Query("SELECT * FROM water_entries WHERE loggedLocalDate = :epochDay ORDER BY loggedLocalTime, id")
    fun observeDay(epochDay: Long): Flow<List<WaterEntryEntity>>

    @Query(
        "SELECT * FROM water_entries WHERE loggedLocalDate BETWEEN :startEpochDay AND :endEpochDay " +
            "ORDER BY loggedLocalDate, loggedLocalTime, id",
    )
    fun observeRange(startEpochDay: Long, endEpochDay: Long): Flow<List<WaterEntryEntity>>

    @Query(
        "SELECT loggedLocalDate AS day, SUM(amountMl) AS totalMl, COUNT(*) AS entryCount " +
            "FROM water_entries WHERE loggedLocalDate BETWEEN :startEpochDay AND :endEpochDay " +
            "GROUP BY loggedLocalDate",
    )
    fun observeDailyTotals(startEpochDay: Long, endEpochDay: Long): Flow<List<DailyTotalRow>>

    @Query("SELECT * FROM water_entries WHERE id = :id")
    suspend fun get(id: Long): WaterEntryEntity?

    @Query("SELECT COUNT(*) FROM water_entries")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: WaterEntryEntity): Long

    @Update
    suspend fun update(entry: WaterEntryEntity): Int

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM water_entries")
    suspend fun deleteAll()
}

@Dao
abstract class GoalChangeDao {

    @Query("SELECT * FROM goal_changes ORDER BY effectiveLocalDate")
    abstract fun observeAll(): Flow<List<GoalChangeEntity>>

    @Query("SELECT * FROM goal_changes ORDER BY effectiveLocalDate")
    abstract suspend fun getAll(): List<GoalChangeEntity>

    @Query("DELETE FROM goal_changes WHERE effectiveLocalDate = :epochDay")
    abstract suspend fun deleteForDate(epochDay: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(change: GoalChangeEntity): Long

    /** Keeps exactly one goal record per effective date. Earlier dates are never touched. */
    @Transaction
    open suspend fun replaceForDate(change: GoalChangeEntity) {
        deleteForDate(change.effectiveLocalDate)
        insert(change)
    }

    @Query("DELETE FROM goal_changes")
    abstract suspend fun deleteAll()
}
