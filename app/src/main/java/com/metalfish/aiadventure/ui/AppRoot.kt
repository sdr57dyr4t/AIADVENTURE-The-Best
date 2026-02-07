package com.metalfish.aiadventure.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.metalfish.aiadventure.ui.model.HeroDraft
import com.metalfish.aiadventure.ui.model.WorldDraft
import com.metalfish.aiadventure.ui.screens.EpochModeScreen
import com.metalfish.aiadventure.ui.screens.GameScreen
import com.metalfish.aiadventure.ui.screens.KnowledgeCheckScreen
import com.metalfish.aiadventure.ui.screens.LearnMoreScreen
import com.metalfish.aiadventure.ui.screens.SettingsScreen
import com.metalfish.aiadventure.ui.screens.SetupScreen
import com.metalfish.aiadventure.ui.screens.StartScreen
import com.metalfish.aiadventure.ui.audio.MusicPlayer
import com.metalfish.aiadventure.ui.audio.SfxPlayer
import com.metalfish.aiadventure.ui.vm.AppViewModel
import com.metalfish.aiadventure.ui.vm.GameViewModel
import com.metalfish.aiadventure.ui.vm.KnowledgeCheckViewModel
import com.metalfish.aiadventure.ui.vm.LearnMoreViewModel
import com.metalfish.aiadventure.ui.vm.SettingsViewModel

object Routes {
    const val Start = "start"
    const val Setup = "setup"
    const val EpochMode = "epoch_mode"
    const val LearnMore = "learn_more"
    const val KnowledgeCheck = "knowledge_check"
    const val Game = "game"
    const val Settings = "settings"
}

@Composable
fun AppRoot() {
    val TAG = "AIAdventure"
    val navController = rememberNavController()
    val appVm: AppViewModel = hiltViewModel()
    val appSettings by appVm.settings.collectAsState()

    androidx.compose.runtime.LaunchedEffect(appSettings.musicVolume, appSettings.sfxVolume) {
        MusicPlayer.setVolume(appSettings.musicVolume)
        SfxPlayer.setVolume(appSettings.sfxVolume)
    }

    val heroDraftState = remember { mutableStateOf<HeroDraft?>(null) }
    val worldDraftState = remember { mutableStateOf<WorldDraft?>(null) }

    fun safeParentEntry(): NavBackStackEntry {
        return runCatching { navController.getBackStackEntry(Routes.Start) }
            .getOrElse { navController.currentBackStackEntry!! }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Start
    ) {
        composable(Routes.Start) {
            val parentEntry = remember(navController) { safeParentEntry() }
            val gameVm: GameViewModel = hiltViewModel(parentEntry)
            val activity = LocalContext.current as? Activity

            BackHandler {
                MusicPlayer.stop()
                SfxPlayer.stop()
                activity?.finishAndRemoveTask()
            }

            StartScreen(
                onStartWorld = { world ->
                    heroDraftState.value = null
                    worldDraftState.value = world

                    gameVm.setWorldTextContext(
                        setting = world.setting,
                        era = world.era,
                        location = world.location,
                        tone = world.tone
                    )

                    navController.navigate(Routes.EpochMode) {
                        popUpTo(Routes.Start) { inclusive = false }
                    }
                },
                onSettings = { navController.navigate(Routes.Settings) }
            )
        }

        composable(Routes.EpochMode) {
            val world = worldDraftState.value
            if (world == null) {
                navController.popBackStack()
            } else {
                EpochModeScreen(
                    world = world,
                    onBack = { navController.popBackStack() },
                    onLearnMore = { navController.navigate(Routes.LearnMore) },
                    onKnowledgeCheck = { navController.navigate(Routes.KnowledgeCheck) },
                    onImmerse = { navController.navigate(Routes.Game) }
                )
            }
        }

        composable(Routes.LearnMore) {
            val world = worldDraftState.value
            if (world == null) {
                navController.popBackStack()
            } else {
                val vm: LearnMoreViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()

                androidx.compose.runtime.LaunchedEffect(
                    world.setting,
                    world.era,
                    world.location,
                    world.tone
                ) {
                    vm.initialize(world)
                }

                LearnMoreScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onInputChanged = vm::onInputChanged,
                    onAsk = vm::askCurrentQuestion,
                    onAskSuggestion = vm::askSuggestedQuestion
                )
            }
        }

        composable(Routes.KnowledgeCheck) {
            val world = worldDraftState.value
            if (world == null) {
                navController.popBackStack()
            } else {
                val vm: KnowledgeCheckViewModel = hiltViewModel()
                val state by vm.uiState.collectAsState()

                androidx.compose.runtime.LaunchedEffect(
                    world.setting,
                    world.era,
                    world.location,
                    world.tone
                ) {
                    vm.initialize(world)
                }

                KnowledgeCheckScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onChooseOption = vm::chooseOption,
                    onNextQuestion = vm::nextQuestion
                )
            }
        }

        composable(Routes.Setup) {
            val parentEntry = remember(navController) { safeParentEntry() }
            val gameVm: GameViewModel = hiltViewModel(parentEntry)

            SetupScreen(
                onBack = { navController.popBackStack() },
                onStart = { hero: HeroDraft, world: WorldDraft ->
                    heroDraftState.value = hero
                    worldDraftState.value = world

                    Log.d(TAG, "Setup: hero=$hero")
                    Log.d(TAG, "Setup: world=$world")

                    gameVm.setWorldTextContext(
                        setting = world.setting,
                        era = world.era,
                        location = world.location,
                        tone = world.tone
                    )

                    navController.navigate(Routes.Game) {
                        popUpTo(Routes.Start) { inclusive = false }
                    }
                }
            )
        }

        composable(Routes.Game) {
            val parentEntry = remember(navController) { safeParentEntry() }
            val gameVm: GameViewModel = hiltViewModel(parentEntry)
            val uiState by gameVm.uiState.collectAsState()

            GameScreen(
                state = uiState,
                gigaChatModel = appSettings.gigaChatModel,
                onPick = { choice ->
                    Log.d(TAG, "AppRoot.onPick -> GameVM.handleChoice('$choice')")
                    gameVm.handleChoice(choice)
                },
                onRestart = {
                    Log.d(TAG, "AppRoot.onRestart")
                    gameVm.restart()
                    navController.navigate(Routes.Start) {
                        popUpTo(Routes.Start) { inclusive = true }
                    }
                },
                onSettings = { navController.navigate(Routes.Settings) }
            )
        }

        composable(Routes.Settings) {
            val vm: SettingsViewModel = hiltViewModel()
            val settings by vm.settings.collectAsState()

            SettingsScreen(
                settings = settings,
                onGigaChatModel = vm::setGigaChatModel,
                onMusicVolume = vm::setMusicVolume,
                onSfxVolume = vm::setSfxVolume,
                onClose = { navController.popBackStack() }
            )
        }
    }
}


