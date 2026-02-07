package com.metalfish.aiadventure.domain.model

/**
 * UI состояние игры.
 * HeroProfileUi / WorldConfigUi и прочие модели уже определены в других файлах проекта,
 * поэтому тут мы их НЕ объявляем повторно — только используем.
 */
data class GameUiState(
    val sceneText: String,
    val choices: List<String>,
    val isGameOver: Boolean,
    val isWaitingForResponse: Boolean,
    val sceneName: String = "",
    val dayWeather: String = "",
    val terrain: String = "",
    val deadPrc: Int? = null,
    val heroMind: String = "",
    val goal: String = "",

    val hero: HeroUi,
    val heroProfile: HeroProfileUi,
    val world: WorldConfigUi,

    val leftAction: ChoiceAction? = null,
    val rightAction: ChoiceAction? = null,
    val tokensTotal: Int = 0,
    val turnNumber: Int = 0,
    val settingRaw: String = "FANTASY"
) {
    companion object {
        fun initial() = GameUiState(
            sceneText = "Ты стоишь на распутье. Приключение начинается.",
            choices = listOf("Пойти налево", "Пойти направо"),
            isGameOver = false,
            isWaitingForResponse = false,
            sceneName = "",
            dayWeather = "",
            terrain = "",
            deadPrc = null,
            heroMind = "",
            goal = "",
            hero = HeroUi(hp = 50, stamina = 30, gold = 10, reputation = 0),

            heroProfile = HeroProfileUi(
                name = "Безымянный",
                archetype = ArchetypeUi.WARRIOR,
                // ✅ ВАЖНО: у тебя это называется HeroStatsUi
                stats = HeroStatsUi(str = 5, agi = 5, int = 5, cha = 5)
            ),

            world = WorldConfigUi(
                setting = SettingUi.FANTASY,
                era = "Средневековье",
                location = "Туманные холмы",
                tone = ToneUi.ADVENTURE
            ),

            leftAction = null,
            rightAction = null,
            tokensTotal = 0,
            turnNumber = 0,
            settingRaw = "FANTASY"
        )
    }
}

data class HeroUi(
    val hp: Int,
    val stamina: Int,
    val gold: Int,
    val reputation: Int
)




