package com.metalfish.aiadventure.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metalfish.aiadventure.data.settings.AppSettings

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onDarkTheme: (Boolean) -> Unit,
    onTextSize: (Int) -> Unit,
    onAutosave: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Настройки",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(14.dp))

            SettingSwitchCard(
                title = "Тёмная тема",
                subtitle = "Переключает оформление",
                checked = settings.darkTheme,
                onCheckedChange = onDarkTheme
            )

            Spacer(Modifier.height(12.dp))

            SettingSwitchCard(
                title = "Автосейв",
                subtitle = "Сохранять прогресс после каждого хода",
                checked = settings.autosave,
                onCheckedChange = onAutosave
            )

            // textSize оставлен для совместимости с AppRoot/VM (чтобы ничего не ломать),
            // но UI не показываем (по твоей просьбе «меньше настроек»).
            // Если захочешь вернуть — скажи, сделаю красиво.
            @Suppress("UNUSED_VARIABLE")
            val _keepTextSizeHook = settings.textSize
            @Suppress("UNUSED_PARAMETER")
            fun _keepOnTextSizeHook(v: Int) = onTextSize(v)
        }
    }
}

@Composable
private fun SettingSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
