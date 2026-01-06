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
            model = "GigaChat-2-Max",
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
                return fallbackTurn("Сеть отвечает странной тишиной. Реальность дрожит и не открывается.", context)
            }

        val raw = resp.choices.firstOrNull()?.message?.content.orEmpty().trim()
        Log.d(TAG, "GigaChat content raw=${raw.take(400)}")

        val scene = parseSceneJson(raw, context)

        val isPrologueSwipe = context.phase.equals("PROLOGUE", ignoreCase = true) && context.step < 2
        val choices = if (isPrologueSwipe) {
            listOf("Дальше", "Дальше")
        } else {
            listOf(scene.varLeft, scene.varRight).map { it.trim() }.filter { it.isNotBlank() }
                .take(2)
                .ifEmpty { listOf("Продолжить путь", "Остановиться и осмотреться") }
        }

        return AiTurnResult(
            sceneText = scene.sceneDescr.trim().take(360),
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
            sceneText = text.take(360),
            choices = listOf("Продолжить путь", "Остановиться и осмотреться"),
            outcomeText = "",
            statChanges = emptyList(),
            imagePrompt = buildFallbackImagePrompt(ctx, text),
            mode = TurnMode.STORY,
            combatOutcome = null
        )
    }

    private fun systemPrompt(): String = """
Ты - ведущий мрачной RPG в историческом сеттинге (без фэнтези).
История небанальная и развивается постепенно. Старт - необычное событие.
Первая сцена - пролог: подробно опиши жизнь героя, мир вокруг, его имя, а также ясно озвучь цель героя на всю игру.
Герой не может побеждать всех подряд или решать сложные задачи на старте. Рост и успех - постепенно.
Каждый ход заканчивается двумя выборами игрока.
Игра рассчитана минимум на 500 ходов.
Вероятность гибели при неверном выборе - 10-15%.
Должны быть сцены боя. Бой длится не более трех сцен. Герой выбирает оружие. Бой реалистичный.
Нужны смена дня и ночи, еда/вода/сон. Нехватка может привести к гибели.
Эпоху и атмосферу бери из контекста, не используй фэнтези-мотивы.
Имя героя придумываешь случайно в прологе и сохраняешь консистентно дальше.
В img_prmt обязательно передавай образ героя (внешность, одежда, детали), чтобы его можно было визуализировать.
Добавляй больше логики в действиях: причины/следствия, небольшие проверки, цена ошибок.
Первые два хода (turn=1 и turn=2) — пролог без выбора: var_left и var_right должны быть одинаковыми, например "Дальше".
ВАЖНО!!! Генерировать изображение тебе НЕ нужно
Отвечай строго JSON без лишнего текста. Поля и формат (scene_descr до 360 символов):
{
  "scene_descr": "подробное описание сцены",
  "img_prmt": "подробное описание для генерации изображения (EN), без текста/логотипов/UI",
  "var_left": "вариант выбора 1",
  "var_right": "вариант выбора 2",
  "music_type": "спокойный|напряжённый",
  "day_weather": "время суток и погода",
  "terrain": "название местности",
  "turn": 1
}
""".trimIndent()

    private fun userPrompt(cur: String, choice: String, ctx: AiContext): String = """
CONTEXT:
phase=${ctx.phase}, step=${ctx.step}
setting=${ctx.setting}
era=${ctx.era}
location=${ctx.location}
tone=${ctx.tone}

HERO:
class=${ctx.heroClass}
stats STR=${ctx.str}, AGI=${ctx.agi}, INT=${ctx.int}, CHA=${ctx.cha}
CURRENT_SCENE:
${cur.ifBlank { "(none)" }}

PLAYER_CHOICE:
$choice

TASK:
Сгенерируй следующий ход в формате JSON, как в системе. Поддерживай связность сюжета.
Если это пролог - начинай с необычного события, подробно введи героя и мир, и ясно озвучь его цель.
Эпоха: ${ctx.era}. Учитывай эпоху в деталях мира, быта, языка и технологий.
Добавляй реалистичные ограничения, потребности (еда/вода/сон) и смену времени суток/погоды.
Всегда завершай ход двумя вариантами выбора. scene_descr максимум 360 символов.
""".trimIndent()

    private fun parseSceneJson(content: String, ctx: AiContext): SceneJson {
        val jsonBlock = extractJsonObject(content) ?: content
        return runCatching { json.decodeFromString(SceneJson.serializer(), jsonBlock) }
            .getOrElse {
                Log.e(TAG, "SceneJson parse failed. Raw=${content.take(1200)}", it)
                SceneJson(
                    sceneDescr = (if (content.isBlank()) "Мир отвечает странной тишиной. Реальность дрожит, как тонкое стекло." else content).take(600),
                    imgPrmt = buildFallbackImagePrompt(ctx, content),
                    varLeft = "Продолжить путь",
                    varRight = "Остановиться и осмотреться",
                    musicType = "напряжённый",
                    dayWeather = "ночь, моросящий дождь",
                    terrain = "дорога у развалин",
                    turn = ctx.step
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
