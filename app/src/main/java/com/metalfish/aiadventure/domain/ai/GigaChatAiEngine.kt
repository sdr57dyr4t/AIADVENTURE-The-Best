package com.metalfish.aiadventure.domain.ai

import android.util.Log
import com.metalfish.aiadventure.BuildConfig
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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GigaChatAiEngine @Inject constructor(
    private val http: HttpClient
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
    private val conversation: MutableList<Msg> = mutableListOf()

    override suspend fun nextTurn(
        currentSceneText: String,
        playerChoice: String,
        context: AiContext
    ): AiTurnResult {
        Log.d(TAG, "GigaChatAiEngine.nextTurn CALLED phase=${context.phase} step=${context.step}")
        if (context.phase.equals("PROLOGUE", ignoreCase = true) && context.step == 0 && playerChoice == "START") {
            systemPromptSent = false
            conversation.clear()
        }

        val authKeyOrToken = BuildConfig.GIGACHAT_AUTH_KEY.trim()
        require(authKeyOrToken.isNotBlank()) { "GIGACHAT_AUTH_KEY is empty. Put it into local.properties and rebuild." }

        val accessToken = getAccessToken(authKeyOrToken)

        val raw = requestRaw(
            accessToken = accessToken,
            userContent = userPrompt(currentSceneText, playerChoice, context)
        ) ?: return fallbackTurn("Сеть отвечает странной тишиной. Реальность дрожит и не открывается.", context)

        var scene = parseSceneJsonOrNull(raw)
        if (scene == null) {
            Log.w(TAG, "GigaChat returned non-JSON. Asking to regenerate.")
            val retryRaw = requestRaw(accessToken, repairPrompt(raw))
            scene = retryRaw?.let { parseSceneJsonOrNull(it) }
        }

        if (scene == null) {
            return fallbackTurn("Сеть отвечает странной тишиной. Реальность дрожит и не открывается.", context)
        }

        val choices = listOf(scene.varLeft, scene.varRight).map { it.trim() }.filter { it.isNotBlank() }
            .take(2)
            .ifEmpty { listOf("Продолжить путь", "Остановиться и осмотреться") }

        return AiTurnResult(
            sceneText = scene.sceneDescr.trim().take(320),
            choices = choices,
            outcomeText = "",
            statChanges = emptyList(),
            imagePrompt = scene.imgPrmt.ifBlank { buildFallbackImagePrompt(context, scene.sceneDescr) },
            mode = TurnMode.STORY,
            combatOutcome = null
        )
    }

    private fun fallbackTurn(text: String, ctx: AiContext): AiTurnResult {
        return AiTurnResult(
            sceneText = text.take(320),
            choices = listOf("Продолжить путь", "Остановиться и осмотреться"),
            outcomeText = "",
            statChanges = emptyList(),
            imagePrompt = buildFallbackImagePrompt(ctx, text),
            mode = TurnMode.STORY,
            combatOutcome = null
        )
    }

    private fun systemPrompt(): String = """
Играем в текстовую rpg игру в мрачном фэнтезийном сеттинге, соответствующем средневековью. 
Придумай сюжет. Сюжет должен быть небанальным и развиваться постепенно. Старт сюжета - какое то необычное событие.
Первая сцена игры - пролог. Где подробно опиши предысторию, мир вокруг, главного героя, его имя и цель.
Главный герой не может на первых ходах сразу побеждать всех противников или разгадывать сложные квесты - это должно происходить по мере развития сюжета. 
После пролога начинается игра - ты описываешь сцену и варианты действий. Я выбираю один из двух вариантов. Игра должна проходиться не менее чем за 500 ходов. Одна сцена - один ход.
Во время игры персонаж может погибнуть, если игрок сделает неверный выбор. 
Должны присутствовать сцены боя. Бой должен длиться не больше пяти ходов, на первом ходе боя я должен выбрать оружие. Нельзя использовать оружие, которого у тебя нет. Бой должен быть реалистичным - персонаж не может голыми руками одолеть нескольких противников.
Добавь смену дня и ночи. А также смену погоды.
Персонаж должен есть, пить и спать. Недостаток сна или еды может привести к гибели персонажа.
Ответ возвращай строго в формате JSON, без переносов строк внутри значений.
Ответ верни JSON с полями из кода: sceneDescr, imgPrmt, varLeft, varRight, musicType, dayWeather, terrain, turn
""".trimIndent()

    private fun userPrompt(cur: String, choice: String, ctx: AiContext): String =
        choice.trim().ifBlank { "CONTINUE" }

    private fun parseSceneJsonOrNull(content: String): SceneJson? {
        val jsonBlock = extractJsonObject(content) ?: return null
        return runCatching { json.decodeFromString(SceneJson.serializer(), jsonBlock) }
            .getOrNull()
            ?: runCatching {
                val sanitized = sanitizeJsonStringNewlines(jsonBlock)
                json.decodeFromString(SceneJson.serializer(), sanitized)
            }.getOrNull()
            ?: runCatching {
                val normalized = normalizeSnakeKeys(sanitizeJsonStringNewlines(jsonBlock))
                json.decodeFromString(SceneJson.serializer(), normalized)
            }.getOrNull()
    }

    private fun extractJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }

    private fun repairPrompt(raw: String): String =
        "Ответь строго JSON по формату из системы. Без переносов строк внутри значений. " +
            "Экранируй кавычки и спецсимволы. Верни только JSON. Твой прошлый ответ:\n$raw"

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
            .replace("\"scene_descr\"", "\"sceneDescr\"")
            .replace("\"img_prmt\"", "\"imgPrmt\"")
            .replace("\"var_left\"", "\"varLeft\"")
            .replace("\"var_right\"", "\"varRight\"")
            .replace("\"music_type\"", "\"musicType\"")
            .replace("\"day_weather\"", "\"dayWeather\"")
            .replace("\"terrain\"", "\"terrain\"")
            .replace("\"turn\"", "\"turn\"")
    }

    private suspend fun requestRaw(accessToken: String, userContent: String): String? {
        if (!systemPromptSent) {
            conversation.clear()
            conversation.add(Msg(role = "system", content = systemPrompt()))
        }
        conversation.add(Msg(role = "user", content = userContent))
        val msgs = conversation.toList()
        Log.d(TAG, "GigaChat request: systemSent=$systemPromptSent userContent=${userContent.take(400)}")
        val req = ChatRequest(
            model = "GigaChat-2",
            messages = msgs,
            temperature = 0.85
        )

        val responseText = http.post(chatUrl) {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/json")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatRequest.serializer(), req))
        }.bodyAsText()

        val resp = runCatching { json.decodeFromString(ChatResponse.serializer(), responseText) }
            .getOrElse {
                Log.e(TAG, "GigaChat parse ChatResponse failed. Raw=${responseText.take(2000)}", it)
                return null
            }

        val raw = resp.choices.firstOrNull()?.message?.content.orEmpty().trim()
        Log.d(TAG, "GigaChat content raw=${raw.take(400)}")
        systemPromptSent = true
        if (raw.isNotBlank()) {
            conversation.add(Msg(role = "assistant", content = raw))
        }
        return raw.ifBlank { null }
    }

    private fun buildFallbackImagePrompt(ctx: AiContext, sceneText: String): String {
        val base = "dark historical scene, realistic, cinematic lighting"
        return "$base, era ${ctx.era}, location ${ctx.location}, hero ${ctx.heroClass}. Scene: ${sceneText.take(120)}. No text, no watermark, no UI."
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
        val choices: List<Choice> = emptyList()
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
    private data class SceneJson(
        @SerialName("sceneDescr") val sceneDescr: String,
        @SerialName("imgPrmt") val imgPrmt: String,
        @SerialName("varLeft") val varLeft: String,
        @SerialName("varRight") val varRight: String,
        @SerialName("musicType") val musicType: String,
        @SerialName("dayWeather") val dayWeather: String,
        @SerialName("terrain") val terrain: String,
        val turn: Int
    )

    @Serializable
    private data class OAuthResponse(
        @SerialName("access_token") val accessToken: String? = null,
        @SerialName("expires_at") val expiresAtMs: Long? = null,
        @SerialName("expires_in") val expiresInSec: Long? = null
    )
}
