package com.example.ai_keyboard

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast

/**
 * Handles SpeechRecognizer lifecycle so the service stays lean.
 */
class VoiceInputManager(private val service: AIKeyboardService) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechRecognizerIntent: Intent? = null
    private var isListening = false

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            service.showVoiceInputFeedback(true)
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            service.showVoiceInputFeedback(false)
        }

        override fun onError(error: Int) {
            isListening = false
            service.showVoiceInputFeedback(false)
            Toast.makeText(service, service.getString(R.string.voice_input_error), Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Voice input error: $error")
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            service.showVoiceInputFeedback(false)
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val transcript = matches?.firstOrNull()?.trim().orEmpty()
            if (transcript.isNotEmpty()) {
                insertRecognizedText(transcript)
            }
            Log.d(TAG, "Voice recognized: $transcript")
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening(language: String = "en") {
        if (isListening) {
            Log.d(TAG, "Voice input already active; ignoring duplicate start.")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(service)) {
            Toast.makeText(service, service.getString(R.string.voice_input_not_available), Toast.LENGTH_SHORT).show()
            Log.w(TAG, "Speech recognition not available on this device.")
            return
        }

        val recognizer = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(service).also {
            it.setRecognitionListener(recognitionListener)
            speechRecognizer = it
        }

        val intent = (speechRecognizerIntent ?: buildIntent(language)).also {
            it.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        }
        speechRecognizerIntent = intent

        try {
            recognizer.cancel()
            isListening = true
            service.showVoiceInputFeedback(true)
            recognizer.startListening(intent)
            Toast.makeText(service, service.getString(R.string.voice_input_listening), Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Voice input started for language: $language")
        } catch (e: Exception) {
            isListening = false
            service.showVoiceInputFeedback(false)
            Log.e(TAG, "Error starting voice input", e)
            Toast.makeText(service, service.getString(R.string.voice_input_error), Toast.LENGTH_SHORT).show()
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling voice input", e)
        }
        isListening = false
        service.showVoiceInputFeedback(false)
    }

    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        speechRecognizerIntent = null
    }

    private fun buildIntent(language: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }

    private fun insertRecognizedText(text: String) {
        val ic = service.currentInputConnection ?: return
        ic.commitText("$text ", 1)
    }

    companion object {
        private const val TAG = "VoiceInputManager"
    }
}
