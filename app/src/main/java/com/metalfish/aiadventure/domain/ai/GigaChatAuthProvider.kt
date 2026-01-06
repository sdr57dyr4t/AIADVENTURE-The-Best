package com.metalfish.aiadventure.domain.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.client.request.forms.FormDataContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.max

class GigaChatAuthProvider(
    private val authKeyBase64: String,
    private val scope: String = "GIGACHAT_API_PERS"
) {
    private val jsonCodec = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(jsonCodec) }
    }

    @Volatile private var cachedToken: String? = null
    @Volatile private var cachedExpiryMillis: Long = 0L

    suspend fun getToken(): String {
        val now = System.currentTimeMillis()
        val token = cachedToken
        if (!token.isNullOrBlank() && now < cachedExpiryMillis) return token

        val rqUid = UUID.randomUUID().toString()

        val resp: TokenResponse = client.post("https://ngw.devices.sberbank.ru:9443/api/v2/oauth") {
            header(HttpHeaders.Accept, "application/json")
            header("RqUID", rqUid)
            header(HttpHeaders.Authorization, "Basic $authKeyBase64")
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("scope", scope)
                    }
                )
            )
        }.body()

        val access = resp.accessToken
        // docs show expires_at as unix seconds; we convert to millis and subtract a small reserve
        val expiresAtMillis = max(0L, resp.expiresAt) * 1000L
        val reserveMillis = 30_000L // обновим за 30 сек до конца
        cachedToken = access
        cachedExpiryMillis = expiresAtMillis - reserveMillis

        return access
    }

    @Serializable
    private data class TokenResponse(
        val access_token: String,
        val expires_at: Long
    ) {
        val accessToken: String get() = access_token
        val expiresAt: Long get() = expires_at
    }
}
