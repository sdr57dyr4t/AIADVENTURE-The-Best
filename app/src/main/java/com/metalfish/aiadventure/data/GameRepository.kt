package com.metalfish.aiadventure.data

import com.metalfish.aiadventure.domain.model.GameState
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    fun observeLastState(): Flow<GameState?>
    suspend fun loadLastState(): GameState?
    suspend fun saveState(state: GameState)
    suspend fun clear()
}
