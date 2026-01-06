package com.metalfish.aiadventure.domain.image

interface ImageEngine {
    suspend fun generateImageBytes(
        prompt: String,
        width: Int = 768,
        height: Int = 1024,
        seed: Long? = null
    ): ByteArray
}
