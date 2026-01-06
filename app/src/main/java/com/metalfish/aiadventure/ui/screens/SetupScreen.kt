package com.metalfish.aiadventure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.metalfish.aiadventure.ui.model.HeroDraft
import com.metalfish.aiadventure.ui.model.WorldDraft
import com.metalfish.aiadventure.ui.theme.BrandMint
import com.metalfish.aiadventure.ui.theme.BrandViolet
import com.metalfish.aiadventure.ui.theme.DarkBg
import com.metalfish.aiadventure.ui.theme.DarkBg2
import com.metalfish.aiadventure.ui.theme.TextPrimary
import com.metalfish.aiadventure.ui.theme.TextSecondary

@Composable
fun SetupScreen(
    onBack: () -> Unit,
    onStart: (HeroDraft, WorldDraft) -> Unit
) {
    var name by remember { mutableStateOf(TextFieldValue("Безымянный")) }
    var archetype by remember { mutableStateOf("WARRIOR") }
    var setting by remember { mutableStateOf("FANTASY") }
    var tone by remember { mutableStateOf("ADVENTURE") }

    val era = when (setting) {
        "CYBERPUNK" -> "Неоновая эра"
        "POSTAPOC" -> "После Падения"
        else -> "Средневековье"
    }
    val location = when (setting) {
        "CYBERPUNK" -> "Неоновый квартал"
        "POSTAPOC" -> "Руины мегаполиса"
        else -> "Туманные холмы"
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(DarkBg, DarkBg2, DarkBg)))
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Новый забег", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text("Выбери основу — остальное дорисует ИИ.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SectionTitle("Класс")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("Воин", archetype == "WARRIOR") { archetype = "WARRIOR" }
                Chip("Плут", archetype == "ROGUE") { archetype = "ROGUE" }
                Chip("Маг", archetype == "MAGE") { archetype = "MAGE" }
                Chip("Следопыт", archetype == "RANGER") { archetype = "RANGER" }
            }

            SectionTitle("Мир")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("Фэнтези", setting == "FANTASY") { setting = "FANTASY" }
                Chip("Киберпанк", setting == "CYBERPUNK") { setting = "CYBERPUNK" }
                Chip("Постапок", setting == "POSTAPOC") { setting = "POSTAPOC" }
            }

            SectionTitle("Тон")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Chip("Прикл.", tone == "ADVENTURE") { tone = "ADVENTURE" }
                Chip("Мрачн.", tone == "DARK") { tone = "DARK" }
                Chip("Ирония", tone == "COMEDY") { tone = "COMEDY" }
            }

            Spacer(Modifier.height(6.dp))
            Text("Эпоха: $era • Локация: $location", style = MaterialTheme.typography.labelLarge, color = TextSecondary)

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = {
                    val hero = HeroDraft(
                        name = name.text.trim().ifEmpty { "Безымянный" },
                        archetype = archetype,
                        str = 5, agi = 5, intStat = 5, cha = 5
                    )
                    val world = WorldDraft(setting = setting, era = era, location = location, tone = tone)
                    onStart(hero, world)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandViolet,
                    contentColor = Color.White
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Старт")
            }

            TextButton(onClick = onBack) {
                Text("Назад", color = BrandMint)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
}

@Composable
private fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) BrandViolet.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.10f)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bg, contentColor = Color.White),
        shape = MaterialTheme.shapes.medium,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
