package com.metalfish.aiadventure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import com.metalfish.aiadventure.ui.AppRoot
import com.metalfish.aiadventure.ui.theme.AIAdventureTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // ВАЖНО: убрали параметры darkTheme/textSize,
            // потому что в твоей AIAdventureTheme их нет (по ошибке компилятора)
            AIAdventureTheme {
                AppRoot()
            }
        }
    }
}
