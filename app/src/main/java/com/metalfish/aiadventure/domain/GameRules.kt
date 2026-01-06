package com.metalfish.aiadventure.domain

import com.metalfish.aiadventure.domain.model.Stats
import kotlin.math.roundToInt

object GameRules {

    fun maxHp(stats: Stats): Int = 50 + stats.strength * 5

    fun maxStamina(stats: Stats): Int = 30 + stats.agility * 4

    fun clampHp(value: Int, stats: Stats): Int = value.coerceIn(0, maxHp(stats))

    fun clampStamina(value: Int, stats: Stats): Int = value.coerceIn(0, maxStamina(stats))

    fun clampGold(value: Int): Int = value.coerceAtLeast(0)

    fun clampReputation(value: Int): Int = value.coerceIn(-100, 100)

    /**
     * Пример “проверки” навыка:
     * successChance = base(0.55) + modifierFromStats - difficultyModifier
     */
    fun successChance(
        stats: Stats,
        difficulty: Int,
        tagModifier: Double = 0.0
    ): Double {
        val base = 0.55
        val statMod =
            (stats.strength - 5) * 0.02 +
                    (stats.agility - 5) * 0.02 +
                    (stats.intelligence - 5) * 0.02 +   // ✅ было intellect
                    (stats.charisma - 5) * 0.02

        val difficultyMod = (difficulty.coerceIn(1, 10) - 5) * 0.05
        return (base + statMod + tagModifier - difficultyMod).coerceIn(0.05, 0.95)
    }

    fun percent(chance: Double): Int = (chance * 100.0).roundToInt().coerceIn(0, 100)
}
