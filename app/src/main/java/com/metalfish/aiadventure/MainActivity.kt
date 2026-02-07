package com.metalfish.aiadventure

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.metalfish.aiadventure.ui.AppRoot
import com.metalfish.aiadventure.ui.audio.SfxPlayer
import com.metalfish.aiadventure.ui.theme.AIAdventureTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var insetsController: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        insetsController = WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        setContent {
            // Р’РђР–РќРћ: СѓР±СЂР°Р»Рё РїР°СЂР°РјРµС‚СЂС‹ darkTheme/textSize,
            // РїРѕС‚РѕРјСѓ С‡С‚Рѕ РІ С‚РІРѕРµР№ AIAdventureTheme РёС… РЅРµС‚ (РїРѕ РѕС€РёР±РєРµ РєРѕРјРїРёР»СЏС‚РѕСЂР°)
            AIAdventureTheme {
                var showSplash by remember { mutableStateOf(true) }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    val durationMs = withContext(Dispatchers.IO) {
                        getRawDurationMs(context, R.raw.ai_intro)
                    }
                    delay(durationMs + 1000L)
                    showSplash = false
                }

                if (showSplash) {
                    SplashScreen()
                } else {
                    AppRoot()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
private fun SplashScreen() {
    val context = LocalContext.current
    val videoView = remember { VideoView(context) }
    LaunchedEffect(Unit) {
        SfxPlayer.playRawName(context, "ai_intro_audio")
    }
    DisposableEffect(Unit) {
        onDispose {
            SfxPlayer.stop()
            videoView.stopPlayback()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.splash_background)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = {
                videoView.apply {
                    setVideoURI(Uri.parse("android.resource://${context.packageName}/${R.raw.ai_intro}"))
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                    }
                    start()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        )
    }
}

private fun getRawDurationMs(context: Context, resId: Int): Long {
    val afd = context.resources.openRawResourceFd(resId) ?: return 3000L
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 3000L
    } catch (_: Exception) {
        3000L
    } finally {
        retriever.release()
        afd.close()
    }
}
