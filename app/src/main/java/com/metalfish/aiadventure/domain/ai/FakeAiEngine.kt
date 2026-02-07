package com.metalfish.aiadventure.domain.ai

import com.metalfish.aiadventure.domain.model.AiContext
import com.metalfish.aiadventure.domain.model.AiTurnResult
import com.metalfish.aiadventure.domain.model.StatChange
import kotlin.random.Random

class FakeAiEngine : AiEngine {

    override suspend fun nextTurn(
        currentSceneText: String,
        playerChoice: String,
        context: AiContext
    ): AiTurnResult {
        val seed = (context.era + context.location + currentSceneText + playerChoice).hashCode()
        val rnd = Random(seed)

        val placeHint = when (context.setting.uppercase()) {
            "CYBERPUNK" -> "неоновый дождь отражается в мокром асфальте"
            "POSTAPOC" -> "ветер гонит пыль, и где-то скрипит ржавая вывеска"
            else -> "в воздухе пахнет костром и мокрой землёй"
        }

        val toneHint = when (context.tone.uppercase()) {
            "DARK" -> "тени сгущаются, и любое решение может дорого стоить"
            "COMEDY" -> "всё выглядит серьёзно, но судьба явно настроена пошутить"
            else -> "впереди ощущается азарт приключения"
        }

        val scene = "(${context.era}, ${context.location}) $placeHint. $toneHint. Ты делаешь шаг и замечаешь развилку."

        val left = "LEFT: Рискнуть и действовать"
        val right = "RIGHT: Осторожно отступить"

        val hpDelta = if (rnd.nextBoolean()) -rnd.nextInt(1, 6) else rnd.nextInt(0, 4)
        val goldDelta = if (rnd.nextInt(100) < 35) rnd.nextInt(1, 12) else 0

        val changes = buildList {
            if (hpDelta != 0) add(StatChange("hp", hpDelta))
            if (goldDelta != 0) add(StatChange("gold", goldDelta))
        }

        val outcome = if (hpDelta < 0) "Ты платишь за риск здоровьем." else "Тебе везёт — всё проходит гладко."

        return AiTurnResult(
            sceneText = scene,
            choices = listOf(left, right),
            outcomeText = outcome,
            sceneName = "Сцена",
            dayWeather = "День, ясно",
            terrain = "Равнина",
            deadPrc = 10,
            heroMind = "Стоит рискнуть, но осторожность не помешает.",
            goal = "Найти безопасный путь через развилку.",
            statChanges = changes
        )
    }

    override suspend fun generateSettingDescription(prompt: String): String {
        return "Тихий прибрежный город на краю шторма, где магия и механика борются за власть."
    }
}
