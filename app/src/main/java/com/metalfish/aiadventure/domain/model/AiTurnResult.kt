package com.metalfish.aiadventure.domain.model

data class AiTurnResult(
    val sceneText: String,
    val choices: List<String>,
    val outcomeText: String,
    val sceneName: String = "",
    val dayWeather: String = "",
    val terrain: String = "",
    val deadPrc: Int? = null,
    val statChanges: List<StatChange> = emptyList(),
    val imagePrompt: String = "",
    val mode: TurnMode = TurnMode.STORY,
    val combatOutcome: CombatOutcome? = null
)

data class StatChange(
    val key: String,
    val delta: Int
)

enum class TurnMode {
    STORY,
    COMBAT
}

enum class CombatOutcome {
    VICTORY,
    DEATH,
    ESCAPE
}
