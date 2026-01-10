package com.metalfish.aiadventure.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.metalfish.aiadventure.ui.audio.MusicPlayer
import com.metalfish.aiadventure.R
import com.metalfish.aiadventure.ui.model.WorldDraft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGestures

@Composable
fun StartScreen(
    onStartWorld: (WorldDraft) -> Unit,
) {
    val context = LocalContext.current.applicationContext

    fun playMusic(rawName: String) {
        MusicPlayer.playRawName(context, rawName)
    }

    val imageShape = RoundedCornerShape(0.dp)
    val borderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    val worlds = listOf(
        WorldCard(
            setting = "CYBERPUNK",
            era = "Late 21st century",
            location = "Neon mega-city",
            tone = "DARK",
            imageRes = R.drawable.world_cyberpunk,
            musicRawName = "music_cyberpunk"
        ),
        WorldCard(
            setting = "FANTASY",
            era = "Medieval age",
            location = "Emerald valleys",
            tone = "ADVENTURE",
            imageRes = R.drawable.world_fantasy,
            musicRawName = "music_fantasy"
        ),
        WorldCard(
            setting = "POSTAPOC",
            era = "After the collapse",
            location = "Wasteland frontier",
            tone = "ADVENTURE",
            imageRes = R.drawable.world_postapoc,
            musicRawName = "music_postapoc"
        )
    )
    val activeDragIndex = remember { mutableStateOf<Int?>(null) }
    val isDragging = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.start_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val spacing = 26.dp.toPx()
            val radius = 1.2.dp.toPx()
            val color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(18.dp))

                val titleBrush = Brush.linearGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFF6CF6FF),
                        androidx.compose.ui.graphics.Color(0xFF9D7BFF),
                        androidx.compose.ui.graphics.Color(0xFFFF4DD8)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(600f, 0f)
                )

                Text(
                    text = "AI Adventure",
                    style = MaterialTheme.typography.displaySmall.copy(
                        brush = titleBrush,
                        shadow = Shadow(
                            color = androidx.compose.ui.graphics.Color(0xAA62F6FF),
                            offset = Offset(0f, 0f),
                            blurRadius = 18f
                        )
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(0.95f)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                worlds.forEachIndexed { index, world ->
                    WorldImageCard(
                        world = world,
                        shape = imageShape,
                        borderColor = borderColor,
                        onStart = onStartWorld,
                        onPlayMusic = { playMusic(world.musicRawName) },
                        staggerMs = 80 * index,
                        isDimmed = isDragging.value && activeDragIndex.value != index,
                        onDragStart = {
                            activeDragIndex.value = index
                            isDragging.value = true
                        },
                        onDragEnd = {
                            isDragging.value = false
                            activeDragIndex.value = null
                        }
                    )
                }
            }
        }
    }
}

private data class WorldCard(
    val setting: String,
    val era: String,
    val location: String,
    val tone: String,
    val imageRes: Int,
    val musicRawName: String
)

@Composable
private fun WorldImageCard(
    world: WorldCard,
    shape: RoundedCornerShape,
    borderColor: androidx.compose.ui.graphics.Color,
    onStart: (WorldDraft) -> Unit,
    onPlayMusic: () -> Unit,
    staggerMs: Int,
    isDimmed: Boolean,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
) {
    val reveal = remember { mutableStateOf(false) }
    val drag = remember { mutableStateOf(Offset.Zero) }
    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }
    val isAnimating = remember { mutableStateOf(false) }
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
            val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val threshold = widthPx * 0.22f
            val displayOffset = if (isAnimating.value) {
                Offset(animX.value, animY.value)
            } else {
                drag.value
            }
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
                    .pointerInput(world.imageRes) {
                        detectTapGestures(onTap = { onPlayMusic() })
                    }
                    .pointerInput(world.imageRes) {
                        detectDragGestures(
                            onDragStart = {
                                if (isAnimating.value) return@detectDragGestures
                                onPlayMusic()
                                onDragStart()
                            },
                            onDrag = { change, amount ->
                                if (isAnimating.value) return@detectDragGestures
                                change.consumePositionChange()
                                drag.value = Offset(
                                    x = (drag.value.x + amount.x).coerceIn(-widthPx, widthPx),
                                    y = (drag.value.y + amount.y * 0.35f).coerceIn(-heightPx * 0.18f, heightPx * 0.18f)
                                )
                            },
                            onDragEnd = {
                                if (isAnimating.value) return@detectDragGestures
                                onDragEnd()
                                val dx = drag.value.x
                                scope.launch {
                                    isAnimating.value = true
                                    animX.snapTo(drag.value.x)
                                    animY.snapTo(drag.value.y)
                                    if (dx <= -threshold || dx >= threshold) {
                                        val targetX = if (dx < 0f) -widthPx * 1.2f else widthPx * 1.2f
                                        animX.animateTo(targetX, tween(220, easing = EaseOutQuad))
                                        animY.animateTo(drag.value.y * 0.2f, tween(220, easing = EaseOutQuad))
                                        onStart(
                                            WorldDraft(
                                                setting = world.setting,
                                                era = world.era,
                                                location = world.location,
                                                tone = world.tone
                                            )
                                        )
                                    } else {
                                        animX.animateTo(0f, tween(200, easing = EaseOutQuad))
                                        animY.animateTo(0f, tween(200, easing = EaseOutQuad))
                                    }
                                    drag.value = Offset.Zero
                                    animX.snapTo(0f)
                                    animY.snapTo(0f)
                                    isAnimating.value = false
                                }
                            },
                            onDragCancel = {
                                if (isAnimating.value) return@detectDragGestures
                                onDragEnd()
                                scope.launch {
                                    isAnimating.value = true
                                    animX.snapTo(drag.value.x)
                                    animY.snapTo(drag.value.y)
                                    animX.animateTo(0f, tween(200, easing = EaseOutQuad))
                                    animY.animateTo(0f, tween(200, easing = EaseOutQuad))
                                    drag.value = Offset.Zero
                                    animX.snapTo(0f)
                                    animY.snapTo(0f)
                                    isAnimating.value = false
                                }
                            }
                        )
                    }
            ) {
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
