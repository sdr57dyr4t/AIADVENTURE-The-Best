package com.metalfish.aiadventure.ui.audio

import android.content.Context
import android.media.MediaPlayer

object MusicPlayer {
    private var player: MediaPlayer? = null
    private var currentResId: Int? = null

    fun playRawName(context: Context, rawName: String) {
        val resId = context.resources.getIdentifier(rawName, "raw", context.packageName)
        if (resId == 0) return

        if (currentResId == resId && player?.isPlaying == true) return

        player?.release()
        player = MediaPlayer.create(context, resId)?.apply { start() }
        currentResId = resId
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
        currentResId = null
    }
}
