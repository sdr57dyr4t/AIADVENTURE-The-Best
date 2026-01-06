package com.metalfish.aiadventure.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Shadow
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
import kotlin.math.absoluteValue

@Composable
fun StartScreen(
    onStartWorld: (WorldDraft) -> Unit,
) {
    val context = LocalContext.current.applicationContext

    fun playMusic(rawName: String) {
        MusicPlayer.playRawName(context, rawName)
    }

    val dragThresholdPx = with(LocalDensity.current) { 48.dp.toPx() }
    val imageShape = RoundedCornerShape(20.dp)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
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
            modifier = Modifier.fillMaxSize(),
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
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                worlds.forEachIndexed { index, world ->
                    WorldImageCard(
                        world = world,
                        shape = imageShape,
                        borderColor = borderColor,
                        dragThresholdPx = dragThresholdPx,
                        onStart = onStartWorld,
                        onPlayMusic = { playMusic(world.musicRawName) },
                        staggerMs = 80 * index
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
    dragThresholdPx: Float,
    onStart: (WorldDraft) -> Unit,
    onPlayMusic: () -> Unit,
    staggerMs: Int
) {
    val dragDistance = remember { mutableStateOf(0f) }
    val started = remember { mutableStateOf(false) }
    val xOffset = remember { Animatable(0f) }
    val reveal = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dragVelocity = remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        delay(90L + staggerMs)
        reveal.value = true
        xOffset.snapTo(-24f)
        xOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 420, easing = EaseOutCubic)
        )
    }

    AnimatedVisibility(
        visible = reveal.value,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(360, easing = EaseOutQuad)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .shadow(12.dp, shape)
                .clip(shape)
                .border(1.dp, borderColor, shape)
                .pointerInput(world.imageRes) {
                    detectTapGestures(onTap = { onPlayMusic() })
                }
                .pointerInput(world.imageRes) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragDistance.value = 0f
                            started.value = false
                            dragVelocity.value = 0f
                            onPlayMusic()
                        },
                    onDragCancel = {
                        dragDistance.value = 0f
                        scope.launch {
                            xOffset.animateTo(0f, tween(220, easing = EaseOutQuad))
                        }
                    },
                    onDragEnd = {
                        dragDistance.value = 0f
                        if (!started.value) {
                            scope.launch {
                                xOffset.stop()
                                val inertia = (dragVelocity.value * 160f)
                                    .coerceIn(-120f, 120f)
                                val target = (xOffset.value + inertia).coerceIn(-120f, 120f)
                                xOffset.animateTo(target, tween(220, easing = EaseOutCubic))
                                xOffset.animateTo(0f, tween(260, easing = EaseOutQuad))
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (started.value) return@detectHorizontalDragGestures
                        dragVelocity.value = dragVelocity.value * 0.85f + dragAmount
                        change.consume()
                        dragDistance.value += dragAmount.absoluteValue
                        val target = (xOffset.value + dragAmount).coerceIn(-90f, 90f)
                        scope.launch {
                            xOffset.snapTo(target)
                        }
                        if (dragDistance.value >= dragThresholdPx) {
                            started.value = true
                            onStart(
                                    WorldDraft(
                                        setting = world.setting,
                                        era = world.era,
                                        location = world.location,
                                        tone = world.tone
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
            Image(
                painter = painterResource(id = world.imageRes),
                contentDescription = world.setting,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
                    .offset(x = with(LocalDensity.current) { xOffset.value.toDp() })
            )
        }
    }
}
