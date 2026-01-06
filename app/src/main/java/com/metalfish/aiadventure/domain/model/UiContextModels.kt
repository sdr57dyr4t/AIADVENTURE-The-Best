package com.metalfish.aiadventure.domain.model

data class HeroProfileUi(
    val name: String,
    val archetype: ArchetypeUi,
    val stats: HeroStatsUi
) {
    companion object {
        fun default(): HeroProfileUi = HeroProfileUi(
            name = "Безымянный",
            archetype = ArchetypeUi.WARRIOR,
            stats = HeroStatsUi(str = 5, agi = 5, int = 5, cha = 5)
        )
    }
}

data class HeroStatsUi(
    val str: Int,
    val agi: Int,
    val int: Int,
    val cha: Int
)

enum class ArchetypeUi {
    WARRIOR, ROGUE, MAGE, RANGER
}

data class WorldConfigUi(
    val setting: SettingUi,
    val era: String,
    val location: String,
    val tone: ToneUi
) {
    companion object {
        fun default(): WorldConfigUi = WorldConfigUi(
            setting = SettingUi.FANTASY,
            era = "Средневековье",
            location = "Туманные холмы",
            tone = ToneUi.ADVENTURE
        )
    }
}

enum class SettingUi {
    FANTASY, CYBERPUNK, POSTAPOC
}

enum class ToneUi {
    DARK, ADVENTURE, COMEDY
}
