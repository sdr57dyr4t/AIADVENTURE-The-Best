package com.metalfish.aiadventure.ui.audio

import android.content.Context
import android.media.MediaPlayer

object MusicPlayer {
    private var player: MediaPlayer? = null
    private var currentResId: Int? = null
    private var volume: Float = 1f

    fun playRawName(context: Context, rawName: String) {
        val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
        if (resId == 0) return
        if (volume <= 0f) return

        if (currentResId == resId && player?.isPlaying == true) return

        player?.release()
        player = MediaPlayer.create(context, resId)?.apply {
            isLooping = true
            setVolume(volume, volume)
            start()
        }
        currentResId = resId
    }

    fun setVolume(level: Float) {
        volume = level.coerceIn(0f, 1f)
        if (volume <= 0f) {
            stop()
        } else {
            player?.setVolume(volume, volume)
        }
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
        currentResId = null
    }
}
