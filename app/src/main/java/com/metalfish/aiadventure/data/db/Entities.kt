package com.metalfish.aiadventure.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey val runId: String,
    val timestamp: Long,
    val worldConfigJson: String,
    val heroJson: String,
    val sceneIndex: Int,
    val gameStateJson: String
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: String,
    val timestamp: Long,
    val sceneText: String,
    val playerChoice: String,
    val outcomeText: String,
    val statChangesJson: String
)
