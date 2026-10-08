package app.tidelio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [WaterEntryEntity::class, GoalChangeEntity::class],
    version = TidelioDatabase.VERSION,
    exportSchema = true,
)
abstract class TidelioDatabase : RoomDatabase() {
    abstract fun entryDao(): WaterEntryDao
    abstract fun goalDao(): GoalChangeDao

    companion object {
        const val VERSION = 1
        const val NAME = "tidelio.db"

        fun build(context: Context): TidelioDatabase =
            Room.databaseBuilder(context.applicationContext, TidelioDatabase::class.java, NAME)
                .addMigrations(*Migrations.ALL)
                // Intentionally NO fallbackToDestructiveMigration(): user data must survive updates.
                .build()
    }
}

/**
 * Schema migrations. Version 1 is the initial schema (exported to app/schemas), so the list is
 * empty. Every future schema change must bump [TidelioDatabase.VERSION], export the new schema
 * and add a Migration here, verified with MigrationTestHelper against the exported JSON.
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
