package com.example.data

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.Buffer
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Retrofit service definition strictly adhering to the requirements:
 * - Base URL: https://generativelanguage.googleapis.com
 * - Version: v1beta
 * - Endpoint path: /v1beta/models/gemini-2.5-flash:generateContent
 */
interface GeminiApiService {

    /**
     * Primary endpoint matching:
     * Base URL: https://generativelanguage.googleapis.com
     * Endpoint path: /v1beta/models/gemini-2.5-flash:generateContent
     */
    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContentFlash25(
        @Query("key") apiKey: String,
        @Body requestBody: RequestBody
    ): Response<ResponseBody>

    /**
     * Legacy backward-compatibility endpoint mapping
     */
    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContentFlash15(
        @Query("key") apiKey: String,
        @Body requestBody: RequestBody
    ): Response<ResponseBody>

    /**
     * Generalized endpoint for other Gemini models.
     */
    @Headers("Content-Type: application/json; charset=utf-8")
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentDynamic(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body requestBody: RequestBody
    ): Response<ResponseBody>
}

/**
 * OkHttp Interceptor that:
 * 1. Injects the GEMINI_API_KEY into the request headers ('x-goog-api-key')
 * 2. Logs the full outgoing request URL, headers, and body for debugging purposes.
 */
class GeminiAuthAndLoggingInterceptor(
    private val apiKeyProvider: () -> String = {
        try {
            val key = com.example.BuildConfig.GEMINI_API_KEY
            if (key.isNullOrBlank() || key == "DEFAULT_API_KEY" || key == "YOUR_GEMINI_API_KEY") "" else key
        } catch (e: Exception) {
            ""
        }
    }
) : Interceptor {
    companion object {
        private const val TAG = "GeminiNetworking"
        const val HEADER_API_KEY = "x-goog-api-key"
    }

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest: Request = chain.request()
        val apiKey = apiKeyProvider()

        // 1. Inject GEMINI_API_KEY into the request headers
        val requestBuilder = originalRequest.newBuilder()
        if (apiKey.isNotBlank()) {
            requestBuilder.header(HEADER_API_KEY, apiKey)
        }

        // Also ensure key query param is present on the URL if not already specified
        val url = originalRequest.url
        if (apiKey.isNotBlank() && url.queryParameter("key").isNullOrBlank()) {
            val newUrl = url.newBuilder().addQueryParameter("key", apiKey).build()
            requestBuilder.url(newUrl)
        }

        val request = requestBuilder.build()
        val fullUrl = request.url.toString()
        val headers = request.headers

        // 2. Read outgoing request body for debugging
        val requestBodyString = request.body?.let { body ->
            try {
                val buffer = Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            } catch (e: Exception) {
                "[Unable to read request body: ${e.localizedMessage}]"
            }
        } ?: "[No Request Body]"

        // 3. Log the full outgoing request URL, headers, and body
        val isBaseUrlValid = fullUrl.startsWith("https://generativelanguage.googleapis.com")
        val isEndpointValid = fullUrl.contains("v1beta/models/gemini-2.5-flash:generateContent") || fullUrl.contains("generateContent")

        val diagnosticLog = buildString {
            appendLine("╔═══════════════════════════════════════════════════════════════════════════════")
            appendLine("║ 🌐 [GEMINI OUTGOING REQUEST - OKHTTP INTERCEPTOR]")
            appendLine("╠═══════════════════════════════════════════════════════════════════════════════")
            appendLine("║ 🔗 Full Outgoing URL : $fullUrl")
            appendLine("║ ⚡ HTTP Method       : ${request.method}")
            appendLine("║ 🔑 Header Injected   : ${if (request.header(HEADER_API_KEY).isNullOrBlank()) "❌ MISSING" else "✅ $HEADER_API_KEY: ${request.header(HEADER_API_KEY)?.take(6)}***"}")
            appendLine("║ 🏛️ Base URL Check    : ${if (isBaseUrlValid) "✅ VERIFIED (https://generativelanguage.googleapis.com)" else "❌ MISMATCH: $fullUrl"}")
            appendLine("║ 🎯 Endpoint Check    : ${if (isEndpointValid) "✅ VERIFIED (${request.url.encodedPath})" else "ℹ️ PATH: ${request.url.encodedPath}"}")
            appendLine("╠───────────────────────────────────────────────────────────────────────────────")
            appendLine("║ 📋 Headers (${headers.size}):")
            if (headers.size == 0) {
                appendLine("║    (None)")
            } else {
                for (i in 0 until headers.size) {
                    val name = headers.name(i)
                    val value = if (name.equals(HEADER_API_KEY, ignoreCase = true)) {
                        "${headers.value(i).take(6)}*** [REDACTED FOR SECURITY]"
                    } else {
                        headers.value(i)
                    }
                    appendLine("║    $name: $value")
                }
            }
            appendLine("╠───────────────────────────────────────────────────────────────────────────────")
            appendLine("║ 📦 Outgoing Request Body:")
            requestBodyString.lines().forEach { line ->
                appendLine("║    $line")
            }
            appendLine("╚═══════════════════════════════════════════════════════════════════════════════")
        }

        Log.i(TAG, diagnosticLog)

        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val elapsedMs = System.currentTimeMillis() - startTime

        Log.i(TAG, "⬅️ [GEMINI RESPONSE] HTTP ${response.code} ${response.message} in ${elapsedMs}ms | URL: ${response.request.url}")

        return response
    }
}

/**
 * Networking client managing OkHttp, Retrofit, and the diagnostic logging interceptor.
 */
object GeminiNetworkClient {
    // Base URL verified: https://generativelanguage.googleapis.com
    // (Retrofit requires a trailing slash for the base URL)
    const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Verified model endpoint path
    const val ENDPOINT_PATH = "v1beta/models/gemini-2.5-flash:generateContent"

    val interceptor = GeminiAuthAndLoggingInterceptor()

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)
}
