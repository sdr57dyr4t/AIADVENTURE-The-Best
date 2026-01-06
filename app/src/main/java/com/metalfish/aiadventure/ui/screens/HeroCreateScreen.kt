package com.metalfish.aiadventure.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metalfish.aiadventure.ui.components.GlassCard
import com.metalfish.aiadventure.ui.components.ScreenBackground
import com.metalfish.aiadventure.ui.model.HeroDraft

@Composable
fun HeroCreateScreen(
    onBack: () -> Unit,
    onNext: (HeroDraft) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var archetype by remember { mutableStateOf("WARRIOR") }

    var str by remember { mutableIntStateOf(5) }
    var agi by remember { mutableIntStateOf(5) }
    var intel by remember { mutableIntStateOf(5) }
    var cha by remember { mutableIntStateOf(5) }

    val total = str + agi + intel + cha
    val pointsLeft = 20 - total // 4*5=20 базово, далее уменьшаем/увеличиваем

    fun clampAll() {
        str = str.coerceIn(1, 10)
        agi = agi.coerceIn(1, 10)
        intel = intel.coerceIn(1, 10)
        cha = cha.coerceIn(1, 10)
    }

    ScreenBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onBack) { Text("Назад") }
                Text(
                    text = "Герой",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(72.dp))
            }

            Spacer(Modifier.height(12.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(18) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Имя") },
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                Text("Класс", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))

                SingleChoiceRow(
                    options = listOf("WARRIOR" to "Воин", "ROGUE" to "Плут", "MAGE" to "Маг", "RANGER" to "Следопыт"),
                    selected = archetype,
                    onSelect = { archetype = it }
                )

                Spacer(Modifier.height(12.dp))
                Text("Очки: ${pointsLeft.coerceAtLeast(0)}", color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(Modifier.height(10.dp))

                StatRow("Сила", str, pointsLeft, onInc = { if (pointsLeft > 0 && str < 10) str++ }, onDec = { if (str > 1) str-- })
                StatRow("Ловкость", agi, pointsLeft, onInc = { if (pointsLeft > 0 && agi < 10) agi++ }, onDec = { if (agi > 1) agi-- })
                StatRow("Интеллект", intel, pointsLeft, onInc = { if (pointsLeft > 0 && intel < 10) intel++ }, onDec = { if (intel > 1) intel-- })
                StatRow("Харизма", cha, pointsLeft, onInc = { if (pointsLeft > 0 && cha < 10) cha++ }, onDec = { if (cha > 1) cha-- })

                Spacer(Modifier.height(14.dp))

                val canContinue = name.trim().isNotEmpty() && pointsLeft == 0
                Button(
                    onClick = {
                        clampAll()
                        onNext(
                            HeroDraft(
                                name = name.trim(),
                                archetype = archetype,
                                str = str,
                                agi = agi,
                                intStat = intel,
                                cha = cha
                            )
                        )
                    },
                    enabled = canContinue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Дальше")
                }

                if (name.trim().isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Введите имя.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (pointsLeft != 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Распределите ровно 10 очков.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SingleChoiceRow(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, title) ->
            val isSelected = key == selected
            OutlinedButton(
                onClick = { onSelect(key) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            ) { Text(title, maxLines = 1) }
        }
    }
}

@Composable
private fun StatRow(
    title: String,
    value: Int,
    pointsLeft: Int,
    onInc: () -> Unit,
    onDec: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDec, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) { Text("−") }
            Text("$value", modifier = Modifier.width(22.dp))
            OutlinedButton(onClick = onInc, enabled = pointsLeft > 0, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) { Text("+") }
        }
    }
}
