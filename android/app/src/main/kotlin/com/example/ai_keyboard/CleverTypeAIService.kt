package com.example.ai_keyboard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * CleverType-style AI Service for Grammar Correction and Tone Adjustment
 * Implements the same patterns as CleverType app
 */
class CleverTypeAIService(private val context: Context) {
    
    companion object {
        private const val TAG = "CleverTypeAIService"
        private const val CONNECT_TIMEOUT = 10000
        private const val REQUEST_TIMEOUT = 30000
    }
    
    private val config = OpenAIConfig.getInstance(context)
    private val cache = AIResponseCache(context)
    
    /**
     * Grammar correction result
     */
    data class GrammarResult(
        val originalText: String,
        val correctedText: String,
        val hasChanges: Boolean,
        val corrections: List<Correction>,
        val processingTimeMs: Long,
        val fromCache: Boolean = false
    )
    
    /**
     * Individual correction made
     */
    data class Correction(
        val original: String,
        val corrected: String,
        val type: CorrectionType,
        val position: Int
    )
    
    enum class CorrectionType {
        GRAMMAR, SPELLING, PUNCTUATION, CAPITALIZATION
    }
    
    /**
     * Tone adjustment result
     */
    data class ToneResult(
        val originalText: String,
        val adjustedText: String,
        val tone: ToneType,
        val processingTimeMs: Long,
        val fromCache: Boolean = false
    )
    
    /**
     * Available tone types (CleverType-style)
     */
    enum class ToneType(val displayName: String, val emoji: String, val description: String) {
        PROFESSIONAL("Professional", "🎩", "Formal and business-appropriate"),
        CASUAL("Casual", "😊", "Relaxed and friendly"),
        FUNNY("Funny", "😂", "Humorous and entertaining"),
        ANGRY("Angry", "😤", "Firm and assertive"),
        EXCITED("Excited", "🎉", "Enthusiastic and energetic"),
        POLITE("Polite", "🙏", "Courteous and respectful"),
        CONFIDENT("Confident", "💪", "Strong and self-assured"),
        CARING("Caring", "❤️", "Warm and empathetic")
    }
    
    /**
     * Fix grammar and spelling errors in text
     * CleverType-style grammar correction
     */
    suspend fun fixGrammar(text: String): GrammarResult {
        val startTime = System.currentTimeMillis()
        
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cacheKey = "grammar_${text.hashCode()}"
                cache.get(cacheKey)?.let { cachedResponse ->
                    Log.d(TAG, "Grammar correction from cache")
                    return@withContext parseGrammarResponse(cachedResponse, text, startTime, true)
                }
                
                // Build grammar correction prompt
                val systemPrompt = """
                    You are a grammar and spelling correction assistant. Your task is to:
                    1. Fix grammar errors
                    2. Correct spelling mistakes
                    3. Fix punctuation issues
                    4. Correct capitalization
                    5. Preserve the original meaning and tone
                    6. Keep the text length similar
                    
                    Return ONLY the corrected text, nothing else.
                    If the text is already correct, return it unchanged.
                """.trimIndent()
                
                val userPrompt = "Correct grammar and spelling in this text:\n\n$text"
                
                // Make API request
                val response = makeOpenAIRequest(systemPrompt, userPrompt)
                
                // Cache the response
                cache.put(cacheKey, response)
                
                val processingTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "Grammar correction completed in ${processingTime}ms")
                
