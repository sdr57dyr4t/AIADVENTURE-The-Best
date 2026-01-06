package com.metalfish.aiadventure.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.metalfish.aiadventure.ui.model.HeroDraft
import com.metalfish.aiadventure.ui.model.WorldDraft
import com.metalfish.aiadventure.ui.screens.GameScreen
import com.metalfish.aiadventure.ui.screens.SettingsScreen
import com.metalfish.aiadventure.ui.screens.SetupScreen
import com.metalfish.aiadventure.ui.screens.StartScreen
import com.metalfish.aiadventure.ui.vm.GameViewModel
import com.metalfish.aiadventure.ui.vm.SettingsViewModel

object Routes {
    const val Start = "start"
    const val Setup = "setup"
    const val Game = "game"
    const val Settings = "settings"
}

@Composable
fun AppRoot() {
    val TAG = "AIAdventure"
    val navController = rememberNavController()

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

                    navController.navigate(Routes.Game) {
                        popUpTo(Routes.Start) { inclusive = false }
                    }
                }
            )
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
                onPick = { choice ->
                    Log.d(TAG, "AppRoot.onPick -> GameVM.handleChoice('$choice')")
                    gameVm.handleChoice(choice)
                },
                onRestart = {
                    Log.d(TAG, "AppRoot.onRestart")
                    gameVm.restart()
                    navController.navigate(Routes.Setup)
                },
                onSettings = { navController.navigate(Routes.Settings) }
            )
        }

        composable(Routes.Settings) {
            val vm: SettingsViewModel = hiltViewModel()
            val settings by vm.settings.collectAsState()

            SettingsScreen(
                settings = settings,
                onDarkTheme = vm::setDarkTheme,
                onTextSize = vm::setTextSize,   // ✅ теперь Int
                onAutosave = vm::setAutosave
            )
        }
    }
}
