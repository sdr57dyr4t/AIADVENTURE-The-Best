package com.metalfish.aiadventure.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.metalfish.aiadventure.ui.model.HeroDraft
import com.metalfish.aiadventure.ui.model.WorldDraft

@Composable
fun WorldSelectScreen(
    hero: HeroDraft,
    onBack: () -> Unit,
    onStart: (WorldDraft) -> Unit
) {
    val TAG = "AIAdventure"

    var setting by remember { mutableStateOf("FANTASY") }
    var tone by remember { mutableStateOf("ADVENTURE") }
    var era by remember { mutableStateOf("Средневековье") }
    var location by remember { mutableStateOf("Туманные холмы") }

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
            Text("Мир", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Выбери сеттинг и тон. Остальное можно менять потом сюжетом.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(16.dp))

            SingleChoiceRow(
                title = "Сеттинг",
                options = listOf(
                    "FANTASY" to "Фэнтези",
                    "CYBERPUNK" to "Киберпанк",
                    "POSTAPOC" to "Постапок"
                ),
                selectedKey = setting,
                onSelect = {
                    setting = it
                    Log.d(TAG, "WorldSelect: setting=$setting")
                    // пресеты эпохи/локации
                    when (setting) {
                        "CYBERPUNK" -> {
                            era = "Неоновая эра"
                            location = "Неоновый квартал"
                        }
                        "POSTAPOC" -> {
                            era = "После Падения"
                            location = "Руины мегаполиса"
                        }
                        else -> {
                            era = "Средневековье"
                            location = "Туманные холмы"
                        }
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            SingleChoiceRow(
                title = "Тон",
                options = listOf(
                    "ADVENTURE" to "Приключение",
                    "DARK" to "Мрачно",
                    "COMEDY" to "Ирония"
                ),
                selectedKey = tone,
                onSelect = {
                    tone = it
                    Log.d(TAG, "WorldSelect: tone=$tone")
                }
            )

            Spacer(Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Эпоха: $era", style = MaterialTheme.typography.titleSmall)
                    Text("Локация: $location", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Герой: ${hero.name} • ${hero.archetype}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onBack
                ) {
                    Text("Назад")
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val world = WorldDraft(
                            setting = setting,
                            era = era,
                            location = location,
                            tone = tone
                        )
                        Log.d(TAG, "WorldSelect: START world=$world")
                        onStart(world)
                    }
                ) {
                    Text("Старт")
                }
            }
        }
    }
}

@Composable
private fun SingleChoiceRow(
    title: String,
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            options.forEach { (key, label) ->
                val selected = key == selectedKey
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    else
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    modifier = Modifier
                        .selectable(
                            selected = selected,
                            onClick = { onSelect(key) },
                            role = Role.RadioButton
                        )
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
