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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
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
import kotlin.random.Random

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

            val canSwipe = !state.isImageLoading && !state.isGameOver

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
            val textPadding = 18.dp
            val settingName = state.world.setting.name
            val cardShape = RoundedCornerShape(0.dp)
            val borderBrush = when (settingName) {
                "CYBERPUNK" -> Brush.linearGradient(
                    listOf(Color(0xFF5BFAFF), Color(0xFF8A5CFF), Color(0xFFFF4BD1))
                )
                "POSTAPOC" -> Brush.linearGradient(
                    listOf(Color(0xFF1B1B1B), Color(0xFFFFC107), Color(0xFF7A4A2A))
                )
                else -> Brush.linearGradient(
                    listOf(Color(0xFF3E2A1A), Color(0xFF5A7A3C), Color(0xFF2E4B2E))
                )
            }
            val choiceBorderBrush = when (settingName) {
                "CYBERPUNK" -> Brush.linearGradient(
                    listOf(Color(0xFF5BFAFF), Color(0xFF8A5CFF))
                )
                "POSTAPOC" -> Brush.linearGradient(
                    listOf(Color(0xFFFFC107), Color(0xFF1B1B1B))
                )
                else -> Brush.linearGradient(
                    listOf(Color(0xFF5A7A3C), Color(0xFF3E2A1A))
                )
            }
            val choiceBg = when (settingName) {
                "CYBERPUNK" -> Color.Black.copy(alpha = 0.45f)
                "POSTAPOC" -> Color(0xFF1A1A1A).copy(alpha = 0.55f)
                else -> Color(0xFF2A2A1A).copy(alpha = 0.45f)
            }
            val hintBg = choiceBg
            val hintBorderBrush = choiceBorderBrush
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
                            .width(cardW)
                            .background(hintBg, cardShape)
                            .border(2.dp, hintBorderBrush, cardShape)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .alpha(hintAlpha)
                    ) {
                        Text(
                            text = hintText,
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // CARD
                Box(
                    modifier = Modifier
                        .size(cardW, cardH)
                        .clip(cardShape)
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
                    val showText = !showLoading
                    if (bmp != null && showText) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    6.dp,
                                    if (settingName == "CYBERPUNK") Color(0xFF4BFBFF).copy(alpha = 0.25f) else Color.Transparent,
                                    cardShape
                                )
                                .border(2.dp, borderBrush, cardShape)
                        ) {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f))
                                .border(
                                    6.dp,
                                    if (settingName == "CYBERPUNK") Color(0xFF4BFBFF).copy(alpha = 0.25f) else Color.Transparent,
                                    cardShape
                                )
                                .border(2.dp, borderBrush, cardShape)
                        )
                    }

                    if (settingName == "POSTAPOC") {
                        val rustSpots = remember {
                            List(160) { Offset(Random.nextFloat(), Random.nextFloat()) }
                        }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 8.dp.toPx()
                            val border = 12.dp.toPx()
                            val w = size.width
                            val h = size.height
                            val yellow = Color(0xFFFFC107).copy(alpha = 0.65f)
                            val black = Color(0xFF1A1A1A).copy(alpha = 0.75f)

                            val borderPath = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(0f, 0f, w, h))
                                addRect(Rect(border, border, w - border, h - border))
                            }

                            clipPath(borderPath) {
                                val stripeStep = stroke * 1.4f
                                var x = -h
                                while (x < w + h) {
                                    drawLine(
                                        if (((x / stripeStep).toInt() % 2) == 0) yellow else black,
                                        Offset(x, 0f),
                                        Offset(x + h, h),
                                        strokeWidth = stroke
                                    )
                                    x += stripeStep
                                }

                                val rust = Color(0xFF8B4A2A).copy(alpha = 0.35f)
                                rustSpots.forEach { p ->
                                    val rx = p.x * w
                                    val ry = p.y * h
                                    drawCircle(rust, radius = 3.dp.toPx(), center = Offset(rx, ry))
                                }
                            }
                        }
                    } else if (settingName != "CYBERPUNK") {
                        data class Knot(val edge: Int, val t: Float, val inset: Float, val radius: Float)
                        val knots = remember {
                            List(28) {
                                Knot(
                                    edge = Random.nextInt(4),
                                    t = Random.nextFloat(),
                                    inset = Random.nextFloat(),
                                    radius = 2f + Random.nextFloat() * 5f
                                )
                            }
                        }
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val plank = 12.dp.toPx()
                            val wood = Color(0xFF6A4528)
                            val woodDark = Color(0xFF3E2816)
                            val grain = Color(0xFF8A5A36).copy(alpha = 0.35f)
                            val w = size.width
                            val h = size.height

                            drawRect(wood, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, plank))
                            drawRect(wood, topLeft = Offset(0f, h - plank), size = androidx.compose.ui.geometry.Size(w, plank))
                            drawRect(wood, topLeft = Offset(0f, plank), size = androidx.compose.ui.geometry.Size(plank, h - 2 * plank))
                            drawRect(wood, topLeft = Offset(w - plank, plank), size = androidx.compose.ui.geometry.Size(plank, h - 2 * plank))

                            repeat(6) { i ->
                                val y = plank * 0.35f + i * 3.dp.toPx()
                                drawLine(grain, Offset(0f, y), Offset(w, y), strokeWidth = 1.5.dp.toPx())
                                val yb = h - plank * 0.35f - i * 3.dp.toPx()
                                drawLine(grain, Offset(0f, yb), Offset(w, yb), strokeWidth = 1.5.dp.toPx())
                            }
                            repeat(6) { i ->
                                val x = plank * 0.35f + i * 3.dp.toPx()
                                drawLine(grain, Offset(x, 0f), Offset(x, h), strokeWidth = 1.5.dp.toPx())
                                val xr = w - plank * 0.35f - i * 3.dp.toPx()
                                drawLine(grain, Offset(xr, 0f), Offset(xr, h), strokeWidth = 1.5.dp.toPx())
                            }

                            drawRect(woodDark, size = size, style = Stroke(width = 2.dp.toPx()))

                            knots.forEach { k ->
                                val pos = when (k.edge) {
                                    0 -> Offset(k.t * w, plank * (0.35f + 0.45f * k.inset))
                                    1 -> Offset(w - plank * (0.35f + 0.45f * k.inset), k.t * h)
                                    2 -> Offset(k.t * w, h - plank * (0.35f + 0.45f * k.inset))
                                    else -> Offset(plank * (0.35f + 0.45f * k.inset), k.t * h)
                                }
                                drawCircle(woodDark, radius = k.radius.dp.toPx(), center = pos)
                            }
                        }
                    }

                    if (showText) {
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

                        val textScroll = rememberScrollState()
                        val t = state.sceneText.trim()
                        val font = 18.sp
                        val fontFamily = when (settingName) {
                            "CYBERPUNK" -> FontFamily(Font(R.font.cyberpunk_modern))
                            "POSTAPOC" -> FontFamily(Font(R.font.postapoc_terminal))
                            else -> FontFamily(Font(R.font.fantasy_book))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = textPadding)
                                .verticalScroll(textScroll),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t,
                                color = Color.White,
                                fontSize = font,
                                fontFamily = fontFamily,
                                lineHeight = (font.value + 4).sp,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
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
                            val loadingText = remember {
                                listOf(
                                    "Прокладываю маршрут…",
                                    "Собираю артефакты…",
                                    "Поджигаю неон…",
                                    "Шью мир по нитям…",
                                    "Пыль оседает…"
                                ).random()
                            }
                            Text(
                                text = loadingText,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                            )
                        }
                    }
                }

            }
        }
    }
}

