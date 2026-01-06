package com.metalfish.aiadventure.domain.image

import android.util.Log
import com.metalfish.aiadventure.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YandexART async image generation (Yandex AI Studio) WITHOUT kotlinx.serialization.
 *
 * POST https://llm.api.cloud.yandex.net/foundationModels/v1/imageGenerationAsync
 * GET  https://operation.api.cloud.yandex.net/operations/{operationId}
 */
@Singleton
class YandexArtImageEngine @Inject constructor() : ImageEngine {

    private val TAG = "AIAdventure"

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }
        expectSuccess = false
    }

    private val apiKey: String = BuildConfig.YANDEX_AI_API_KEY.trim()
    private val folderId: String = BuildConfig.YANDEX_FOLDER_ID.trim()

    override suspend fun generateImageBytes(
        prompt: String,
        width: Int,
        height: Int,
        seed: Long?
    ): ByteArray {
        require(apiKey.isNotBlank()) { "YANDEX_AI_API_KEY пустой. Добавь в local.properties" }
        require(folderId.isNotBlank()) { "YANDEX_FOLDER_ID пустой. Добавь в local.properties" }

        val (wr, hr) = aspectRatioFor(width, height)

        val requestJson = buildRequestJson(
            modelUri = "art://$folderId/yandex-art/latest",
            prompt = prompt,
            mimeType = "image/jpeg",
            seed = seed?.toString(),
            widthRatio = wr.toString(),
            heightRatio = hr.toString()
        )

        Log.d(TAG, "YandexART request: modelUri=art://$folderId/yandex-art/latest")

        val createResp = client.post("https://llm.api.cloud.yandex.net/foundationModels/v1/imageGenerationAsync") {
            header("Authorization", "Api-Key $apiKey")
            header("Accept", "application/json")
            contentType(ContentType.Application.Json)
            setBody(requestJson)
        }

        val op = parseOperationOrThrow(createResp, "imageGenerationAsync")

        if (op.done) {
            op.errorMessage?.let { msg ->
                throw IllegalStateException("YandexART error ${op.errorCode ?: ""}: $msg")
            }
            val img = op.imageBase64
            if (!img.isNullOrBlank()) return Base64.getDecoder().decode(img)
            throw IllegalStateException("YandexART: done=true but image is empty")
        }

        val deadline = System.currentTimeMillis() + 60_000
        var backoffMs = 900L

        while (System.currentTimeMillis() < deadline) {
            delay(backoffMs)
            backoffMs = (backoffMs * 1.2).toLong().coerceAtMost(3000L)

            val statusResp = client.get("https://operation.api.cloud.yandex.net/operations/${op.id}") {
                header("Authorization", "Api-Key $apiKey")
                header("Accept", "application/json")
            }

            val status = parseOperationOrThrow(statusResp, "operation.get")

            if (!status.done) continue

            status.errorMessage?.let { msg ->
                throw IllegalStateException("YandexART error ${status.errorCode ?: ""}: $msg")
            }

            val img = status.imageBase64
            if (!img.isNullOrBlank()) return Base64.getDecoder().decode(img)

            throw IllegalStateException("YandexART: done=true but image is empty")
        }

        throw IllegalStateException("YandexART timeout: generation still in progress (operation=${op.id})")
    }

    private fun aspectRatioFor(width: Int, height: Int): Pair<Int, Int> {
        return if (height >= width) 3 to 4 else 4 to 3
    }

    private suspend fun parseOperationOrThrow(resp: HttpResponse, stage: String): OperationLite {
        val body = resp.bodyAsText()

        if (resp.status.value !in 200..299) {
            // покажем серверную причину (важно для 400/401/403)
            throw IllegalStateException("YandexART $stage failed: HTTP ${resp.status.value}. Body: ${body.take(2000)}")
        }

        return runCatching { parseOperation(body) }.getOrElse {
            throw IllegalStateException("YandexART $stage: cannot parse JSON. Body: ${body.take(2000)}", it)
        }
    }

    private fun parseOperation(body: String): OperationLite {
        val obj = JSONObject(body)

        val id = obj.optString("id", "")
        val done = obj.optBoolean("done", false)

        val errObj = obj.optJSONObject("error")
        val errorCode = errObj?.optInt("code")
        val errorMessage = errObj?.optString("message")

        val respObj = obj.optJSONObject("response")
        val image = respObj?.optString("image")

        return OperationLite(
            id = id,
            done = done,
            imageBase64 = image,
            errorCode = errorCode,
            errorMessage = errorMessage
        )
    }

    private fun buildRequestJson(
        modelUri: String,
        prompt: String,
        mimeType: String,
        seed: String?,
        widthRatio: String,
        heightRatio: String
    ): String {
        val p = jsonEscape(prompt)

        val seedPart = if (!seed.isNullOrBlank()) ""","seed":"${jsonEscape(seed)}"""" else ""

        return """
            {
              "modelUri":"${jsonEscape(modelUri)}",
              "messages":[{"text":"$p"}],
              "generationOptions":{
                "mimeType":"${jsonEscape(mimeType)}"$seedPart,
                "aspectRatio":{"widthRatio":"${jsonEscape(widthRatio)}","heightRatio":"${jsonEscape(heightRatio)}"}
              }
            }
        """.trimIndent()
    }

    private fun jsonEscape(s: String): String {
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
    }

    private data class OperationLite(
        val id: String,
        val done: Boolean,
        val imageBase64: String?,
        val errorCode: Int?,
        val errorMessage: String?
    )
}
