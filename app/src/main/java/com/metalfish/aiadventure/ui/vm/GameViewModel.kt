package com.metalfish.aiadventure.ui.vm

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metalfish.aiadventure.domain.ai.AiEngine
import com.metalfish.aiadventure.domain.image.ImageEngine
import com.metalfish.aiadventure.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val ai: AiEngine,
    private val images: ImageEngine,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val TAG = "AIAdventure"

    private val _uiState = MutableStateFlow(GameUiState.initial())
    val uiState: StateFlow<GameUiState> = _uiState

    // ---- Prologue ----
    private var prologueActive: Boolean = true
    private var prologueStep: Int = 0

    // ---- Cache files ----
    private val imagesDir: File by lazy {
        File(appContext.cacheDir, "scene_images").apply { mkdirs() }
    }
    fun setWorldTextContext(setting: String, era: String, location: String, tone: String) {
        val base = GameUiState.initial()

        Log.d(TAG, "GameVM.setWorldTextContext: setting=$setting era=$era location=$location tone=$tone")

        val settingUi = when (setting.trim().uppercase()) {
            "CYBERPUNK" -> SettingUi.CYBERPUNK
            "POSTAPOC" -> SettingUi.POSTAPOC
            else -> SettingUi.FANTASY
        }

        val toneUi = when (tone.trim().uppercase()) {
            "DARK" -> ToneUi.DARK
            "COMEDY" -> ToneUi.COMEDY
            else -> ToneUi.ADVENTURE
        }

        _uiState.value = base.copy(
            world = WorldConfigUi(
                setting = settingUi,
                era = era.trim().ifEmpty { base.world.era },
                location = location.trim().ifEmpty { base.world.location },
                tone = toneUi
            )
        )

        prologueActive = true
        prologueStep = 0
        startPrologue()
    }

    // ---------------------- PROLOGUE ----------------------

    private fun startPrologue() {
        val state = _uiState.value
        val ctx = buildContext(state, phase = "PROLOGUE", step = 0)

        viewModelScope.launch {
            Log.d(TAG, "PROLOGUE step=0 -> ai.nextTurn()")
            _uiState.value = _uiState.value.copy(isWaitingForResponse = true)
            val result = runCatching {
                ai.nextTurn(
                    currentSceneText = "",
                    playerChoice = "START",
                    context = ctx
                )
            }.getOrElse {
                Log.e(TAG, "Prologue failed: ${it.message}", it)
                _uiState.value = _uiState.value.copy(
                    sceneText = "Ошибка пролога: ${it.message ?: "unknown"}",
                    choices = listOf("Продолжить", "Заново").take(2),
                    isGameOver = false,
                    isWaitingForResponse = false
                )
                return@launch
            }

            applyAiResult(result)
            generateCardForPrompt(result.imagePrompt)
        }
    }

    // ---------------------- CHOICE ----------------------

    fun handleChoice(choice: String) {
        val current = _uiState.value
        if (current.isGameOver) return

        if (prologueActive) {
            val nextStep = (prologueStep + 1).coerceAtMost(2)
            val ctx = buildContext(current, phase = "PROLOGUE", step = nextStep)

            viewModelScope.launch {
                Log.d(TAG, "PROLOGUE step=$nextStep -> ai.nextTurn() choice=$choice")
                _uiState.value = _uiState.value.copy(isWaitingForResponse = true)
                val result = runCatching {
                    ai.nextTurn(
                        currentSceneText = _uiState.value.sceneText,
                        playerChoice = choice,
                        context = ctx
                    )
                }.getOrElse {
                    Log.e(TAG, "Prologue step=$nextStep failed: ${it.message}", it)
                    _uiState.value = _uiState.value.copy(
                        sceneText = "Ошибка пролога: ${it.message ?: "unknown"}",
                        choices = _uiState.value.choices.take(2),
                        isWaitingForResponse = false
                    )
                    return@launch
                }

                prologueStep = nextStep
                applyAiResult(result)
                generateCardForPrompt(result.imagePrompt)

                if (prologueStep >= 2) {
                    prologueActive = false
                    startRunAfterPrologue()
                }
            }
            return
        }

        val ctx = buildContext(current, phase = "RUN", step = 0)

        viewModelScope.launch {
            Log.d(TAG, "RUN -> ai.nextTurn() choice=$choice")
            _uiState.value = _uiState.value.copy(isWaitingForResponse = true)
            val result = runCatching {
                ai.nextTurn(
                    currentSceneText = _uiState.value.sceneText,
                    playerChoice = choice,
                    context = ctx
                )
            }.getOrElse {
                Log.e(TAG, "RUN failed: ${it.message}", it)
                _uiState.value = _uiState.value.copy(
                    sceneText = "Ошибка генерации: ${it.message ?: "unknown"}",
                    choices = _uiState.value.choices.take(2),
                        isWaitingForResponse = false
                )
                return@launch
            }

            applyAiResult(result)
            generateCardForPrompt(result.imagePrompt)
        }
    }

    private fun startRunAfterPrologue() {
        val ctx = buildContext(_uiState.value, phase = "RUN", step = 0)

        viewModelScope.launch {
            Log.d(TAG, "RUN start after prologue -> ai.nextTurn CONTINUE")
            _uiState.value = _uiState.value.copy(isWaitingForResponse = true)
            val result = runCatching {
                ai.nextTurn(
                    currentSceneText = _uiState.value.sceneText,
                    playerChoice = "CONTINUE",
                    context = ctx
                )
            }.getOrElse {
                Log.e(TAG, "Continue after prologue failed: ${it.message}", it)
                _uiState.value = _uiState.value.copy(isWaitingForResponse = false)
                return@launch
            }

            applyAiResult(result)
            generateCardForPrompt(result.imagePrompt)
        }
    }

    // ---------------------- APPLY RESULT ----------------------

    private fun applyAiResult(result: AiTurnResult) {
        val cur = _uiState.value

        val hero0 = cur.hero
        var hp = hero0.hp
        var stamina = hero0.stamina
        var gold = hero0.gold
        var reputation = hero0.reputation

        result.statChanges.forEach { ch ->
            when (ch.key.lowercase()) {
                "hp" -> hp += ch.delta
                "stamina" -> stamina += ch.delta
                "gold" -> gold += ch.delta
                "reputation" -> reputation += ch.delta
            }
        }

        hp = hp.coerceAtLeast(0)
        stamina = stamina.coerceAtLeast(0)
        gold = gold.coerceAtLeast(0)
        reputation = reputation.coerceIn(-100, 100)

        val hero1 = hero0.copy(
            hp = hp,
            stamina = stamina,
            gold = gold,
            reputation = reputation
        )

        val combinedText =
            if (result.outcomeText.isNotBlank())
                "${result.outcomeText.trim()}\n\n${result.sceneText.trim()}"
            else
                result.sceneText.trim()

        val diedByCombat = (result.mode == TurnMode.COMBAT && result.combatOutcome == CombatOutcome.DEATH)
        val gameOver = diedByCombat || hp <= 0

        val finalText = if (result.mode == TurnMode.COMBAT) {
            when (result.combatOutcome) {
                CombatOutcome.VICTORY -> (combinedText + "\n\nПобеда. Ты выжил.").take(520)
                CombatOutcome.ESCAPE -> (combinedText + "\n\nТы сбежал. Жив, но не бесплатно.").take(520)
                CombatOutcome.DEATH -> (combinedText + "\n\nТы пал.").take(520)
                null -> combinedText
            }
        } else combinedText

        _uiState.value = cur.copy(
            hero = hero1,
            sceneText = finalText,
            choices = result.choices.take(2),
            isGameOver = gameOver,
            isWaitingForResponse = false
        )

        Log.d(TAG, "applyAiResult: mode=${result.mode} combat=${result.combatOutcome} hp=$hp gameOver=$gameOver")
    }

    // ---------------------- IMAGE GENERATION ----------------------

    private fun generateCardForPrompt(imagePrompt: String) {
        val prompt = imagePrompt.ifBlank {
            buildCardFallbackPrompt(_uiState.value, _uiState.value.sceneText)
        }

        _uiState.value = _uiState.value.copy(isImageLoading = true)

        viewModelScope.launch {
            val file = File(imagesDir, "card_${System.currentTimeMillis()}.jpg")

            val bytes = runCatching {
                images.generateImageBytes(
                    prompt = prompt,
                    width = 768,
                    height = 1024,
                    seed = stableSeed("card:${prompt.take(180)}")
                )
            }.getOrElse {
                Log.e(TAG, "Card generation failed: ${it.message}", it)
                _uiState.value = _uiState.value.copy(isImageLoading = false)
                return@launch
            }

            runCatching {
                withContext(Dispatchers.IO) { file.writeBytes(bytes) }
                cleanupOldCards(keep = 8)
                _uiState.value = _uiState.value.copy(
                    imagePath = file.absolutePath,
                    isImageLoading = false
                )
                Log.d(TAG, "Card generated: ${file.absolutePath}")
            }.onFailure {
                Log.e(TAG, "Saving card failed: ${it.message}", it)
                _uiState.value = _uiState.value.copy(isImageLoading = false)
            }
        }
    }

    private fun cleanupOldCards(keep: Int) {
        val cards = imagesDir.listFiles()
            ?.filter { it.name.startsWith("card_") && it.name.endsWith(".jpg") }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        cards.drop(keep).forEach { runCatching { it.delete() } }
    }

    // ---------------------- PROMPTS ----------------------

    private fun buildCardFallbackPrompt(s: GameUiState, sceneText: String): String {
        val setting = s.world.setting.name
        val tone = s.world.tone.name
        val era = s.world.era
        val location = s.world.location

        val heroClass = s.heroProfile.archetype.name
        val heroName = s.heroProfile.name

        val style = when (setting) {
            "CYBERPUNK" -> "cinematic cyberpunk action frame, neon rain, dramatic lighting"
            "POSTAPOC" -> "cinematic post-apocalyptic action frame, gritty, dramatic light"
            else -> "cinematic fantasy action frame, dramatic light, detailed"
        }

        return """
$style.
World: $setting / $era / $location. Tone: $tone.
Hero: $heroName, class: $heroClass.
Scene: ${sceneText.take(200)}
No text, no watermark, no UI. High quality digital painting.
""".trimIndent()
    }

    private fun stableSeed(key: String): Long {
        var h = 1125899906842597L
        for (c in key) h = 31L * h + c.code
        return h
    }

    private fun buildContext(state: GameUiState, phase: String, step: Int): AiContext {
        return AiContext(
            setting = state.world.setting.name,
            era = state.world.era,
            location = state.world.location,
            tone = state.world.tone.name,
            heroName = state.heroProfile.name,
            heroClass = state.heroProfile.archetype.name,
            str = state.heroProfile.stats.str,
            agi = state.heroProfile.stats.agi,
            int = state.heroProfile.stats.int,
            cha = state.heroProfile.stats.cha,
            phase = phase,
            step = step
        )
    }

    fun restart() {
        prologueActive = true
        prologueStep = 0
        _uiState.value = GameUiState.initial()
        startPrologue()
    }
}


