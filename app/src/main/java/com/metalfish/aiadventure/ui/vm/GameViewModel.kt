package com.metalfish.aiadventure.ui.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metalfish.aiadventure.domain.ai.AiEngine
import com.metalfish.aiadventure.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val ai: AiEngine
) : ViewModel() {

    private val TAG = "AIAdventure"

    private val _uiState = MutableStateFlow(GameUiState.initial())
    val uiState: StateFlow<GameUiState> = _uiState

    // ---- Prologue ----
    private var prologueActive: Boolean = true
    private var prologueStep: Int = 0
    private var turnNumber: Int = 0
    // ✅ Схараняем оригинальное значение setting
    private var originalSetting: String = "FANTASY"

    fun setWorldTextContext(setting: String, era: String, location: String, tone: String) {
        val base = GameUiState.initial()

        Log.d(TAG, "GameVM.setWorldTextContext: setting=$setting era=$era location=$location tone=$tone")

        // ✅ Схараяем оригинальное значение
        originalSetting = setting.trim().uppercase()

        val settingUi = when (originalSetting) {
            "CYBERPUNK" -> SettingUi.CYBERPUNK
            "POSTAPOC" -> SettingUi.POSTAPOC
            "SMUTA", "PETR1", "WAR1812" -> SettingUi.FANTASY
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
            ),
            settingRaw = originalSetting
        )

        prologueActive = true
        prologueStep = 0
        turnNumber = 0
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
                    sceneText = "������ �������: ${it.message ?: "unknown"}",
                    choices = listOf("����������", "������").take(2),
                    isGameOver = false,
                    isWaitingForResponse = false
                )
                return@launch
            }

            applyAiResult(result)
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
                        sceneText = "������ �������: ${it.message ?: "unknown"}",
                        choices = _uiState.value.choices.take(2),
                        isWaitingForResponse = false
                    )
                    return@launch
                }

                prologueStep = nextStep
                applyAiResult(result)

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
                    sceneText = "������ ���������: ${it.message ?: "unknown"}",
                    choices = _uiState.value.choices.take(2),
                    isWaitingForResponse = false
                )
                return@launch
            }

            applyAiResult(result)
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
                CombatOutcome.VICTORY -> (combinedText + "\n\n������. �� �����.").take(520)
                CombatOutcome.ESCAPE -> (combinedText + "\n\n�� ������. ���, �� �� ���������.").take(520)
                CombatOutcome.DEATH -> (combinedText + "\n\n�� ���.").take(520)
                null -> combinedText
            }
        } else combinedText

        val resolvedGoal = when {
            cur.goal.isNotBlank() -> cur.goal
            result.goal.isNotBlank() -> result.goal
            else -> ""
        }

        _uiState.value = cur.copy(
            hero = hero1,
            sceneText = finalText,
            choices = result.choices.take(2),
            sceneName = result.sceneName,
            dayWeather = result.dayWeather,
            terrain = result.terrain,
            deadPrc = result.deadPrc,
            heroMind = result.heroMind,
            goal = resolvedGoal,
            isGameOver = gameOver,
            isWaitingForResponse = false,
            leftAction = result.leftAction,
            rightAction = result.rightAction,
            tokensTotal = if (result.tokensTotal > 0) result.tokensTotal else cur.tokensTotal,
            turnNumber = run {
                turnNumber += 1
                turnNumber
            },
            settingRaw = cur.settingRaw
        )

        Log.d(TAG, "applyAiResult: mode=${result.mode} combat=${result.combatOutcome} hp=$hp gameOver=$gameOver")
    }

    // ✅ Исправлено: используем originalSetting вместо state.world.setting.name
    private fun buildContext(state: GameUiState, phase: String, step: Int): AiContext {
        return AiContext(
            setting = originalSetting,
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
        turnNumber = 0
        _uiState.value = GameUiState.initial()
        startPrologue()
    }
}
