package com.metalfish.aiadventure.ui.screens

import android.app.Activity
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.metalfish.aiadventure.ui.audio.MusicPlayer
import com.metalfish.aiadventure.ui.audio.SfxPlayer
import com.metalfish.aiadventure.R
import com.metalfish.aiadventure.ui.model.WorldDraft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

private const val TAG_VIDEO = "StartScreenVideo"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StartScreen(
    onStartWorld: (WorldDraft) -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val carouselState = rememberLazyListState()
    var activeVideoIndex by remember { mutableStateOf<Int?>(null) }
    var activeAudioIndex by remember { mutableStateOf<Int?>(null) }

    fun playMusic(rawName: String) {
        MusicPlayer.playRawName(context, rawName)
    }

    fun startGameWithWorld(world: WorldCard) {
        activeAudioIndex = null
        activeVideoIndex = null
        scope.launch {
            SfxPlayer.fadeOutAndStop(180L)
        }
        MusicPlayer.stop()
        MusicPlayer.playRawName(context, world.musicRawName)
        onStartWorld(world.toWorldDraft())
    }

    val imageShape = RoundedCornerShape(0.dp)
    val borderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    val worlds = listOf(
        WorldCard(
            setting = "SMUTA",
            title = "Смута",
            description = "Начало XVII века. Династия прервалась, и Русь охвачена хаосом. Боярские интриги, голод и интервенты раздирают страну, а самозванцы претендуют на трон.",
            imageRatio = 0.671f,
            era = "Смутное время (1598-1613)",
            location = "Русское государство",
            tone = "ADVENTURE",
            imageRes = R.drawable.card_smuta,
            musicRawName = "music_smuta",
            videoRes = R.raw.card_smuta,
            videoAudioRes = R.raw.card_smuta_audio
        ),
        WorldCard(
            setting = "PETR1",
            title = "Эпоха Петра I",
            description = "Конец XVII - начало XVIII века. Реформы Петра I меняют страну: строится флот, создаётся новая армия, открывается путь к европейским преобразованиям.",
            imageRatio = 0.671f,
            era = "Эпоха Петра I (1682-1725)",
            location = "Россия времен реформ",
            tone = "ADVENTURE",
            imageRes = R.drawable.peter1_intro,
            musicRawName = "music_smuta",
            videoRes = R.raw.card_peter1,
            videoAudioRes = null
        ),
        WorldCard(
            setting = "WAR1812",
            title = "Отечественная война 1812 года",
            description = "1812 год. Вторжение армии Наполеона, Бородинское сражение и народное сопротивление становятся ключевыми событиями борьбы за независимость России.",
            imageRatio = 0.671f,
            era = "Отечественная война 1812 года",
            location = "Россия начала XIX века",
            tone = "ADVENTURE",
            imageRes = R.drawable.war1812_intro,
            musicRawName = "music_smuta",
            videoRes = R.raw.card_war1812,
            videoAudioRes = null
        )
    )
    val carouselStartIndex = remember {
        val base = Int.MAX_VALUE / 2
        base - (base % worlds.size)
    }

    LaunchedEffect(Unit) {
        if (carouselState.firstVisibleItemIndex == 0) {
            carouselState.scrollToItem(carouselStartIndex)
        }
    }
    val selectedIndex by remember {
        derivedStateOf {
            val layoutInfo = carouselState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf 0
            val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo.minByOrNull { info ->
                kotlin.math.abs(info.offset + info.size / 2 - center)
            }?.index ?: 0
        }
    }
    val selectedWorld = worlds[(selectedIndex % worlds.size + worlds.size) % worlds.size]
    val selectedVideoRes = selectedWorld.videoRes
    LaunchedEffect(carouselState, selectedVideoRes) {
        snapshotFlow { Triple(carouselState.isScrollInProgress, selectedIndex, selectedVideoRes) }
            .collectLatest { (scrolling, index, videoRes) ->
                if (scrolling || videoRes == null) {
                    if (activeVideoIndex != null) {
                        Log.d(TAG_VIDEO, "stop video: scrolling=$scrolling videoRes=$videoRes")
                    }
                    activeVideoIndex = null
                    activeAudioIndex = null
                    scope.launch {
                        SfxPlayer.fadeOutAndStop(220L)
                    }
                    return@collectLatest
                }
                Log.d(TAG_VIDEO, "autoplay wait index=$index res=$videoRes")
                delay(1000L)
                if (!carouselState.isScrollInProgress && selectedIndex == index) {
                    Log.d(TAG_VIDEO, "autoplay start index=$index res=$videoRes")
                    activeVideoIndex = index
                    activeAudioIndex = index
                }
            }
    }
    val snapFling = rememberSnapFlingBehavior(lazyListState = carouselState)

    BackHandler {
        MusicPlayer.stop()
        com.metalfish.aiadventure.ui.audio.SfxPlayer.stop()
        activity?.finishAffinity()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val carouselCardW = 240.dp
        val carouselCardH = 360.dp
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                .clickable { onSettings() }
                .zIndex(3f),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Settings",
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
            )
        }

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
            val color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f)
            var y = 0f
            while (y <= this.size.height) {
                var x = 0f
                while (x <= this.size.width) {
                    drawCircle(color = color, radius = radius, center = Offset(x, y))
                    x += spacing
                }
                y += spacing
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(carouselCardH + 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val cardW = carouselCardW
                        val cardH = carouselCardH
                        val sidePadding = (maxWidth - cardW).coerceAtLeast(0.dp) / 2
                        LazyRow(
                            state = carouselState,
                            flingBehavior = snapFling,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = sidePadding,
                                vertical = 8.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.height(cardH + 24.dp)
                        ) {
                            items(Int.MAX_VALUE) { index ->
                                val world = worlds[index % worlds.size]
                                val isSelected = index == selectedIndex
                                val scale = if (isSelected) 1f else 0.92f
                                val elevation = if (isSelected) 16.dp else 10.dp
                            val showVideo = activeVideoIndex == index && world.videoRes != null
                            var videoVisible by remember(world.videoRes, index) { mutableStateOf(false) }
                            LaunchedEffect(showVideo) {
                                if (!showVideo) {
                                    videoVisible = false
                                } else {
                                    Log.d(TAG_VIDEO, "showVideo index=$index res=${world.videoRes}")
                                }
                            }
                            LaunchedEffect(activeAudioIndex, index) {
                                if (activeAudioIndex == index) {
                                    world.videoAudioRes?.let { res ->
                                        SfxPlayer.playRawId(context, res)
                                    }
                                }
                            }
                                Box(
                                    modifier = Modifier
                                        .width(cardW)
                                        .height(cardH)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .shadow(elevation, imageShape)
                                        .clip(imageShape)
                                        .background(Color.Black.copy(alpha = 0.28f))
                                        .clickable {
                                            scope.launch {
                                                carouselState.animateScrollToItem(index)
                                            }
                                        }
                                ) {
                                if (showVideo) {
                                    val videoView = remember(world.videoRes) { VideoView(context) }
                                    DisposableEffect(videoView) {
                                        onDispose { videoView.stopPlayback() }
                                    }
                                    AndroidView(
                                        factory = {
                                            videoView
                                        },
                                        update = { view ->
                                            view.setOnPreparedListener { mp ->
                                                mp.isLooping = false
                                                mp.setVolume(0f, 0f)
                                                mp.setVideoScalingMode(
                                                    MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                                                )
                                                view.start()
                                                Log.d(TAG_VIDEO, "prepared index=$index res=${world.videoRes}")
                                            }
                                            view.setOnInfoListener { _, what, _ ->
                                                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                                    videoVisible = true
                                                    Log.d(TAG_VIDEO, "rendering start index=$index res=${world.videoRes}")
                                                    return@setOnInfoListener true
                                                }
                                                false
                                            }
                                            view.setOnCompletionListener {
                                                videoVisible = false
                                                activeVideoIndex = null
                                                Log.d(TAG_VIDEO, "completed index=$index res=${world.videoRes}")
                                            }
                                            view.setOnErrorListener { _, what, extra ->
                                                videoVisible = false
                                                activeVideoIndex = null
                                                Log.e(
                                                    TAG_VIDEO,
                                                    "error index=$index res=${world.videoRes} what=$what extra=$extra"
                                                )
                                                true
                                            }
                                            val uri = Uri.parse(
                                                "android.resource://${context.packageName}/${world.videoRes}"
                                            )
                                            val current = view.tag as? String
                                            val nextTag = uri.toString()
                                            if (current != nextTag) {
                                                view.tag = nextTag
                                                videoVisible = false
                                                view.setVideoURI(uri)
                                                view.requestFocus()
                                                view.start()
                                                Log.d(TAG_VIDEO, "setVideoURI index=$index uri=$nextTag")
                                            }
                                            if (!view.isPlaying) {
                                                view.requestFocus()
                                                view.start()
                                                Log.d(TAG_VIDEO, "force start index=$index res=${world.videoRes}")
                                            } else if (!videoVisible && view.currentPosition > 0) {
                                                // Avoid black screen by showing video only after frames advance.
                                                view.postDelayed({
                                                    if (activeVideoIndex == index && !videoVisible && view.currentPosition > 0) {
                                                        videoVisible = true
                                                    }
                                                }, 120L)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .alpha(if (videoVisible) 1f else 0f)
                                    )
                                }

                                Image(
                                    painter = painterResource(id = world.imageRes),
                                    contentDescription = world.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(if (videoVisible) 0f else 1f),
                                    alignment = Alignment.Center
                                )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedWorld.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .height(140.dp)
                        .fillMaxWidth(0.9f)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedWorld.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { startGameWithWorld(selectedWorld) },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black.copy(alpha = 0.45f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(0.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                    ) {
                        Text("\u0418\u0433\u0440\u0430\u0442\u044c", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
}

private data class WorldCard(
    val setting: String,
    val title: String,
    val description: String,
    val imageRatio: Float,
    val era: String,
    val location: String,
    val tone: String,
    val imageRes: Int,
    val musicRawName: String,
    val videoRes: Int?,
    val videoAudioRes: Int?
) {
    fun toWorldDraft(): WorldDraft = WorldDraft(setting, era, location, tone)
}


@Composable
private fun WorldImageCard(
    world: WorldCard,
    shape: RoundedCornerShape,
    borderColor: androidx.compose.ui.graphics.Color,
    onStart: (WorldDraft) -> Unit,
    onPlayMusic: () -> Unit,
    staggerMs: Int,
    isDimmed: Boolean,
    demoSwipeOffsetX: Float,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
) {
    val reveal = remember { mutableStateOf(false) }
    val drag = remember { mutableStateOf(Offset.Zero) }
    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }
    val isAnimating = remember { mutableStateOf(false) }
    val isDismissed = remember { mutableStateOf(false) }
    val dismissedAlpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val saturation by animateFloatAsState(
        targetValue = if (isDimmed) 0f else 1f,
        animationSpec = tween(1500, easing = LinearEasing),
        label = "worldCardSaturation"
    )

    LaunchedEffect(Unit) {
        delay(90L + staggerMs)
        reveal.value = true
    }

    AnimatedVisibility(
        visible = reveal.value && !isDismissed.value,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(360, easing = EaseOutQuad)
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .aspectRatio(16f / 9f)
        ) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val threshold = widthPx * 0.22f
            val draggableState = rememberDraggableState { delta ->
                if (isAnimating.value) return@rememberDraggableState
                drag.value = drag.value.copy(
                    x = (drag.value.x + delta).coerceIn(-widthPx, widthPx)
                )
            }
            val baseX = if (isAnimating.value) animX.value else drag.value.x
            val baseY = if (isAnimating.value) animY.value else drag.value.y
            val displayOffset = Offset(baseX + demoSwipeOffsetX, baseY)

            val rotation = ((displayOffset.x / widthPx) * 9f).coerceIn(-10f, 10f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = displayOffset.x
                        translationY = displayOffset.y
                        rotationZ = rotation
                        alpha = dismissedAlpha.value
                    }
                    .shadow(12.dp, shape)
                    .clip(shape)
                    .border(1.dp, borderColor, shape)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = draggableState,
                        onDragStarted = {
                            if (isAnimating.value) return@draggable
                            onPlayMusic()
                            onDragStart()
                        },
                        onDragStopped = {
                            if (isAnimating.value) return@draggable
                            onDragEnd()
                            val dx = drag.value.x
                            val accepted = dx <= -threshold || dx >= threshold
                            scope.launch {
                                isAnimating.value = true
                                animX.snapTo(drag.value.x)
                                animY.snapTo(drag.value.y)
                                if (accepted) {
                                    val targetX = if (dx < 0) -widthPx * 3.0f else widthPx * 3.0f
                                    animX.animateTo(targetX, tween(220, easing = EaseOutQuad))
                                    animY.animateTo(drag.value.y * 0.2f, tween(220, easing = EaseOutQuad))
                                    dismissedAlpha.animateTo(0f, tween(80))
                                    isDismissed.value = true
                                    onStart(world.toWorldDraft())
                                } else {
                                    animX.animateTo(0f, tween(200, easing = EaseOutQuad))
                                    animY.animateTo(0f, tween(200, easing = EaseOutQuad))
                                }
                                if (!accepted) {
                                    drag.value = Offset.Zero
                                    animX.snapTo(0f)
                                    animY.snapTo(0f)
                                }
                                isAnimating.value = false
                            }
                        }
                    )) {
                Image(
                    painter = painterResource(id = world.imageRes),
                    contentDescription = world.setting,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) }),
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun UserWorldCard(
    world: WorldCard,
    shape: RoundedCornerShape,
    borderColor: androidx.compose.ui.graphics.Color,
    onPlayMusic: () -> Unit,
    staggerMs: Int,
    isDimmed: Boolean,
    demoSwipeOffsetX: Float,
    dialogResetTick: Int,
    onRequestDialog: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
) {
    val reveal = remember { mutableStateOf(false) }
    val drag = remember { mutableStateOf(Offset.Zero) }
    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }
    val isAnimating = remember { mutableStateOf(false) }
    val pendingReset = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val saturation by animateFloatAsState(
        targetValue = if (isDimmed) 0f else 1f,
        animationSpec = tween(1500, easing = LinearEasing),
        label = "worldCardSaturation"
    )

    LaunchedEffect(Unit) {
        delay(90L + staggerMs)
        reveal.value = true
    }

    LaunchedEffect(dialogResetTick) {
        if (!pendingReset.value) return@LaunchedEffect
        scope.launch {
            isAnimating.value = true
            animX.animateTo(0f, tween(220, easing = EaseOutQuad))
            animY.animateTo(0f, tween(220, easing = EaseOutQuad))
            drag.value = Offset.Zero
            animX.snapTo(0f)
            animY.snapTo(0f)
            isAnimating.value = false
            pendingReset.value = false
        }
    }

    AnimatedVisibility(
        visible = reveal.value,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(360, easing = EaseOutQuad)
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .aspectRatio(16f / 9f)
        ) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val threshold = widthPx * 0.22f
            val draggableState = rememberDraggableState { delta ->
                if (isAnimating.value) return@rememberDraggableState
                drag.value = drag.value.copy(
                    x = (drag.value.x + delta).coerceIn(-widthPx, widthPx)
                )
            }
            val baseX = if (isAnimating.value) animX.value else drag.value.x
            val baseY = if (isAnimating.value) animY.value else drag.value.y
            val displayOffset = Offset(baseX + demoSwipeOffsetX, baseY)

            val rotation = ((displayOffset.x / widthPx) * 9f).coerceIn(-10f, 10f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = displayOffset.x
                        translationY = displayOffset.y
                        rotationZ = rotation
                    }
                    .shadow(12.dp, shape)
                    .clip(shape)
                    .border(1.dp, borderColor, shape)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = draggableState,
                        onDragStarted = {
                            if (isAnimating.value) return@draggable
                            onPlayMusic()
                            onDragStart()
                        },
                        onDragStopped = {
                            if (isAnimating.value) return@draggable
                            onDragEnd()
                            val dx = drag.value.x
                            scope.launch {
                                isAnimating.value = true
                                animX.snapTo(drag.value.x)
                                animY.snapTo(drag.value.y)
                                if (dx <= -threshold || dx >= threshold) {
                                    val targetX = if (dx < 0) -widthPx * 3.0f else widthPx * 3.0f
                                    animX.animateTo(targetX, tween(220, easing = EaseOutQuad))
                                    animY.animateTo(drag.value.y * 0.2f, tween(220, easing = EaseOutQuad))
                                    pendingReset.value = true
                                    onRequestDialog()
                                } else {
                                    animX.animateTo(0f, tween(200, easing = EaseOutQuad))
                                    animY.animateTo(0f, tween(200, easing = EaseOutQuad))
                                }
                                isAnimating.value = false
                            }
                        }
                    )) {
                Image(
                    painter = painterResource(id = world.imageRes),
                    contentDescription = world.setting,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(saturation) }),
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )

            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CustomWorldDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onGenerate: suspend () -> String
) {
    val cardShape = RoundedCornerShape(0.dp)
    val panelBg = Color.Black.copy(alpha = 0.45f)
    val sectionBg = Color.Black.copy(alpha = 0.35f)
    val borderColor = Color.White.copy(alpha = 0.22f)
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.75f)
    val accent = Color(0xFF9AA4FF)

    var description by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxSize(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val cardW = maxWidth * 0.86f
            val cardH = (cardW * (4f / 3f)).coerceAtMost(maxHeight * 0.80f)
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .size(cardW, cardH),
                shape = cardShape,
                color = panelBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = true)
                            .heightIn(min = 220.dp)
                    ) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("\u0412\u0430\u0448 \u043c\u0438\u0440 \u2014 \u0440\u0430\u0441\u0441\u043a\u0430\u0436\u0438\u0442\u0435 \u043e \u043d\u0451\u043c...", color = textSecondary) },
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(focusRequester)
                                .alpha(if (isGenerating) 0.6f else 1f),
                            shape = cardShape,
                            maxLines = 9,
                            singleLine = false,
                            enabled = !isGenerating,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                cursorColor = accent,
                                focusedContainerColor = sectionBg,
                                unfocusedContainerColor = sectionBg
                            )
                        )

                        if (isGenerating) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    strokeWidth = 3.dp,
                                    color = accent
                                )
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        TextButton(
                            onClick = {
                                if (isGenerating) return@TextButton
                                scope.launch {
                                    isGenerating = true
                                    try {
                                        val text = onGenerate().trim()
                                        description = text
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            },
                            enabled = !isGenerating,
                            colors = ButtonDefaults.textButtonColors(contentColor = textPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isGenerating) 0.6f else 1f)
                        ) {
                            Text("\u041f\u0440\u0438\u0434\u0443\u043c\u0430\u0439 \u0437\u0430 \u043c\u0435\u043d\u044f", textAlign = TextAlign.Center)
                        }

                        TextButton(
                            onClick = onDismiss,
                            enabled = !isGenerating,
                            colors = ButtonDefaults.textButtonColors(contentColor = textPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isGenerating) 0.6f else 1f)
                        ) {
                            Text("\u041e\u0442\u043c\u0435\u043d\u0430", textAlign = TextAlign.Center)
                        }

                        Button(
                            onClick = {
                                if (description.trim().isNotEmpty()) {
                                    keyboard?.hide()
                                    onConfirm(description.trim())
                                }
                            },
                            enabled = description.trim().isNotEmpty() && !isGenerating,
                            shape = cardShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black.copy(alpha = 0.45f),
                                contentColor = textPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isGenerating) 0.6f else 1f)
                        ) {
                            Text("\u0418\u0433\u0440\u0430\u0442\u044c", textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}




