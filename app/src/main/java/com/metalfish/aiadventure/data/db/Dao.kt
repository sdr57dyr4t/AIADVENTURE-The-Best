package com.metalfish.aiadventure.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RunDao {
    @Query("SELECT * FROM runs ORDER BY timestamp DESC LIMIT 1")
    suspend fun latestRun(): RunEntity?

    @Query("SELECT * FROM runs ORDER BY timestamp DESC")
    suspend fun allRuns(): List<RunEntity>

    @Query("DELETE FROM runs WHERE runId = :runId")
    suspend fun deleteRun(runId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRun(run: RunEntity)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE runId = :runId ORDER BY timestamp ASC")
    suspend fun historyForRun(runId: String): List<HistoryEntity>

    @Insert
    suspend fun insert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE runId = :runId")
    suspend fun clearRun(runId: String)
}
