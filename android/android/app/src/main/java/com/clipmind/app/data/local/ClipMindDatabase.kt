package com.clipmind.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [VideoEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class ClipMindDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    companion object {
        const val NAME = "clipmind.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE videos ADD COLUMN uploadStatus TEXT NOT NULL DEFAULT 'PENDING'")
                database.execSQL("ALTER TABLE videos ADD COLUMN uploadProgress REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE videos ADD COLUMN remoteId TEXT")
            }
        }
    }
}
