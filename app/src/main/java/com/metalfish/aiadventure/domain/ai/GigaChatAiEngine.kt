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

    override suspend fun nextTurn(
        currentSceneText: String,
        playerChoice: String,
        context: AiContext
    ): AiTurnResult {
        Log.d(TAG, "GigaChatAiEngine.nextTurn CALLED phase=${context.phase} step=${context.step}")

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

        val isPrologueSwipe = context.phase.equals("PROLOGUE", ignoreCase = true) && context.step < 1
        val choices = if (isPrologueSwipe) {
            listOf("Дальше", "Дальше")
        } else {
            listOf(scene.varLeft, scene.varRight).map { it.trim() }.filter { it.isNotBlank() }
                .take(2)
                .ifEmpty { listOf("Продолжить путь", "Остановиться и осмотреться") }
        }

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
Ты ведущий мрачной исторической RPG без фэнтези. Ответ строго JSON, без лишнего текста.
Пролог (turn=1): 1 короткая сцена, назвать героя и цель, без выбора (var_left=var_right="Дальше").
Далее: 2 выбора, риск смерти 10-15%, реализм, потребности (еда/вода/сон), день/ночь. Бои до 3 сцен.
Имя героя неизменно. Эпоха/атмосфера из контекста. img_prmt: образ героя, EN, без текста/логотипов/UI.
scene_descr<=320.
{"scene_descr":"...","img_prmt":"...","var_left":"...","var_right":"...","music_type":"спокойный|напряжённый","day_weather":"...","terrain":"...","turn":1}
""".trimIndent()

    private fun userPrompt(cur: String, choice: String, ctx: AiContext): String = """
CTX: phase=${ctx.phase}, step=${ctx.step}, setting=${ctx.setting}, era=${ctx.era}, location=${ctx.location}, tone=${ctx.tone}
HERO: class=${ctx.heroClass}
CURRENT: ${cur.take(220).ifBlank { "(none)" }}; CHOICE: $choice
TASK: Следующий ход по системе, связность, 2 выбора, scene_descr<=320.
""".trimIndent()

    private fun parseSceneJsonOrNull(content: String): SceneJson? {
        val jsonBlock = extractJsonObject(content) ?: return null
        return runCatching { json.decodeFromString(SceneJson.serializer(), jsonBlock) }.getOrNull()
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
        "Ответь строго JSON по формату из системы. Исправь и верни только JSON. Твой прошлый ответ:\n$raw"

    private suspend fun requestRaw(accessToken: String, userContent: String): String? {
        val req = ChatRequest(
            model = "GigaChat-2",
            messages = listOf(
                Msg(role = "system", content = systemPrompt()),
                Msg(role = "user", content = userContent)
            ),
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
        @SerialName("scene_descr") val sceneDescr: String,
        @SerialName("img_prmt") val imgPrmt: String,
        @SerialName("var_left") val varLeft: String,
        @SerialName("var_right") val varRight: String,
        @SerialName("music_type") val musicType: String,
        @SerialName("day_weather") val dayWeather: String,
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