                parseGrammarResponse(response, text, startTime, false)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in grammar correction", e)
                GrammarResult(
                    originalText = text,
                    correctedText = text,
                    hasChanges = false,
                    corrections = emptyList(),
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    fromCache = false
                )
            }
        }
    }
    
    /**
     * Adjust text tone (CleverType-style)
     */
    suspend fun adjustTone(text: String, tone: ToneType): ToneResult {
        val startTime = System.currentTimeMillis()
        
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cacheKey = "tone_${tone.name}_${text.hashCode()}"
                cache.get(cacheKey)?.let { cachedResponse ->
                    Log.d(TAG, "Tone adjustment from cache")
                    return@withContext ToneResult(
                        originalText = text,
                        adjustedText = cachedResponse,
                        tone = tone,
                        processingTimeMs = System.currentTimeMillis() - startTime,
                        fromCache = true
                    )
                }
                
                // Build tone adjustment prompt
                val systemPrompt = """
                    You are a text tone adjustment assistant. Your task is to rewrite text in a specific tone while:
                    1. Preserving the core meaning
                    2. Maintaining appropriate length
                    3. Using natural language
                    4. Matching the requested tone perfectly
                    
                    Return ONLY the rewritten text, nothing else.
                """.trimIndent()
                
                val userPrompt = "Rewrite this text in a ${tone.displayName.lowercase()} tone (${tone.description}):\n\n$text"
                
                // Make API request
                val response = makeOpenAIRequest(systemPrompt, userPrompt)
                
                // Cache the response
                cache.put(cacheKey, response)
                
                val processingTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "Tone adjustment completed in ${processingTime}ms")
                
                ToneResult(
                    originalText = text,
                    adjustedText = response.trim(),
                    tone = tone,
                    processingTimeMs = processingTime,
                    fromCache = false
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in tone adjustment", e)
                ToneResult(
                    originalText = text,
                    adjustedText = text,
                    tone = tone,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    fromCache = false
                )
            }
        }
    }
    
    /**
     * Parse grammar correction response
     */
    private fun parseGrammarResponse(
        response: String, 
        originalText: String, 
        startTime: Long, 
        fromCache: Boolean
    ): GrammarResult {
        val correctedText = response.trim()
        val hasChanges = correctedText != originalText
        
        // Simple correction detection (could be enhanced with more sophisticated analysis)
        val corrections = if (hasChanges) {
            listOf(
                Correction(
                    original = originalText,
                    corrected = correctedText,
                    type = CorrectionType.GRAMMAR,
                    position = 0
                )
            )
        } else {
            emptyList()
        }
        
        return GrammarResult(
            originalText = originalText,
            correctedText = correctedText,
            hasChanges = hasChanges,
            corrections = corrections,
            processingTimeMs = System.currentTimeMillis() - startTime,
            fromCache = fromCache
        )
    }
    
    /**
     * Make OpenAI API request
     */
    private suspend fun makeOpenAIRequest(systemPrompt: String, userText: String): String {
        return withContext(Dispatchers.IO) {
            val authHeader = config.getAuthorizationHeader()
                ?: throw Exception("API key not configured")

            val url = URL(OpenAIConfig.CHAT_COMPLETIONS_ENDPOINT)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", authHeader)
                    setRequestProperty("User-Agent", "CleverType-AI-Keyboard/1.0")
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = REQUEST_TIMEOUT
                    doOutput = true
                }

                val requestBody = createChatCompletionRequest(systemPrompt, userText)

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                when (responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                            reader.readText()
                        }
                        return@withContext parseOpenAIResponse(response)
                    }
                    HttpURLConnection.HTTP_UNAUTHORIZED -> {
                        throw Exception("Invalid API key. Please check your OpenAI API key.")
                    }
                    429 -> {
                        throw Exception("Rate limit exceeded. Please wait before making another request.")
                    }
                    else -> {
                        val errorResponse = try {
                            BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                                reader.readText()
                            }
                        } catch (e: Exception) {
                            "No error details available."
                        }
                        throw Exception("API request failed with code $responseCode: $errorResponse")
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }
    
    /**
     * Create chat completion request JSON
     */
    private fun createChatCompletionRequest(systemPrompt: String, userText: String): String {
        val requestJson = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userText)
                })
            })
            put("max_tokens", 500)
            put("temperature", 0.3)
        }
        
        return requestJson.toString()
    }
    
    /**
     * Parse OpenAI response
     */
    private fun parseOpenAIResponse(response: String): String {
        try {
            val jsonResponse = JSONObject(response)
            val choices = jsonResponse.getJSONArray("choices")
            
            if (choices.length() > 0) {
                val firstChoice = choices.getJSONObject(0)
                val message = firstChoice.getJSONObject("message")
                return message.getString("content").trim()
            }
            
            throw Exception("No response content found")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing OpenAI response", e)
            throw Exception("Failed to parse AI response: ${e.message}")
        }
    }
    
    /**
     * Get available tone types
     */
    fun getAvailableTones(): List<ToneType> {
        return ToneType.values().toList()
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        // Cleanup if needed
        Log.d(TAG, "CleverTypeAIService cleaned up")
    }
}
