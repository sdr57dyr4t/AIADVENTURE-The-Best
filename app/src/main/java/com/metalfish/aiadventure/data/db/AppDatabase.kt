package com.metalfish.aiadventure.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RunEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun runDao(): RunDao
    abstract fun historyDao(): HistoryDao
}
