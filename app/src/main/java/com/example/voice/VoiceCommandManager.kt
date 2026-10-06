package com.example.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.RobotState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    data class Processing(val text: String) : VoiceState()
    data class Recognized(val command: String, val payload: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

class VoiceCommandManager(
    private val context: Context,
    private val onCommandPayload: (payload: String, description: String) -> Unit
) {
    companion object {
        private const val TAG = "VoiceCommandManager"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb: StateFlow<Float> = _audioRmsDb.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    private var isListeningDesired = false

    init {
        mainHandler.post {
            ensureRecognizer()
        }
    }

    private fun ensureRecognizer() {
        if (speechRecognizer != null) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "SpeechRecognizer is not marked as available on this device/ROM.")
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(listener)
            }
            Log.i(TAG, "SpeechRecognizer created successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SpeechRecognizer: ${e.message}")
            speechRecognizer = null
        }
    }

    fun startListening() {
        isListeningDesired = true
        mainHandler.post {
            // Check microphone permission first
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                _voiceState.value = VoiceState.Error("Microphone permission required. Tap mic to request.")
                isListeningDesired = false
                return@post
            }

            ensureRecognizer()

            if (speechRecognizer == null) {
                _voiceState.value = VoiceState.Error(
                    "Speech engine unavailable on this device. Use registered phrase buttons or text command."
                )
                isListeningDesired = false
                return@post
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            try {
                // Cancel any previous session to clear RECOGNIZER_BUSY
                speechRecognizer?.cancel()
                _voiceState.value = VoiceState.Listening
                speechRecognizer?.startListening(intent)
                Log.i(TAG, "SpeechRecognizer started listening.")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting voice recognition: ${e.message}")
                _voiceState.value = VoiceState.Error(e.message ?: "Failed to start listening")
                isListeningDesired = false
            }
        }
    }

    fun stopListening() {
        isListeningDesired = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _voiceState.value = VoiceState.Idle
                _audioRmsDb.value = 0f
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping voice recognition: ${e.message}")
            }
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _voiceState.value = VoiceState.Listening
        }

        override fun onBeginningOfSpeech() {
            _voiceState.value = VoiceState.Listening
        }

        override fun onRmsChanged(rmsdB: Float) {
            _audioRmsDb.value = rmsdB
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _audioRmsDb.value = 0f
        }

        override fun onError(error: Int) {
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client error (speech engine paused)"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK -> "Network error connecting to speech service"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap mic and try again."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy. Resetting..."
                SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap mic to speak."
                else -> "Speech service error ($error)"
            }
            Log.w(TAG, "SpeechRecognizer onError: $errorMsg ($error)")
            _voiceState.value = VoiceState.Error(errorMsg)
            _audioRmsDb.value = 0f
            isListeningDesired = false
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val topMatch = matches?.firstOrNull() ?: ""
            Log.i(TAG, "Speech recognized: $topMatch (all matches: $matches)")
            _lastRecognizedText.value = topMatch
            processSpeechInput(topMatch)
            isListeningDesired = false
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let {
                _voiceState.value = VoiceState.Processing(it)
                // Opportunistic quick-matching for responsive real-time control
                val cleaned = it.lowercase().trim()
                if (cleaned.contains("stop") || cleaned == "halt" || cleaned == "freeze") {
                    processSpeechInput(cleaned)
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * Map voice phrases to robot payloads:
     * - "move forward" -> RV:1
     * - "reverse" -> RV:2
     * - "turn left" -> RV:3
     * - "turn right" -> RV:4
     * - "stop" -> RV:0,BS:0,BM:0
     * - "dig" -> AUTO:1
     * - "confirm" -> CONFIRM:1
     */
    fun processSpeechInput(input: String) {
        val sanitized = input.lowercase().replace(Regex("[^a-z0-9 ]"), " ").trim()
        _lastRecognizedText.value = input

        val (payload, description) = when {
            // STOP COMMAND (highest priority)
            sanitized.contains("stop") || sanitized.contains("halt") ||
                    sanitized.contains("freeze") || sanitized.contains("brake") ||
                    sanitized.contains("pause") || sanitized.contains("wait") -> {
                RobotState.PAYLOAD_SAFETY_STOP to "EMERGENCY STOP"
            }

            // MOVE FORWARD
            sanitized.contains("move forward") || sanitized == "forward" ||
                    sanitized.contains("go forward") || sanitized.contains("drive forward") ||
                    sanitized == "ahead" || sanitized == "advance" || sanitized == "front" -> {
                "RV:1" to "MOVE FORWARD"
            }

            // REVERSE
            sanitized.contains("reverse") || sanitized == "back" ||
                    sanitized.contains("go back") || sanitized.contains("backward") ||
                    sanitized.contains("backwards") || sanitized.contains("retreat") -> {
                "RV:2" to "REVERSE"
            }

            // TURN LEFT
            sanitized.contains("turn left") || sanitized == "left" ||
                    sanitized.contains("go left") || sanitized.contains("spin left") ||
                    sanitized.contains("rotate left") || sanitized.contains("pivot left") -> {
                "RV:3" to "TURN LEFT"
            }

            // TURN RIGHT
            sanitized.contains("turn right") || sanitized == "right" ||
                    sanitized.contains("go right") || sanitized.contains("spin right") ||
                    sanitized.contains("rotate right") || sanitized.contains("pivot right") -> {
                "RV:4" to "TURN RIGHT"
            }

            // DIG / AUTO RUN
            sanitized.contains("dig") || sanitized.contains("start auto") ||
                    sanitized.contains("auto run") || sanitized.contains("excavate") ||
                    sanitized == "auto" || sanitized.contains("digging") -> {
                RobotState.PAYLOAD_AUTO_RUN to "START AUTO RUN"
            }

            // CONFIRM POSITION
            sanitized.contains("confirm") || sanitized.contains("confirm position") ||
                    sanitized == "dump" || sanitized == "done" || sanitized == "ok" ||
                    sanitized == "okay" || sanitized == "finished" -> {
                RobotState.PAYLOAD_CONFIRM_POS to "CONFIRM POSITION"
            }

            else -> {
                null to null
            }
        }

        if (payload != null && description != null) {
            _voiceState.value = VoiceState.Recognized(description, payload)
            onCommandPayload(payload, description)
        } else {
            _voiceState.value = VoiceState.Error("Unknown command: \"$input\". Tap a phrase below.")
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying speech recognizer: ${e.message}")
            }
        }
    }
}
