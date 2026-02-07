package com.metalfish.aiadventure.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.metalfish.aiadventure.R
import com.metalfish.aiadventure.data.settings.AppSettings
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onGigaChatModel: (Int) -> Unit,
    onMusicVolume: (Float) -> Unit,
    onSfxVolume: (Float) -> Unit,
    onClose: () -> Unit
) {
    val cardShape = RoundedCornerShape(0.dp)
    val panelBg = Color.Black.copy(alpha = 0.45f)
    val sectionBg = Color.Black.copy(alpha = 0.35f)
    val borderColor = Color.White.copy(alpha = 0.22f)
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.75f)
    val accent = Color(0xFF9AA4FF)

    var selectedModel by remember(settings.gigaChatModel) {
        mutableStateOf(settings.gigaChatModel.coerceIn(0, 2))
    }
    var musicVolume by remember(settings.musicVolume) {
        mutableStateOf(settings.musicVolume.coerceIn(0f, 1f))
    }
    var sfxVolume by remember(settings.sfxVolume) {
        mutableStateOf(settings.sfxVolume.coerceIn(0f, 1f))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.start_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.85f
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 26.dp.toPx()
            val radius = 1.2.dp.toPx()
            val color = Color.White.copy(alpha = 0.06f)
            var y = 0f
            while (y <= size.height) {
                var x = 0f
                while (x <= size.width) {
                    drawCircle(color = color, radius = radius, center = Offset(x, y))
                    x += spacing
                }
                y += spacing
            }
        }
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val cardW = maxWidth * 0.86f
            Column(
                modifier = Modifier
                    .width(cardW)
                    .heightIn(max = maxHeight * 0.80f)
                    .wrapContentHeight()
                    .background(panelBg, cardShape)
                    .border(2.dp, borderColor, cardShape)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(sectionBg, cardShape)
                        .border(1.dp, borderColor, cardShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "GigaChat модель",
                            style = MaterialTheme.typography.titleMedium,
                            color = textPrimary
                        )
                        Text(
                            text = "lite / pro / max",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                        Slider(
                            value = selectedModel.toFloat(),
                            onValueChange = { selectedModel = it.roundToInt().coerceIn(0, 2) },
                            valueRange = 0f..2f,
                            steps = 1,
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                activeTrackColor = accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                                thumbColor = accent
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("lite", color = textSecondary)
                            Text("pro", color = textSecondary)
                            Text("max", color = textSecondary)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(sectionBg, cardShape)
                        .border(1.dp, borderColor, cardShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "Фоновая музыка",
                            style = MaterialTheme.typography.titleMedium,
                            color = textPrimary
                        )
                        Slider(
                            value = musicVolume,
                            onValueChange = { musicVolume = it.coerceIn(0f, 1f) },
                            valueRange = 0f..1f,
                            steps = 9,
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                activeTrackColor = accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                                thumbColor = accent
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(sectionBg, cardShape)
                        .border(1.dp, borderColor, cardShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = "Звуки",
                            style = MaterialTheme.typography.titleMedium,
                            color = textPrimary
                        )
                        Slider(
                            value = sfxVolume,
                            onValueChange = { sfxVolume = it.coerceIn(0f, 1f) },
                            valueRange = 0f..1f,
                            steps = 9,
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                activeTrackColor = accent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                                thumbColor = accent
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        onGigaChatModel(selectedModel)
                        onMusicVolume(musicVolume)
                        onSfxVolume(sfxVolume)
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.45f),
                        contentColor = textPrimary
                    ),
                    shape = cardShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Text("Ок")
                }
            }
        }
    }
}
