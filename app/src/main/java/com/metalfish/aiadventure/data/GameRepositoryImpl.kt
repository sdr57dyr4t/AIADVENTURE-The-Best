package com.metalfish.aiadventure.data

import com.metalfish.aiadventure.domain.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepositoryImpl @Inject constructor() : GameRepository {

    private val lastState = MutableStateFlow<GameState?>(null)

    override fun observeLastState(): Flow<GameState?> = lastState.asStateFlow()

    override suspend fun loadLastState(): GameState? = lastState.value

    override suspend fun saveState(state: GameState) {
        lastState.value = state
    }

    override suspend fun clear() {
        lastState.value = null
    }
}
