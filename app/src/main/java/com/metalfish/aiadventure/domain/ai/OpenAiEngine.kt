package com.metalfish.aiadventure.domain.ai

import com.metalfish.aiadventure.domain.model.AiContext
import com.metalfish.aiadventure.domain.model.AiTurnResult
import com.metalfish.aiadventure.domain.model.StatChange

/**
 * Заглушка. Подключение OpenAI можно сделать позже.
 * Сейчас оставляем, чтобы проект компилировался.
 */
class OpenAiEngine : AiEngine {
    override suspend fun nextTurn(
        currentSceneText: String,
        playerChoice: String,
        context: AiContext
    ): AiTurnResult {
        return AiTurnResult(
            sceneText = "OpenAI Engine не подключён. Используй GigaChat/FakeAiEngine.",
            choices = listOf("LEFT: Ок", "RIGHT: Ок"),
            outcomeText = "Заглушка.",
            statChanges = listOf(StatChange("reputation", 0))
        )
    }
}
