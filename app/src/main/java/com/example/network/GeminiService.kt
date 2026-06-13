package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
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
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

object GeminiService {
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    // Configured with generous 60-second timeouts as demanded by gemini-api guidelines
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun callGemini(
        prompt: String,
        bitmap: Bitmap? = null,
        systemInstruction: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API Key is missing. Please add your GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        val url = "$BASE_URL?key=$apiKey"

        val parts = mutableListOf<Part>()
        parts.add(Part(text = prompt))
        if (bitmap != null) {
            parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64())))
        }

        val requestPayload = GeminiRequest(
            contents = listOf(Content(parts = parts)),
            systemInstruction = systemInstruction?.let {
                Content(parts = listOf(Part(text = it)))
            }
        )

        val jsonBody = requestAdapter.toJson(requestPayload)
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext "API Call Failed: Code ${response.code}\n${responseString}"
                }
                val geminiResponse = responseAdapter.fromJson(responseString)
                val responseText = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                responseText ?: "Error: Failed to retrieve text from Gemini response."
            }
        } catch (e: Exception) {
            "Network Error: ${e.localizedMessage ?: "Unknown connection error"}"
        }
    }

    suspend fun performOcr(bitmap: Bitmap): String {
        return callGemini(
            prompt = "Perform accurate optical character extraction (OCR) on this scanned document. Pull all readable text word-for-word. Keep the layout organization as similar to the original image as possible.",
            bitmap = bitmap,
            systemInstruction = "You are an expert OCR and document analysis engine. Your sole objective is to output only clean extracted text and formatting, without conversational preambles."
        )
    }

    suspend fun summarizeText(text: String): String {
        return callGemini(
            prompt = "Please summarize this extracted document text in a neat, well-structured, bullet-pointed review: \n\n$text",
            systemInstruction = "You are a professional business intelligence assistant. Prepare a concise executive summary matching key metrics and take-aways."
        )
    }

    suspend fun translateText(text: String, targetLanguage: String): String {
        return callGemini(
            prompt = "Translate the following document text into target language '$targetLanguage'. Maintain absolute original meaning and structure: \n\n$text",
            systemInstruction = "You are a professional document translator. Translate text accurately, maintaining structure and tone."
        )
    }

    suspend fun parseInvoice(bitmap: Bitmap): String {
        return callGemini(
            prompt = "Extract and parse structured facts from this invoice. Return an elegant, cleanly organized Markdown list showing: Vendor Name, Invoice Number, Billing Date, Due Date, Detailed Line Items, Subtotal, Taxes, and Grand Total. If certain info is not present, mark it as N/A.",
            bitmap = bitmap,
            systemInstruction = "You are an AI financial auditor. Extract invoice parameters cleanly into readable markdown."
        )
    }
}
