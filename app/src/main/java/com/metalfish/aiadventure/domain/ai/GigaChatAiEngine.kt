package com.metalfish.aiadventure.domain.ai

import android.util.Log
import com.metalfish.aiadventure.BuildConfig
import com.metalfish.aiadventure.data.settings.SettingsStore
import com.metalfish.aiadventure.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GigaChatAiEngine @Inject constructor(
    private val http: HttpClient,
    private val settingsStore: SettingsStore
) : AiEngine {

    private val TAG = "AIAdventure"

    private val chatUrl = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
    private val oauthUrl = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
    private val scope = "GIGACHAT_API_PERS"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Volatile private var cachedToken: String? = null
    @Volatile private var cachedTokenExpiresAtMs: Long = 0L
    @Volatile private var systemPromptSent: Boolean = false
    @Volatile private var storySeed: StorySeed? = null
    private val conversation: MutableList<Msg> = mutableListOf()
    @Volatile private var rateLimitStreak: Int = 0
    @Volatile private var rateLimitExhausted: Boolean = false
    @Volatile private var totalTokens: Int = 0

    override suspend fun nextTurn(
        currentSceneText: String,
        playerChoice: String,
        context: AiContext
    ): AiTurnResult {
        Log.d(TAG, "GigaChatAiEngine.nextTurn CALLED phase=${context.phase} step=${context.step}")
        Log.d(TAG, "context.setting = '${context.setting}'") // ✅ Добавлено: логируем raw значение

        if (context.phase.equals("PROLOGUE", ignoreCase = true) && context.step == 0 && playerChoice == "START") {
            systemPromptSent = false
            conversation.clear()
            storySeed = null
        }

        val authKeyOrToken = BuildConfig.GIGACHAT_AUTH_KEY.trim()
        require(authKeyOrToken.isNotBlank()) { "GIGACHAT_AUTH_KEY is empty. Put it into local.properties and rebuild." }

        val accessToken = getAccessToken(authKeyOrToken)

        // ✅ Исправлено: нормализация и отладка
        val normalizedSetting = context.setting.trim().uppercase()
        Log.d(TAG, "normalizedSetting = '$normalizedSetting'") // ✅ Логируем нормализованное значение

        val gameType = when (normalizedSetting) {
            "POSTAPOC" -> "постапокалипсис"
            "CYBERPUNK" -> "киберпанк"
            "SMUTA" -> "смутное время в России XVII век"
            "PETR1" -> "Россия в эпоху Петра I (конец XVII - начало XVIII века)"
            "WAR1812" -> "Отечественная война 1812 года в России"
            "FANTASY" -> "средневековое фэнтези"
            else -> {
                Log.w(TAG, "Unknown setting: '${context.setting}'. Normalized: '$normalizedSetting'. Using fallback.")
                "средневековое фэнтези"
            }
        }

        Log.d(TAG, "gameType = '$gameType'") // ✅ Логируем итоговый gameType

        if (storySeed == null) {
            storySeed = generateStorySeed(accessToken, gameType)
            if (storySeed == null) {
                storySeed = StorySeed(plot = "(не удалось получить сюжет)", goal = "")
            }
            Log.d(TAG, "Story seed block applied:\n${storySeedBlock()}")
        }

        val raw = requestRaw(
            accessToken = accessToken,
            gameType = gameType,
            settingKey = normalizedSetting,
            userContent = userPrompt(currentSceneText, playerChoice, context)
        ) ?: return if (rateLimitExhausted) {
            rateLimitExhausted = false
            fallbackTurn("Обеденный перерыв! Сервера перегружены.", context)
        } else {
            fallbackTurn("Сеть отвечает странной тишиной. Реальность дрожит и не открывается.", context)
        }

        var scene = parseSceneJsonOrNull(raw)
        if (scene == null) {
            Log.w(TAG, "GigaChat returned non-JSON. Asking to regenerate.")
            val retryRaw = requestRaw(accessToken, gameType, normalizedSetting, repairPrompt(raw))
            scene = retryRaw?.let { parseSceneJsonOrNull(it) }
        }

        if (scene == null) {
            return fallbackTurn("Сеть отвечает странной тишиной. Реальность дрожит и не открывается.", context)
        }

        val leftText = scene.varLeft.trim()
        val rightText = scene.varRight.trim()
        val dayWeather = scene.dayWeather.ifBlank { extractStringField(raw, "dayWeather").orEmpty() }
        val terrain = scene.terrain.ifBlank { extractStringField(raw, "terrain").orEmpty() }

        val choices = listOf(leftText, rightText)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(2)
            .ifEmpty { listOf("Продолжить путь", "Остановиться и осмотреться") }

        val sceneText = scene.sceneDescr.trim().ifBlank {
            extractStringField(raw, "sceneDescr").orEmpty()
        }

        return AiTurnResult(
            sceneText = sceneText.trim(),
            choices = choices,
            outcomeText = "",
            sceneName = scene.sceneName.trim(),
            dayWeather = dayWeather.trim(),
            terrain = terrain.trim(),
            deadPrc = parseDeadPrc(scene.deadPrc),
            statChanges = emptyList(),
            mode = TurnMode.STORY,
            combatOutcome = null,
            heroMind = scene.heroMind.trim(),
            goal = scene.goal.trim(),
            tokensTotal = totalTokens,
            leftAction = null,
            rightAction = null
        )
    }

    override suspend fun generateSettingDescription(prompt: String): String {
        val authKeyOrToken = BuildConfig.GIGACHAT_AUTH_KEY.trim()
        require(authKeyOrToken.isNotBlank()) { "GIGACHAT_AUTH_KEY is empty. Put it into local.properties and rebuild." }

        val accessToken = getAccessToken(authKeyOrToken)
        val raw = requestSimple(accessToken, prompt) ?: return ""
        return raw.trim()
    }

    private fun fallbackTurn(text: String, ctx: AiContext): AiTurnResult {
        return AiTurnResult(
            sceneText = text,
            choices = listOf("Продолжить путь", "Остановиться и осмотреться"),
            outcomeText = "",
            sceneName = "",
            dayWeather = "",
            terrain = "",
            deadPrc = null,
            statChanges = emptyList(),
            mode = TurnMode.STORY,
            combatOutcome = null,
            heroMind = "",
            goal = "",
            tokensTotal = totalTokens
        )
    }

    private fun systemPrompt(gameType: String, settingKey: String): String {
        val historical = isHistoricalSetting(settingKey)
        val intro = if (historical) {
            "Твоя задача — быть Мастером исторической интерактивной истории."
        } else {
            "Твоя задача — быть Мастером игры в текстовой RPG."
        }
        val historicalRules = if (historical) {
            """
*   **Историческая достоверность:** не используй магию, фантастику и анахронизмы.
*   **Точность эпохи:** события, быт, оружие, титулы и география должны соответствовать сеттингу.
*   **Язык эпохи:** используй характерные для эпохи слова и термины (чины, сословия, названия должностей, военные термины), но формулируй понятно для ученика 7 класса.
*   **События эпохи:** в каждой сцене опирайся на реальные события, личности и причинно-следственные связи выбранного периода.
*   **Учебный формат:** в `scene_descr` добавляй 1-2 коротких факта, полезных для изучения эпохи.
"""
        } else {
            ""
        }
        return """
$intro Ты должен полностью управлять игровым процессом, развивая сюжет, сцены и выборы.

${storySeedBlock()}

### 1. Основные правила игры

*   **Сеттинг:** "$gameType".
*   **Пролог:** Первая сцена — опиши предысторию, мир, главного героя и его цель.
*   **Длительность:** Игра должна быть рассчитана не менее чем на 500 ходов (сцен).
*   **Опасность:** Игрок может погибнуть. В начале каждого хода рассчитывай **Шкалу опасности** (`dead_prc`) — вероятность гибели в процентах.
*   **Бой:** Бои длятся не более 5 ходов. Бои должны быть реалистичными.
*   **Динамичный мир:**
    *   **Время суток:** День сменяет ночь каждые 3-5 ходов.
    *   **Погода:** Меняется в зависимости от времени и местности (горы, море и т.д.).
*   **Умные NPC:** Враги должны действовать логично (устраивать засады, звать подмогу).
*   **Выживание:** Герой должен есть, пить и спать, иначе он может погибнуть от истощения.
*   **Взаимодействие:** Жди от пользователя только два варианта ответа.
$historicalRules

### 2. Структура ответа (JSON)

Твой ответ **всегда** должен быть в формате JSON без ограничения длины. Верни ровно один JSON объект.
Все атрибуты должны быть заполнены. Никакого другого текста, кроме JSON.

*   `scene_name` (String): Название сцены.
*   `scene_descr` (String): Подробное описание сцены, которое подразумевает только два выбора дальнейшего развития событий от игрока. Варианты выбора не пиши здесь
*   `var_left` (String): Текст первого варианта выбора.
*   `var_right` (String): Текст второго варианта выбора.
*   `hero_mind` (String): Мысли главного героя, почему он склоняется к одному из вариантов, с учётом `dead_prc`.
*   `goal` (String): Текущая цель героя. **Формируется в прологе** и **не меняется**, пока герой её не достигнет.
*   `day_weather` (String): Время суток и погода (максимум 3 слова).
*   `terrain` (String): Название местности (например, "Лес", "Город", "Горы").
*   `dead_prc` (Integer): Вероятность гибели в процентах (0-100).
""".trimIndent()
    }

    private fun isHistoricalSetting(settingKey: String): Boolean {
        return settingKey in setOf("SMUTA", "PETR1", "WAR1812")
    }

    private fun storySeedBlock(): String {
        val seed = storySeed ?: return ""
        if (seed.plot.isBlank() && seed.goal.isBlank()) return ""
        val plotLine = if (seed.plot.isNotBlank()) "Сюжет: ${seed.plot}" else ""
        val goalLine = if (seed.goal.isNotBlank()) "Цель героя: ${seed.goal}" else ""
        return """
### Сюжет и цель (зафиксированы)
$plotLine
$goalLine
Не изменяй сюжет и цель, пока цель не достигнута.
""".trimIndent()
    }

    private suspend fun generateStorySeed(accessToken: String, gameType: String): StorySeed? {
        val prompt = """
Придумай сюжет и четкую цель для текстовой RPG.
Сеттинг: "$gameType".
Ответь в двух строках:
Сюжет: ...
Цель: ...
""".trimIndent()
        val raw = requestSimple(accessToken, prompt)?.trim().orEmpty()
        if (raw.isBlank()) {
            Log.w(TAG, "Story seed raw is empty.")
            return null
        }
        var plot = extractLabeledLine(raw, "Сюжет")
        var goal = extractLabeledLine(raw, "Цель")
        if (plot.isNullOrBlank() && goal.isNullOrBlank()) {
            val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
            plot = lines.getOrNull(0)
            goal = lines.getOrNull(1)
        }
        if (plot.isNullOrBlank()) {
            plot = raw.take(300).trim()
        }
        if (goal.isNullOrBlank()) {
            goal = ""
        }
        Log.d(TAG, "Story seed raw=$raw")
        Log.d(TAG, "Story seed parsed plot='${plot.orEmpty()}' goal='${goal.orEmpty()}'")
        return StorySeed(plot = plot.orEmpty(), goal = goal.orEmpty())
    }

    private fun extractLabeledLine(raw: String, label: String): String? {
        val regex = Regex("""(?im)^\s*$label\s*[:\\-–—]\s*(.+)\s*$""")
        return regex.find(raw)?.groupValues?.getOrNull(1)?.trim()
    }

    private data class StorySeed(
        val plot: String,
        val goal: String
    )

    private fun userPrompt(cur: String, choice: String, ctx: AiContext): String {
        val trimmed = choice.trim()
        val cleaned = trimmed
            .replace(Regex("^\\s*(LEFT|RIGHT)\\s*:\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
        return when {
            trimmed.startsWith("LEFT", ignoreCase = true) -> "1"
            trimmed.startsWith("RIGHT", ignoreCase = true) -> "2"
            cleaned.isNotBlank() -> cleaned
            else -> "CONTINUE"
        }
    }

    private fun parseSceneJsonOrNull(content: String): SceneJson? {
        val jsonBlock = extractJsonObject(content) ?: return null
        val cleaned = sanitizeJsonArtifacts(jsonBlock)
        val normalized = normalizeSnakeKeys(sanitizeJsonStringNewlines(cleaned))
        return runCatching { json.decodeFromString(SceneJson.serializer(), normalized) }
            .getOrNull()
    }

    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val ch = text[i]
            if (escaped) {
                escaped = false
                continue
            }
            when (ch) {
                '\\' -> if (inString) escaped = true
                '"' -> inString = !inString
                '{' -> if (!inString) depth += 1
                '}' -> if (!inString) {
                    depth -= 1
                    if (depth == 0) {
                        val jsonBlock = text.substring(start, i + 1)
                        val tail = text.substring(i + 1)
                        if (tail.contains('{')) {
                            Log.w(TAG, "Multiple JSON objects detected. Using the first one.")
                        }
                        return jsonBlock
                    }
                }
            }
        }
        return null
    }

    private fun repairPrompt(raw: String): String =
        "Ответь строго JSON по формату из системы. Без переносов строк внутри значений. " +
                "Экранируй кавычки и спецсимволы. Верни ровно один JSON объект. " +
                "Твой прошлый ответ:\n$raw"

    private fun sanitizeJsonStringNewlines(jsonText: String): String {
        val out = StringBuilder(jsonText.length + 16)
        var inString = false
        var escaped = false
        for (ch in jsonText) {
            if (escaped) {
                out.append(ch)
                escaped = false
                continue
            }
            when (ch) {
                '\\' -> {
                    out.append(ch)
                    escaped = true
                }
                '"' -> {
                    out.append(ch)
                    inString = !inString
                }
                '\n', '\r' -> {
                    if (inString) {
                        out.append("\\n")
                    } else {
                        out.append(ch)
                    }
                }
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    private fun normalizeSnakeKeys(jsonText: String): String {
        return jsonText
            .replace("\"scene_name\"", "\"sceneName\"")
            .replace("\"scene_descr\"", "\"sceneDescr\"")
            .replace("\"scene_desc\"", "\"sceneDescr\"")
            .replace("\"scene_descр\"", "\"sceneDescr\"")
            .replace("\"var_left\"", "\"varLeft\"")
            .replace("\"var_right\"", "\"varRight\"")
            .replace("\"hero_mind\"", "\"heroMind\"")
            .replace("\"day_weather\"", "\"dayWeather\"")
            .replace("\"terrain\"", "\"terrain\"")
            .replace("\"dead_prc\"", "\"deadPrc\"")
    }

    private fun parseDeadPrc(value: JsonElement?): Int? {
        if (value == null) return null
        val raw = value.toString().trim().trim('"')
        val match = Regex("(-?\\d+(?:[.,]\\d+)?)").find(raw) ?: return null
        val number = match.value.replace(',', '.').toDoubleOrNull() ?: return null
        val pct = if (number in 0.0..1.0) (number * 100).roundToInt() else number.roundToInt()
        return pct.coerceIn(0, 100)
    }

    private fun extractStringField(raw: String, key: String): String? {
        val jsonBlock = extractJsonObject(raw) ?: return null
        val cleaned = sanitizeJsonArtifacts(jsonBlock)
        val normalized = normalizeSnakeKeys(sanitizeJsonStringNewlines(cleaned))
        val obj = runCatching { json.parseToJsonElement(normalized).jsonObject }.getOrNull() ?: return null
        val value = obj[key] ?: return null
        return (value as? JsonPrimitive)?.content ?: value.toString().trim('"')
    }

    private fun sanitizeJsonArtifacts(jsonText: String): String {
        var out = jsonText.replace("**", "")
        out = out.replace(Regex(""":\s*'([^']*)'(?=\s*[},])""")) { match ->
            val value = match.groupValues[1].replace("\"", "\\\"")
            ": \"$value\""
        }
        return out
    }

    private suspend fun requestRaw(
        accessToken: String,
        gameType: String,
        settingKey: String,
        userContent: String
    ): String? {
        if (!systemPromptSent) {
            conversation.clear()
            conversation.add(Msg(role = "system", content = systemPrompt(gameType, settingKey)))
        }
        conversation.add(Msg(role = "user", content = userContent))
        val msgs = conversation.toList()
        Log.d(TAG, "GigaChat request: systemSent=$systemPromptSent userContent=${userContent.take(400)}")
        val modelName = resolveModelName()
        val req = ChatRequest(
            model = modelName,
            messages = msgs,
            temperature = 0.85
        )
        val reqJson = json.encodeToString(ChatRequest.serializer(), req)
        Log.d(TAG, "GigaChat request json=${reqJson.take(4000)}")

        val response = http.post(chatUrl) {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/json")
            contentType(ContentType.Application.Json)
            setBody(reqJson)
        }
        val responseText = response.bodyAsText()
        Log.d(TAG, "GigaChat response raw=$responseText")
        val isRateLimited = response.status.value == 429 || responseText.contains("\"code\": 429")
        if (isRateLimited) {
            rateLimitStreak += 1
            Log.w(TAG, "GigaChat rate limited: streak=$rateLimitStreak")
            if (rateLimitStreak >= 10) {
                rateLimitExhausted = true
                rateLimitStreak = 0
                return null
            }
            delay(1000)
            return requestRaw(accessToken, gameType, settingKey, userContent)
        } else {
            rateLimitStreak = 0
        }

        val resp = runCatching { json.decodeFromString(ChatResponse.serializer(), responseText) }
            .getOrElse {
                Log.e(TAG, "GigaChat parse ChatResponse failed. Raw=${responseText.take(2000)}", it)
                return null
            }

        val raw = resp.choices.firstOrNull()?.message?.content.orEmpty().trim()
        Log.d(TAG, "GigaChat content raw=$raw")
        systemPromptSent = true
        if (raw.isNotBlank()) {
            conversation.add(Msg(role = "assistant", content = raw))
        }
        val usageTokens = resp.usage?.totalTokens
        if (usageTokens != null) {
            totalTokens += usageTokens
        } else {
            totalTokens += estimateTokensForMessages(msgs) + estimateTokens(raw)
        }
        return raw.ifBlank { null }
    }

    private suspend fun requestSimple(accessToken: String, prompt: String): String? {
        val modelName = resolveModelName()
        val req = ChatRequest(
            model = modelName,
            messages = listOf(Msg(role = "user", content = prompt)),
            temperature = 0.7
        )
        val reqJson = json.encodeToString(ChatRequest.serializer(), req)

        val response = http.post(chatUrl) {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/json")
            contentType(ContentType.Application.Json)
            setBody(reqJson)
        }
        val responseText = response.bodyAsText()
        val isRateLimited = response.status.value == 429 || responseText.contains("\"code\": 429")
        if (isRateLimited) {
            rateLimitStreak += 1
            if (rateLimitStreak >= 10) {
                rateLimitExhausted = true
                rateLimitStreak = 0
                return null
            }
            delay(1000)
            return requestSimple(accessToken, prompt)
        } else {
            rateLimitStreak = 0
        }

        val resp = runCatching { json.decodeFromString(ChatResponse.serializer(), responseText) }
            .getOrElse { return null }

        val raw = resp.choices.firstOrNull()?.message?.content.orEmpty().trim()
        val usageTokens = resp.usage?.totalTokens
        if (usageTokens != null) {
            totalTokens += usageTokens
        } else {
            totalTokens += estimateTokens(prompt) + estimateTokens(raw)
        }
        return raw.ifBlank { null }
    }

    private fun estimateTokensForMessages(messages: List<Msg>): Int {
        var total = 0
        for (msg in messages) {
            total += estimateTokens(msg.content)
        }
        return total
    }

    private fun estimateTokens(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return kotlin.math.ceil(trimmed.length / 4.0).toInt().coerceAtLeast(1)
    }

    private suspend fun resolveModelName(): String {
        val model = runCatching { settingsStore.settings.first().gigaChatModel }
            .getOrDefault(0)
        return when (model) {
            1 -> "GigaChat-2-Pro"
            2 -> "GigaChat-2-Max"
            else -> "GigaChat-2"
        }
    }

    private suspend fun getAccessToken(keyOrToken: String): String {
        val now = System.currentTimeMillis()

        cachedToken?.let { token ->
            if (now + 30_000 < cachedTokenExpiresAtMs) return token
        }

        val looksLikeJwt = keyOrToken.count { it == '.' } >= 2
        if (looksLikeJwt) {
            cachedToken = keyOrToken
            cachedTokenExpiresAtMs = now + 60 * 60 * 1000L
            return keyOrToken
        }

        val basic = if (keyOrToken.startsWith("Basic ", ignoreCase = true)) keyOrToken else "Basic $keyOrToken"
        val rqUid = UUID.randomUUID().toString()
        val bodyForm = "scope=$scope"

        val oauthRaw = http.post(oauthUrl) {
            header("Authorization", basic)
            header("RqUID", rqUid)
            header("Accept", "application/json")
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(bodyForm)
        }.bodyAsText()

        val oauth = runCatching { json.decodeFromString(OAuthResponse.serializer(), oauthRaw) }
            .getOrElse {
                Log.e(TAG, "OAuth parse failed. Raw=${oauthRaw.take(2000)}", it)
                throw IllegalStateException("GigaChat OAuth failed: cannot parse response")
            }

        val token = oauth.accessToken?.trim().orEmpty()
        if (token.isBlank()) {
            Log.e(TAG, "OAuth token is empty. Raw=${oauthRaw.take(2000)}")
            throw IllegalStateException("GigaChat OAuth failed: access_token is empty")
        }

        val expiresAtMs = oauth.expiresAtMs
            ?: (now + ((oauth.expiresInSec ?: 3300L) * 1000L))

        cachedToken = token
        cachedTokenExpiresAtMs = expiresAtMs
        return token
    }

    // ---- DTOs ----

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<Msg>,
        val temperature: Double? = null
    )

    @Serializable
    private data class Msg(
        val role: String,
        val content: String
    )

    @Serializable
    private data class ChatResponse(
        val choices: List<Choice> = emptyList(),
        val usage: Usage? = null
    )

    @Serializable
    private data class Choice(
        val message: OutMsg
    )

    @Serializable
    private data class OutMsg(
        val content: String
    )

    @Serializable
    private data class Usage(
        @SerialName("total_tokens") val totalTokens: Int? = null,
        @SerialName("prompt_tokens") val promptTokens: Int? = null,
        @SerialName("completion_tokens") val completionTokens: Int? = null
    )

    @Serializable
    private data class SceneJson(
        @SerialName("sceneName") val sceneName: String = "",
        @SerialName("sceneDescr") val sceneDescr: String = "",
        @SerialName("varLeft") val varLeft: String = "",
        @SerialName("varRight") val varRight: String = "",
        @SerialName("heroMind") val heroMind: String = "",
        @SerialName("goal") val goal: String = "",
        @SerialName("dayWeather") val dayWeather: String = "",
        @SerialName("terrain") val terrain: String = "",
        @SerialName("deadPrc") val deadPrc: JsonElement? = null
    )

    @Serializable
    private data class OAuthResponse(
        @SerialName("access_token") val accessToken: String? = null,
        @SerialName("expires_at") val expiresAtMs: Long? = null,
        @SerialName("expires_in") val expiresInSec: Long? = null
    )
}
