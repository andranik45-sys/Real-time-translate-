package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiTranslationService {
    suspend fun translateText(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
        stylePrompt: String = "",
        contextPrompt: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API key is not set. Please set it via the Secrets panel in AI Studio."))
        }

        val styleSuffix = if (stylePrompt.isNotBlank()) "\nStyle/Tone requirement: $stylePrompt" else ""
        val contextSuffix = if (contextPrompt.isNotBlank()) "\nContextual Scenario constraint: Use context matching '$contextPrompt' to resolve pronouns, polysemy, or ambiguous expressions appropriately." else ""

        val prompt = "Translate the following statement accurately from $sourceLanguage to $targetLanguage. Provide only the direct translated text, nothing else, no commentary, no quotes, no explanations. Just direct translated sentence.\n\nStatement: $text$styleSuffix$contextSuffix"

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            generationConfig = GenerationConfig(
                temperature = 0.3f
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are an expert real-time translation agent. Translate the phrase exactly, honors all specific tone, style, and scenario guidelines parameters. Do not include any introductory comments or metadata — output ONLY the clean translation."))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val translated = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()

            if (translated != null) {
                Result.success(translated)
            } else {
                Result.failure(Exception("Failed to extract translation from Gemini response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
