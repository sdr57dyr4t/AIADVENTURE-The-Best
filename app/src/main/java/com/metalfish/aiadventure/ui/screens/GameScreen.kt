package com.metalfish.aiadventure.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metalfish.aiadventure.R
import com.metalfish.aiadventure.domain.model.GameUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    state: GameUiState,
    onPick: (String) -> Unit,
    onRestart: () -> Unit,
    onSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val threshold = widthPx * 0.22f

            val leftChoice = state.choices.getOrNull(0) ?: "Лево"
            val rightChoice = state.choices.getOrNull(1) ?: "Право"

            val bgRes = when (state.world.setting.name) {
                "CYBERPUNK" -> R.drawable.bg_cyberpunk
                "POSTAPOC" -> R.drawable.bg_postapoc
                else -> R.drawable.bg_fantasy
            }

            val cardBitmap: State<ImageBitmap?> = produceState<ImageBitmap?>(initialValue = null, state.imagePath) {
                val path = state.imagePath
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        if (path.isNullOrBlank()) return@runCatching null
                        BitmapFactory.decodeFile(path)?.asImageBitmap()
                    }.getOrNull()
                }
            }

            // BACKGROUND
            Image(
                painter = painterResource(id = bgRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark scrim
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.55f),
                            )
                        )
                    )
            )

            val hasCard = cardBitmap.value != null
            val canSwipe = hasCard && !state.isImageLoading && !state.isGameOver

            var drag by remember { mutableStateOf(Offset.Zero) }
            val rotation = ((drag.x / widthPx) * 9f).coerceIn(-10f, 10f)

            val hintText = when {
                drag.x <= -10f -> leftChoice
                drag.x >= 10f -> rightChoice
                else -> ""
            }
            val hintAlpha = ((abs(drag.x) / threshold).coerceIn(0f, 1f) * 0.95f)

            val cardW = maxWidth * 0.86f
            val cardH = (cardW * (4f / 3f)).coerceAtMost(maxHeight * 0.80f)
            val borderBrush = when (state.world.setting.name) {
                "CYBERPUNK" -> Brush.linearGradient(
                    listOf(Color(0xFF5BFAFF), Color(0xFF8A5CFF), Color(0xFFFF4BD1))
                )
                "POSTAPOC" -> Brush.linearGradient(
                    listOf(Color(0xFFD49A53), Color(0xFF7A4A2A), Color(0xFFB65A3A))
                )
                else -> Brush.linearGradient(
                    listOf(Color(0xFF7CFFB4), Color(0xFF4B8BFF), Color(0xFF9F6BFF))
                )
            }
            val showLoading = state.isWaitingForResponse || state.isImageLoading
            val spin = rememberInfiniteTransition(label = "loading")
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "loadingSpin"
                ).value

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                // HUD small
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.30f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("HP ${state.hero.hp}", color = Color.White, fontSize = 12.sp)
                    Text("ST ${state.hero.stamina}", color = Color.White, fontSize = 12.sp)
                    Text("$ ${state.hero.gold}", color = Color.White, fontSize = 12.sp)
                    Text("REP ${state.hero.reputation}", color = Color.White, fontSize = 12.sp)
                }

                // Hint (shows while swiping)
                AnimatedVisibility(
                    visible = hintText.isNotBlank() && canSwipe,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 72.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .alpha(hintAlpha)
                    ) {
                        Text(
                            text = hintText,
                            color = Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // CARD
                Box(
                    modifier = Modifier
                        .size(cardW, cardH)
                        .clip(RoundedCornerShape(22.dp))
                        .border(2.dp, borderBrush, RoundedCornerShape(22.dp))
                        .rotate(rotation)
                        .offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
                        .pointerInput(canSwipe, leftChoice, rightChoice) {
                            if (!canSwipe) return@pointerInput

                            detectDragGestures(
                                onDrag = { change, amount ->
                                    change.consumePositionChange()
                                    drag = Offset(
                                        x = (drag.x + amount.x).coerceIn(-widthPx, widthPx),
                                        y = (drag.y + amount.y * 0.35f).coerceIn(-heightPx * 0.18f, heightPx * 0.18f)
                                    )
                                },
                                onDragEnd = {
                                    val dx = drag.x
                                    if (dx <= -threshold) {
                                        val picked = "LEFT: $leftChoice"
                                        scope.launch {
                                            drag = Offset(-widthPx, drag.y * 0.2f)
                                            drag = Offset.Zero
                                            onPick(picked)
                                        }
                                    } else if (dx >= threshold) {
                                        val picked = "RIGHT: $rightChoice"
                                        scope.launch {
                                            drag = Offset(widthPx, drag.y * 0.2f)
                                            drag = Offset.Zero
                                            onPick(picked)
                                        }
                                    } else {
                                        scope.launch { drag = Offset.Zero }
                                    }
                                },
                                onDragCancel = { scope.launch { drag = Offset.Zero } }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = cardBitmap.value
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f))
                        )
                    }

                    if (bmp != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.10f),
                                            0.45f to Color.Black.copy(alpha = 0.40f),
                                            1.0f to Color.Black.copy(alpha = 0.65f),
                                        )
                                    )
                                )
                        )

                        val t = state.sceneText.trim()
                        val font = when {
                            t.length <= 140 -> 20.sp
                            t.length <= 260 -> 18.sp
                            t.length <= 420 -> 16.sp
                            else -> 15.sp
                        }

                        Text(
                            text = t,
                            color = Color.White,
                            fontSize = font,
                            lineHeight = (font.value + 6).sp,
                            textAlign = TextAlign.Center,
                            maxLines = 9,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .size(72.dp)
                                    .rotate(spin)
                            ) {
                                val stroke = Stroke(width = 6.dp.toPx())
                                drawArc(
                                    brush = borderBrush,
                                    startAngle = 0f,
                                    sweepAngle = 280f,
                                    useCenter = false,
                                    style = stroke
                                )
                            }
                            Text(
                                text = "Generating...",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !state.isGameOver,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 22.dp)
                ) {
                    Text(
                        text = if (canSwipe) "Свайпни: ← / →" else "Подождите…",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

