package com.metalfish.aiadventure.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuad
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.clipPath
import com.metalfish.aiadventure.R
import com.metalfish.aiadventure.domain.model.GameUiState
import com.metalfish.aiadventure.ui.audio.MusicPlayer
import com.metalfish.aiadventure.ui.audio.SfxPlayer
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.util.Locale
import kotlin.random.Random
import java.text.NumberFormat

@Composable
fun GameScreen(
    state: GameUiState,
    gigaChatModel: Int,
    onPick: (String) -> Unit,
    onRestart: () -> Unit,
    onSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current.applicationContext

    DisposableEffect(Unit) {
        onDispose {
            MusicPlayer.stop()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {

            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val threshold = widthPx * 0.22f

            val leftChoice = state.choices.getOrNull(0) ?: "Лево"
            val rightChoice = state.choices.getOrNull(1) ?: "Право"

            val bgSetting = state.settingRaw.ifBlank { state.world.setting.name }
            val bgRes = when (bgSetting) {
                "CYBERPUNK" -> R.drawable.bg_cyberpunk
                "POSTAPOC" -> R.drawable.bg_postapoc
                "SMUTA", "PETR1", "WAR1812" -> R.drawable.bg_smuta
                else -> R.drawable.bg_fantasy
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

            var showHeroMind by remember { mutableStateOf(false) }
            var showGoal by remember { mutableStateOf(false) }
            val showDialog = showHeroMind || showGoal
            val canSwipe = !state.isGameOver && !showDialog

            var drag by remember { mutableStateOf(Offset.Zero) }
            var previewDir by remember { mutableStateOf(0) }
            val demoSwipeOffsetX = remember { Animatable(0f) }
            var demoSwipePlayed by remember { mutableStateOf(false) }
            val combinedDragX = drag.x + demoSwipeOffsetX.value
            val rotation = ((combinedDragX / widthPx) * 9f).coerceIn(-10f, 10f)

            val hintText = when {
                drag.x <= -10f -> leftChoice
                drag.x >= 10f -> rightChoice
                else -> ""
            }
            val hintAlpha = ((abs(drag.x) / threshold).coerceIn(0f, 1f) * 0.95f)
            val hintLineHeight = 20.sp
            val hintMinHeight = with(LocalDensity.current) { (hintLineHeight * 2).toDp() } + 20.dp

            val cardW = maxWidth * 0.86f
            val cardH = (cardW * (4f / 3f)).coerceAtMost(maxHeight * 0.80f)
            val previewThreshold = threshold * 0.45f
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
                    listOf(Color(0xFF3A3A3A), Color(0xFF3A3A3A))
                )
            }
            val choiceBg = when (settingName) {
                "CYBERPUNK" -> Color.Black.copy(alpha = 0.45f)
                "POSTAPOC" -> Color(0xFF1A1A1A).copy(alpha = 0.55f)
                else -> Color(0xFF2A2A1A).copy(alpha = 0.45f)
            }
            val hintBg = choiceBg
            val hintBorderBrush = choiceBorderBrush
            val statusBg = when (settingName) {
                "CYBERPUNK" -> Color.Black.copy(alpha = 0.55f)
                "POSTAPOC" -> Color(0xFF141414).copy(alpha = 0.65f)
                else -> Color(0xFF241A12).copy(alpha = 0.55f)
            }
            val statusBorderBrush = choiceBorderBrush
            val sceneFont = 18.sp
            val sceneFontFamily = when (settingName) {
                "CYBERPUNK" -> FontFamily(Font(R.font.cyberpunk_modern))
                "POSTAPOC" -> FontFamily(Font(R.font.postapoc_terminal))
                else -> FontFamily(Font(R.font.fantasy_book))
            }
            val showLoading = state.isWaitingForResponse
            LaunchedEffect(state.turnNumber) {
                if (state.turnNumber == 0) {
                    demoSwipePlayed = false
                }
                showHeroMind = false
                showGoal = false
            }
            LaunchedEffect(state.turnNumber, canSwipe, showLoading) {
                if (!demoSwipePlayed && state.turnNumber == 1 && canSwipe && !showLoading && drag == Offset.Zero) {
                    demoSwipePlayed = true
                    demoSwipeOffsetX.snapTo(0f)
                    demoSwipeOffsetX.animateTo(-threshold * 0.65f, tween(700, easing = EaseOutQuad))
                    demoSwipeOffsetX.animateTo(threshold * 0.65f, tween(900, easing = EaseOutQuad))
                    demoSwipeOffsetX.animateTo(0f, tween(600, easing = EaseOutQuad))
                }
            }
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
            val milkColor = Color(0xFFF5F1E8)
            val loadingArcBrush = when (bgSetting) {
                "SMUTA", "PETR1", "WAR1812" -> SolidColor(milkColor)
                else -> borderBrush
            }
            val loadingTextColor = when (bgSetting) {
                "CYBERPUNK" -> Color(0xFF5BFAFF)
                "POSTAPOC" -> Color(0xFFFFC107)
                "SMUTA", "PETR1", "WAR1812" -> milkColor
                else -> Color(0xFF5A7A3C)
            }

            val modelLabel = when (gigaChatModel) {
                1 -> "pro"
                2 -> "max"
                else -> "lite"
            }
            val tokensCostRub = when (gigaChatModel) {
                1 -> (state.tokensTotal * 1500.0) / 3_000_000.0
                2 -> (state.tokensTotal * 1950.0) / 3_000_000.0
                else -> (state.tokensTotal * 1300.0) / 20_000_000.0
            }
            val numberFormat = remember {
                NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
                    isGroupingUsed = true
                }
            }
            val tokensValueText = numberFormat.format(state.tokensTotal.toLong())
            val tokensLineText = "$modelLabel $tokensValueText"
            val costFormat = remember {
                NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
                    isGroupingUsed = true
                    minimumFractionDigits = 2
                    maximumFractionDigits = 2
                }
            }
            val tokensCostText = costFormat.format(tokensCostRub)

            Box(Modifier.fillMaxSize()) {
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
                            .heightIn(min = hintMinHeight)
                            .background(hintBg, cardShape)
                            .border(2.dp, hintBorderBrush, cardShape)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .alpha(hintAlpha),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = hintText,
                            color = Color.White,
                            fontSize = 16.sp,
                            lineHeight = hintLineHeight,
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
                        .fillMaxSize()
                        .align(Alignment.Center)
                        .graphicsLayer {
                            translationX = combinedDragX
                            translationY = drag.y
                            rotationZ = rotation
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(cardW, cardH)
                            .graphicsLayer {
                                shape = cardShape
                                clip = true
                            }
                            .pointerInput(canSwipe, leftChoice, rightChoice) {
                                if (!canSwipe) return@pointerInput

                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val pointerId = down.id
                                    val slop = viewConfiguration.touchSlop
                                    var total = Offset.Zero
                                    var pastSlop = false
                                    var swipeEnabled = false

                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                        if (!change.pressed) break

                                        val delta = change.positionChange()
                                        if (delta != Offset.Zero) {
                                            total += delta
                                            if (!pastSlop && total.getDistance() > slop) {
                                                pastSlop = true
                                                swipeEnabled = kotlin.math.abs(total.x) > kotlin.math.abs(total.y)
                                                if (!swipeEnabled) {
                                                    drag = Offset.Zero
                                                    previewDir = 0
                                                    return@awaitEachGesture
                                                }
                                            }
                                            if (pastSlop && swipeEnabled) {
                                                change.consumePositionChange()
                                                drag = Offset(
                                                    x = (drag.x + delta.x).coerceIn(-widthPx, widthPx),
                                                    y = (drag.y + delta.y * 0.35f).coerceIn(-heightPx * 0.18f, heightPx * 0.18f)
                                                )
                                                val dir = when {
                                                    drag.x <= -previewThreshold -> -1
                                                    drag.x >= previewThreshold -> 1
                                                    else -> 0
                                                }
                                                if (dir != 0 && dir != previewDir) {
                                                    previewDir = dir
                                                } else if (dir == 0 && previewDir != 0) {
                                                    previewDir = 0
                                                }
                                            }
                                        }
                                    }

                                    if (!pastSlop || !swipeEnabled) {
                                        drag = Offset.Zero
                                        previewDir = 0
                                        return@awaitEachGesture
                                    }

                                    val dx = drag.x
                                    if (dx <= -threshold) {
                                        val picked = "LEFT: $leftChoice"
                                        scope.launch {
                                            SfxPlayer.playRawName(context, "card_filp")
                                            drag = Offset(-widthPx, drag.y * 0.2f)
                                            drag = Offset.Zero
                                            previewDir = 0
                                            onPick(picked)
                                        }
                                    } else if (dx >= threshold) {
                                        val picked = "RIGHT: $rightChoice"
                                        scope.launch {
                                            SfxPlayer.playRawName(context, "card_filp")
                                            drag = Offset(widthPx, drag.y * 0.2f)
                                            drag = Offset.Zero
                                            previewDir = 0
                                            onPick(picked)
                                        }
                                    } else {
                                        drag = Offset.Zero
                                        previewDir = 0
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val edgeGlow =
                            if (settingName == "CYBERPUNK") Color(0xFF4BFBFF).copy(alpha = 0.25f) else Color.Transparent
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f))
                                .border(6.dp, edgeGlow, cardShape)
                                .border(2.dp, borderBrush, cardShape)
                        )

                        if (settingName == "POSTAPOC") {
                            val rustSpots = remember {
                                List(160) { Offset(Random.nextFloat(), Random.nextFloat()) }
                            }
                            val dents = remember {
                                List(36) { Offset(Random.nextFloat(), Random.nextFloat()) }
                            }
                            val scratches = remember {
                                List(40) {
                                    val x1 = Random.nextFloat()
                                    val y1 = Random.nextFloat()
                                    val x2 = (x1 + (Random.nextFloat() - 0.5f) * 0.2f).coerceIn(0f, 1f)
                                    val y2 = (y1 + (Random.nextFloat() - 0.5f) * 0.2f).coerceIn(0f, 1f)
                                    Offset(x1, y1) to Offset(x2, y2)
                                }
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

                                    val dentDark = Color(0xFF2A1A0E).copy(alpha = 0.45f)
                                    val dentLight = Color(0xFFFFD27A).copy(alpha = 0.25f)
                                    dents.forEach { p ->
                                        val rx = p.x * w
                                        val ry = p.y * h
                                        val r = (4.dp + (p.x * 4f).dp).toPx()
                                        drawCircle(dentDark, radius = r, center = Offset(rx, ry))
                                        drawCircle(dentLight, radius = r * 0.55f, center = Offset(rx + r * 0.2f, ry + r * 0.15f))
                                    }

                                    val scratchLight = Color(0xFFFFE6A0).copy(alpha = 0.35f)
                                    val scratchDark = Color(0xFF3A2A18).copy(alpha = 0.35f)
                                    scratches.forEach { (a, b) ->
                                        val p1 = Offset(a.x * w, a.y * h)
                                        val p2 = Offset(b.x * w, b.y * h)
                                        drawLine(scratchDark, p1, p2, strokeWidth = 2.dp.toPx())
                                        drawLine(
                                            scratchLight,
                                            p1 + Offset(1.dp.toPx(), 1.dp.toPx()),
                                            p2 + Offset(1.dp.toPx(), 1.dp.toPx()),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                }
                            }
                        } else if (settingName != "CYBERPUNK") {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val frame = 6.dp.toPx()
                                drawRect(Color(0xFF3A3A3A), size = size, style = Stroke(width = frame))
                            }
                        }

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

                        val showCardText = !showLoading
                        if (showCardText) {
                            if (state.sceneName.isNotBlank()) {
                                val title = if (state.turnNumber > 0) {
                                    "Ход ${state.turnNumber}. ${state.sceneName.trim()}"
                                } else {
                                    state.sceneName.trim()
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 10.dp, start = 12.dp, end = 12.dp)
                                        .fillMaxWidth()
                                            .background(hintBg, cardShape)
                                            .let {
                                                if (settingName == "CYBERPUNK") {
                                                    it.border(2.dp, hintBorderBrush, cardShape)
                                                } else it
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            val textScroll = rememberScrollState()
                            val t = state.sceneText.trim()
                            val textTopPadding = if (state.sceneName.isNotBlank()) 56.dp else 24.dp
                            val innerTextPadding = textPadding + 6.dp

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = innerTextPadding)
                                    .padding(top = textTopPadding, bottom = 92.dp)
                                    .verticalScroll(textScroll),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t,
                                    color = Color.White,
                                    fontSize = sceneFont,
                                    fontFamily = sceneFontFamily,
                                    lineHeight = (sceneFont.value + 4).sp,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Clip,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp, start = 12.dp, end = 12.dp)
                                    .fillMaxWidth()
                                    .background(statusBg, cardShape)
                                    .let {
                                        if (settingName == "CYBERPUNK") {
                                            it.border(2.dp, statusBorderBrush, cardShape)
                                        } else it
                                    }
                                    .height(72.dp)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                val deadPrc = state.deadPrc
                                val level = when {
                                    deadPrc == null -> -1
                                    deadPrc <= 33 -> 0
                                    deadPrc <= 66 -> 1
                                    else -> 2
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = state.dayWeather.trim().ifBlank { "-" },
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Clip
                                        )
                                        Text(
                                            text = state.terrain.trim().ifBlank { "-" },
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 13.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Clip
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .padding(start = 12.dp)
                                            .fillMaxHeight(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                                    .clickable {
                                                        showGoal = true
                                                        showHeroMind = false
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_goal),
                                                    contentDescription = "Goal",
                                                    modifier = Modifier.size(26.dp),
                                                    colorFilter = ColorFilter.tint(Color.White)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                                                    .clickable {
                                                        showHeroMind = true
                                                        showGoal = false
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.ic_mind),
                                                    contentDescription = "Mind",
                                                    modifier = Modifier.size(26.dp),
                                                    colorFilter = ColorFilter.tint(Color.White)
                                                )
                                            }
                                        }
                                        Column(
                                            modifier = Modifier.fillMaxHeight(),
                                            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            val red = if (level == 2) Color(0xFFFF4D4D) else Color(0x55FF4D4D)
                                            val yellow = if (level == 1) Color(0xFFFFC107) else Color(0x55FFC107)
                                            val green = if (level == 0) Color(0xFF5CFF7A) else Color(0x555CFF7A)
                                            Box(Modifier.size(10.dp).background(red, RoundedCornerShape(2.dp)))
                                            Box(Modifier.size(10.dp).background(yellow, RoundedCornerShape(2.dp)))
                                            Box(Modifier.size(10.dp).background(green, RoundedCornerShape(2.dp)))
                                        }
                                    }
                                }
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Canvas(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .rotate(spin)
                                    ) {
                                        val stroke = Stroke(width = 6.dp.toPx())
                                        drawArc(
                                            brush = loadingArcBrush,
                                            startAngle = 0f,
                                            sweepAngle = 280f,
                                            useCenter = false,
                                            style = stroke
                                        )
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    val loadingText = remember(settingName) {
                                        val lines = when (settingName) {
                                            "CYBERPUNK" -> listOf(
                                                "Неон шипит в дожде. Сканирую узел...",
                                                "Прокси-зонд в работе. Держи линию.",
                                                "Сеть трещит. Подгружаю фрагменты реальности...",
                                                "Дрон-курьер несет данные. Еще секунда.",
                                                "Лед трещит под трассировкой. Жди сигнала."
                                            )
                                            "POSTAPOC" -> listOf(
                                                "Пыль в эфире. Собираю обрывки хроник...",
                                                "Старая антенна ловит шум. Подождем.",
                                                "Генератор кашляет. Данные на подходе.",
                                                "Ржавый терминал оживает. Еще немного.",
                                                "Ветер завывает. Радио шепчет ответы..."
                                            )
                                            "SMUTA", "PETR1", "WAR1812" -> listOf(
                                                "Летописец сверяет даты и имена. Формируется новая сцена эпохи...",
                                                "Шелестят страницы учебника. Сюжет выстраивается по историческим фактам.",
                                                "Историческая хроника уточняется. Подожди секунду.",
                                                "Собираю детали времени: люди, события и причины. Почти готово.",
                                                "Сюжет эпохи дополняется достоверными деталями. Еще момент."
                                            )
                                            else -> listOf(
                                                "Скрипят страницы. Летописец пишет...",
                                                "Огонь в очаге мерцает. Сказание рождается.",
                                                "Ветер несет вести. Терпи мгновение.",
                                                "Старинный хроникер вздыхает... почти готово.",
                                                "Сверкнул знак судьбы. История складывается."
                                            )
                                        }
                                        lines.random()
                                    }

                                    Text(
                                        text = loadingText,
                                        color = loadingTextColor,
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .padding(horizontal = 24.dp)
                                    )
                                }
                            }
                        }

                        if (showDialog) {
                            val dialogScroll = rememberScrollState()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .clickable {
                                        showHeroMind = false
                                        showGoal = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .background(Color.Black.copy(alpha = 0.9f), cardShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), cardShape)
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val dialogTitle = if (showGoal) "Цель" else "Мысли героя"
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(hintBg, cardShape)
                                            .let {
                                                if (settingName == "CYBERPUNK") {
                                                    it.border(2.dp, hintBorderBrush, cardShape)
                                                } else it
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = dialogTitle,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .verticalScroll(dialogScroll)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 180.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = if (showGoal) {
                                                    state.goal.ifBlank { "-" }
                                                } else {
                                                    state.heroMind.ifBlank { "-" }
                                                },
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontSize = sceneFont,
                                                fontFamily = sceneFontFamily,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = tokensCostText,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = tokensLineText,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.End
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "\u20BD",
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "\uD83E\uDDE9",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

        }
        }
    }
}


