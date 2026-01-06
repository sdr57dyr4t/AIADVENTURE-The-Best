package com.metalfish.aiadventure.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.ui.unit.sp

/**
 * Премиальная типографика без кастомных шрифтов (чтобы не требовать файлов .ttf)
 * - Заголовки: Serif (атмосфера RPG)
 * - Текст/интерфейс: SansSerif (читабельно)
 *
 * ВАЖНО:
 * TextUnit может быть Unspecified (особенно letterSpacing), и любые арифметические операции с ним падают.
 */
fun adventureTypography(scale: Float): Typography {
    val serif = FontFamily.Serif
    val sans = FontFamily.SansSerif

    fun scaleIfSpecified(v: TextUnit, scale: Float): TextUnit {
        return if (v.isUnspecified) v else v * scale
    }

    fun TextStyle.s(scale: Float) = copy(
        fontSize = scaleIfSpecified(fontSize, scale),
        lineHeight = scaleIfSpecified(lineHeight, scale),
        letterSpacing = scaleIfSpecified(letterSpacing, scale)
    )

    val base = Typography(
        headlineLarge = TextStyle(
            fontFamily = serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            letterSpacing = (-0.2).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            letterSpacing = (-0.1).sp
        ),
        titleLarge = TextStyle(
            fontFamily = serif,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 22.sp
            // letterSpacing по умолчанию Unspecified — теперь это безопасно
        ),
        titleMedium = TextStyle(
            fontFamily = sans,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = sans,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 20.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = sans,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        labelLarge = TextStyle(
            fontFamily = sans,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp
        ),
        labelMedium = TextStyle(
            fontFamily = sans,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.2.sp
        )
    )

    return Typography(
        headlineLarge = base.headlineLarge.s(scale),
        headlineMedium = base.headlineMedium.s(scale),
        titleLarge = base.titleLarge.s(scale),
        titleMedium = base.titleMedium.s(scale),
        bodyLarge = base.bodyLarge.s(scale),
        bodyMedium = base.bodyMedium.s(scale),
        labelLarge = base.labelLarge.s(scale),
        labelMedium = base.labelMedium.s(scale)
    )
}
