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

        val req = ChatRequest(
            model = "GigaChat",
            messages = listOf(
                Msg(role = "system", content = systemPrompt()),
                Msg(role = "user", content = userPrompt(currentSceneText, playerChoice, context))
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
                return fallbackTurn("Сбой генерации. Ситуация накаляется — действуй!", context)
            }

        val raw = resp.choices.firstOrNull()?.message?.content.orEmpty().trim()
        Log.d(TAG, "GigaChat content raw=${raw.take(400)}")

        val scene = parseSceneJson(raw, context)

        return AiTurnResult(
            sceneText = scene.sceneText.trim(),
            choices = scene.choices.take(2).ifEmpty { listOf("Рвануть вперёд", "Отступить") },
            outcomeText = scene.outcomeText.trim(),
            statChanges = scene.statChanges.map { StatChange(it.key, it.delta) },
            imagePrompt = scene.imagePrompt.ifBlank { buildFallbackImagePrompt(context, scene.sceneText) },
            mode = parseTurnMode(scene.mode),
            combatOutcome = parseCombatOutcome(scene.combatOutcome)
        )
    }

    private fun parseTurnMode(mode: String?): TurnMode {
        return if (mode.equals("COMBAT", ignoreCase = true)) TurnMode.COMBAT else TurnMode.STORY
    }

    private fun parseCombatOutcome(outcome: String?): CombatOutcome? {
        return when (outcome?.trim()?.uppercase()) {
            "VICTORY" -> CombatOutcome.VICTORY
            "DEATH" -> CombatOutcome.DEATH
            "ESCAPE" -> CombatOutcome.ESCAPE
            else -> null
        }
    }

    private fun fallbackTurn(text: String, ctx: AiContext): AiTurnResult {
        return AiTurnResult(
            sceneText = text.take(260),
            choices = listOf("Рвануть вперёд", "Отступить"),
            outcomeText = "",
            statChanges = emptyList(),
            imagePrompt = buildFallbackImagePrompt(ctx, text),
            mode = TurnMode.STORY,
            combatOutcome = null
        )
    }

    private fun systemPrompt(): String = """
Ты — сценарист и гейм-мастер короткой, логичной и динамичной RPG (стиль: Reigns — лаконично, кинематографично).
ТЫ ОБЯЗАН отвечать СТРОГО JSON без пояснений и без текста вокруг. Пиши ТОЛЬКО НА РУССКОМ ЯЗЫКЕ
ВСЕГДА ровно 2 выбора.

Обязательные поля JSON:
{
  "scene_text": "2–4 предложения, максимум 260 символов",
  "choices": ["ровно 2 варианта (2–6 слов)", "ровно 2 варианта (2–6 слов)"],
  "outcome_text": "0–2 предложения, максимум 140 символов",
  "stat_changes": [{"key":"hp|stamina|gold|reputation","delta":-10..+10}] (может быть пустым),
  "image_prompt": "одна строка (EN). Кратко описывает КАРТИНКУ текущей сцены. Без текста/логотипов/UI.",
  "mode": "STORY или COMBAT"
}

Если mode="COMBAT", то добавь:
"combat_outcome": "VICTORY или DEATH или ESCAPE"

Правила боя:
- В бою возможны ТОЛЬКО 3 исхода: победа, смерть, бегство.
- DEATH редко и только если логично/опасно.
- ESCAPE возможен, но имеет цену (stamina/репутация/золото).
- VICTORY может давать добычу или репутацию.

Сюжет:
- Учитывай setting/era/location/tone и класс героя.
- Делай причинно-следственные связи. Не телепортируй героя без причины.
- Держи 1-2 постоянных крючка/антагониста/цель.
""".trimIndent()

    private fun userPrompt(cur: String, choice: String, ctx: AiContext): String = """
CONTEXT:
phase=${ctx.phase}, step=${ctx.step}
setting=${ctx.setting}
era=${ctx.era}
location=${ctx.location}
tone=${ctx.tone}

HERO:
name=${ctx.heroName}
class=${ctx.heroClass}
stats STR=${ctx.str}, AGI=${ctx.agi}, INT=${ctx.int}, CHA=${ctx.cha}

CURRENT_SCENE:
${cur.ifBlank { "(none)" }}

PLAYER_CHOICE:
$choice

TASK:
Сгенерируй следующую сцену. Логично продолжай сюжет. Если уместно — mode=COMBAT.
Верни JSON со всеми обязательными полями, включая image_prompt (EN).
""".trimIndent()

    private fun parseSceneJson(content: String, ctx: AiContext): SceneJson {
        val jsonBlock = extractJsonObject(content) ?: content
        return runCatching { json.decodeFromString(SceneJson.serializer(), jsonBlock) }
            .getOrElse {
                Log.e(TAG, "SceneJson parse failed. Raw=${content.take(1200)}", it)
                SceneJson(
                    sceneText = (if (content.isBlank()) "Всё вспыхивает — действуй!" else content).take(260),
                    choices = listOf("Рвануть вперёд", "Отступить"),
                    outcomeText = "",
                    statChanges = emptyList(),
                    imagePrompt = buildFallbackImagePrompt(ctx, content),
                    mode = "STORY",
                    combatOutcome = null
                )
            }
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

    private fun buildFallbackImagePrompt(ctx: AiContext, sceneText: String): String {
        val base = when (ctx.setting.uppercase()) {
            "CYBERPUNK" -> "cinematic cyberpunk scene, neon rain, gritty streets"
            "POSTAPOC" -> "cinematic post-apocalyptic scene, ruins, dust, dramatic light"
            else -> "cinematic fantasy scene, dramatic light, detailed environment"
        }
        return "$base, era ${ctx.era}, location ${ctx.location}, hero ${ctx.heroClass}. Scene: ${sceneText.take(120)}. No text, no watermark, no UI."
    }

    private suspend fun getAccessToken(keyOrToken: String): String {
        val now = System.currentTimeMillis()

        cachedToken?.let { token ->
            if (now + 30_000 < cachedTokenExpiresAtMs) return token
        }

        // If looks like JWT -> treat as ready bearer token
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
        @SerialName("scene_text") val sceneText: String,
        val choices: List<String> = emptyList(),
        @SerialName("outcome_text") val outcomeText: String = "",
        @SerialName("stat_changes") val statChanges: List<StatChangeJson> = emptyList(),
        @SerialName("image_prompt") val imagePrompt: String = "",
        val mode: String? = "STORY",
        @SerialName("combat_outcome") val combatOutcome: String? = null
    )

    @Serializable
    private data class StatChangeJson(
        val key: String,
        val delta: Int
    )

    @Serializable
    private data class OAuthResponse(
        @SerialName("access_token") val accessToken: String? = null,
        @SerialName("expires_at") val expiresAtMs: Long? = null,
        @SerialName("expires_in") val expiresInSec: Long? = null
    )
}
