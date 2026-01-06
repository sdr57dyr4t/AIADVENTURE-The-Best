package com.metalfish.aiadventure.domain.model

data class AiContext(
    val setting: String,
    val era: String,
    val location: String,
    val tone: String,

    val heroName: String,
    val heroClass: String,
    val str: Int,
    val agi: Int,
    val int: Int,
    val cha: Int,

    // ✅ для предыстории (пролога)
    val phase: String = "RUN", // "PROLOGUE" or "RUN"
    val step: Int = 0          // 0..2 (для PROLOGUE)
)
