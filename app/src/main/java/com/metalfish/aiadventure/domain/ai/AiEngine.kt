package com.metalfish.aiadventure.domain.ai

import com.metalfish.aiadventure.domain.model.AiContext
import com.metalfish.aiadventure.domain.model.AiTurnResult

interface AiEngine {
    suspend fun nextTurn(
        currentSceneText: String,
        playerChoice: String,
        context: AiContext
    ): AiTurnResult

    suspend fun generateSettingDescription(prompt: String): String
}
