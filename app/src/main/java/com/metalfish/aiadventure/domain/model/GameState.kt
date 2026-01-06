package com.metalfish.aiadventure.domain.model

import kotlinx.serialization.Serializable

/**
 * Доменные модели (их ждут GameRepository / GameRules).
 * Даже если UI сейчас использует GameUiState/HeroUi — эти классы нужны для компиляции
 * и дальнейшего расширения (сохранения/Room/история).
 */

@Serializable
data class Stats(
    val strength: Int = 5,
    val agility: Int = 5,
    val intelligence: Int = 5,
    val charisma: Int = 5
)

@Serializable
data class Hero(
    val id: String = "hero",
    val name: String = "Безымянный",
    val archetype: String = "WARRIOR",
    val stats: Stats = Stats(),
    val hp: Int = 50,
    val stamina: Int = 30,
    val gold: Int = 10,
    val reputation: Int = 0,
    val tags: List<String> = emptyList(),
    val inventory: List<String> = emptyList()
)

@Serializable
data class WorldConfig(
    val setting: String = "FANTASY",
    val era: String = "Средневековье",
    val location: String = "Туманные холмы",
    val tone: String = "ADVENTURE"
)

@Serializable
data class Scene(
    val text: String,
    val choices: List<String>,
    val difficulty: Int = 3,
    val imagePrompt: String? = null
)

@Serializable
data class HistoryEntry(
    val timestamp: Long,
    val sceneText: String,
    val playerChoice: String,
    val outcomeText: String,
    val statChanges: List<StatDelta> = emptyList()
)

@Serializable
data class StatDelta(
    val key: String,
    val delta: Int
)

@Serializable
data class GameState(
    val runId: String = "run",
    val hero: Hero = Hero(),
    val worldConfig: WorldConfig = WorldConfig(),
    val sceneIndex: Int = 0,
    val currentScene: Scene = Scene(
        text = "Ты стоишь на распутье. Приключение начинается.",
        choices = listOf("Пойти налево", "Пойти направо")
    ),
    val history: List<HistoryEntry> = emptyList(),
    val rngSeed: Long = 1L,
    val isGameOver: Boolean = false
)
