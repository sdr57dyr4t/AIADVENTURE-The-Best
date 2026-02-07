package com.metalfish.aiadventure.domain.image

import com.metalfish.aiadventure.domain.model.HeroProfileUi
import com.metalfish.aiadventure.domain.model.WorldConfigUi

object ImagePromptBuilder {

    fun buildBackground(world: WorldConfigUi): String {
        val setting = when (world.setting.name.uppercase()) {
            "CYBERPUNK" -> "киберпанк, неон, дождь, мегаполис, мокрый асфальт, светящиеся вывески"
            "POSTAPOC" -> "постапокалипсис, руины, пыль, заброшенный город, пустые улицы"
            "SMUTA" -> "Россия, Смутное время, XVII век, древний Кремль, тревожная атмосфера"
            "PETR1" -> "Россия, эпоха Петра I, конец XVII - начало XVIII века, кораблестроение, реформы"
            "WAR1812" -> "Россия, Отечественная война 1812 года, армия, поле боя, дым, костры"
            else -> "историческая Россия, реалистичная атмосфера, исторические детали"
        }

        val tone = when (world.tone.name.uppercase()) {
            "DARK" -> "мрачная атмосфера, драматический свет, тени"
            "COMEDY" -> "легкий тон, теплый свет, мягкая подача"
            else -> "приключенческий тон, кинематографично"
        }

        return """
$setting. ${world.era}, ${world.location}. $tone.
Сцена: атмосферный фон-окружение, широкий кадр, без персонажей, без текста.
Стиль: цифровая иллюстрация, cinematic, реалистично, высокая детализация.
Ограничения: без надписей, без водяных знаков, без логотипов.
""".trimIndent()
            .replace(Regex("[ \\t]+"), " ")
            .trim()
    }

    fun buildCard(
        world: WorldConfigUi,
        hero: HeroProfileUi,
        sceneText: String,
        outcomeText: String? = null
    ): String {
        val story = buildStory(sceneText, outcomeText)

        val setting = when (world.setting.name.uppercase()) {
            "CYBERPUNK" -> "киберпанк, неон, мокрый асфальт, контрастный свет"
            "POSTAPOC" -> "постапокалипсис, руины, пыль, выживание"
            "SMUTA" -> "Россия, Смутное время, XVII век, древние крепости, военные лагеря"
            "PETR1" -> "Россия эпохи Петра I, ранняя империя, верфи, армия нового образца"
            "WAR1812" -> "Россия 1812 года, поле сражения, пехота, кавалерия, дым"
            else -> "историческая Россия, реализм, приключение"
        }

        val tone = when (world.tone.name.uppercase()) {
            "DARK" -> "мрачная атмосфера, напряжение"
            "COMEDY" -> "легкий тон, слегка иронично"
            else -> "приключенческий тон, кинематографично"
        }

        val heroClass = when (hero.archetype.name.uppercase()) {
            "MAGE" -> "маг"
            "ROGUE" -> "плут"
            "RANGER" -> "следопыт"
            else -> "воин"
        }

        return """
$setting. ${world.era}, ${world.location}. $tone.
Герой: ${hero.name}, класс: $heroClass.
Кадр: $story
Стиль: цифровая иллюстрация, cinematic, реалистично, высокая детализация, портрет 3:4.
Ограничения: без текста, без надписей, без водяных знаков, без логотипов.
""".trimIndent()
            .replace(Regex("[ \\t]+"), " ")
            .trim()
    }

    private fun buildStory(sceneText: String, outcomeText: String?): String {
        val s = sceneText.trim()
        val o = outcomeText?.trim().orEmpty()

        val merged = buildString {
            if (o.isNotBlank()) append(o).append(". ")
            append(s)
        }.trim()

        val cleaned = merged
            .replace("\n", " ")
            .replace(Regex("[ \\t]+"), " ")
            .trim()

        val max = 220
        if (cleaned.length <= max) return cleaned

        val cut = cleaned.substring(0, max)
        val last = maxOf(cut.lastIndexOf('.'), cut.lastIndexOf('!'), cut.lastIndexOf('?'))
        return if (last >= 90) cut.substring(0, last + 1).trim() else cut.trim() + "..."
    }
}
