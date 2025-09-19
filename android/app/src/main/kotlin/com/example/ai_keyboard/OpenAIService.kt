package com.example.ai_keyboard

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
 * OpenAI API service for AI-powered keyboard features
 */
class OpenAIService(private val context: Context) {
    
    companion object {
        private const val TAG = "OpenAIService"
        private const val REQUEST_TIMEOUT = 30000 // 30 seconds
        private const val CONNECT_TIMEOUT = 10000 // 10 seconds
    }
    
    private val config = OpenAIConfig.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * AI Writing Features
     */
    enum class AIFeature(val displayName: String, val systemPrompt: String) {
        GRAMMAR_CHECK(
            "Grammar Check",
            "You are a grammar correction assistant. Fix grammar, spelling, and punctuation errors in the given text. Return only the corrected text without explanations."
        ),
        TONE_FORMAL(
            "Make Formal",
            "Rewrite the following text in a formal, professional tone suitable for business communication. Maintain the original meaning."
        ),
        TONE_CASUAL(
            "Make Casual",
            "Rewrite the following text in a casual, friendly tone suitable for informal conversation. Maintain the original meaning."
        ),
        TONE_FUNNY(
            "Make Funny",
            "Rewrite the following text to be more humorous and entertaining while maintaining the core message."
        ),
        PARAPHRASE(
            "Paraphrase",
            "Rewrite the following text using different words and sentence structure while maintaining the same meaning."
        ),
        SUMMARIZE(
            "Summarize",
            "Provide a concise summary of the following text, capturing the main points in fewer words."
        ),
        EXPAND(
            "Expand",
            "Expand the following text with more details, examples, and elaboration while maintaining the original tone."
        ),
        TRANSLATE_SIMPLE(
            "Simple English",
            "Rewrite the following text using simpler words and shorter sentences, making it easier to understand."
        )
    }
    
    /**
     * Smart Reply Categories
     */
    enum class SmartReplyContext(val displayName: String, val systemPrompt: String) {
        GENERAL(
            "General",
            "Generate 3 brief, appropriate responses to the following message. Each response should be different in tone (friendly, neutral, enthusiastic)."
        ),
        BUSINESS(
            "Business",
            "Generate 3 professional business responses to the following message. Keep them concise and appropriate for workplace communication."
        ),
        SOCIAL(
            "Social",
            "Generate 3 casual, friendly responses suitable for social media or informal messaging."
        ),
        QUESTION(
            "Question",
            "Generate 3 helpful responses to answer or acknowledge the following question."
        )
    }
    
    /**
     * Process text with AI feature
     */
    suspend fun processText(text: String, feature: AIFeature): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isNetworkAvailable()) {
                    return@withContext Result.failure(Exception("No internet connection"))
                }
                
                if (!config.isAIFeaturesEnabled()) {
                    return@withContext Result.failure(Exception("AI features are disabled"))
                }
                
                val response = makeOpenAIRequest(feature.systemPrompt, text)
                Result.success(response)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing text with AI", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Generate smart replies
     */
    suspend fun generateSmartReplies(message: String, context: SmartReplyContext = SmartReplyContext.GENERAL): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isNetworkAvailable()) {
                    return@withContext Result.failure(Exception("No internet connection"))
                }
                
                if (!config.isAIFeaturesEnabled()) {
                    return@withContext Result.failure(Exception("AI features are disabled"))
                }
                
                val response = makeOpenAIRequest(context.systemPrompt, message)
                val replies = parseSmartReplies(response)
                Result.success(replies)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating smart replies", e)
                Result.failure(e)
            }
        }
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
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        reader.readText()
                    }
                    
                    return@withContext parseOpenAIResponse(response)
                } else {
                    val errorResponse = BufferedReader(InputStreamReader(connection.errorStream)).use { reader ->
                        reader.readText()
                    }
                    throw Exception("OpenAI API error: $responseCode - $errorResponse")
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
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userText)
            })
        }
        
        return JSONObject().apply {
            put("model", OpenAIConfig.DEFAULT_MODEL)
            put("messages", messages)
            put("max_tokens", OpenAIConfig.MAX_TOKENS)
            put("temperature", OpenAIConfig.TEMPERATURE)
            put("top_p", 1.0)
            put("frequency_penalty", 0.0)
            put("presence_penalty", 0.0)
        }.toString()
    }
    
    /**
     * Parse OpenAI API response
     */
    private fun parseOpenAIResponse(response: String): String {
        val jsonResponse = JSONObject(response)
        val choices = jsonResponse.getJSONArray("choices")
        
        if (choices.length() > 0) {
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            return message.getString("content").trim()
        }
        
        throw Exception("No response content from OpenAI")
    }
    
    /**
     * Parse smart replies from AI response
     */
    private fun parseSmartReplies(response: String): List<String> {
        // Split response into individual replies
        val replies = response.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("Response") && !it.matches(Regex("^\\d+\\..*")) }
            .map { it.removePrefix("-").removePrefix("•").trim() }
            .filter { it.isNotEmpty() }
        
        return if (replies.size >= 3) {
            replies.take(3)
        } else {
            // Fallback replies if parsing fails
            listOf("Thanks!", "Got it", "Sounds good")
        }
    }
    
    /**
     * Check network availability
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
    
    /**
     * Test API connectivity
     */
    suspend fun testConnection(): Result<String> {
        return try {
            val result = processText("Hello", AIFeature.GRAMMAR_CHECK)
            if (result.isSuccess) {
                Result.success("OpenAI API connection successful")
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Connection test failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get available AI features
     */
    fun getAvailableFeatures(): List<AIFeature> = AIFeature.values().toList()
    
    /**
     * Get available smart reply contexts
     */
    fun getAvailableContexts(): List<SmartReplyContext> = SmartReplyContext.values().toList()
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
    }
}
