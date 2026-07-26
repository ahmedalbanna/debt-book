package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    data class Processing(val partialText: String) : SpeechState()
    data class Success(val recognizedText: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
}

class SpeechRecognizerManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _volumeLevel = MutableStateFlow(0f)
    val volumeLevel: StateFlow<Float> = _volumeLevel.asStateFlow()

    fun isOfflineRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _speechState.value = SpeechState.Error("خدمة التعرف على الصوت غير متاحة مباشرة على هذا الجهاز.")
            return
        }

        try {
            stopListening()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ar-SA")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // Force offline recognition
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            _speechState.value = SpeechState.Listening
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _speechState.value = SpeechState.Error("خطأ في تشغيل الميكروفون: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            // Ignore clean up errors
        }
    }

    /**
     * Emulator / Quick Dialect Test Simulation Helper:
     * Simulates spoken phrase recognition directly for rapid testing
     */
    fun simulateVoiceInput(spokenPhrase: String) {
        _speechState.value = SpeechState.Listening
        _speechState.value = SpeechState.Processing(spokenPhrase)
        _speechState.value = SpeechState.Success(spokenPhrase)
    }

    fun resetState() {
        _speechState.value = SpeechState.Idle
        _volumeLevel.value = 0f
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _speechState.value = SpeechState.Listening
        }

        override fun onBeginningOfSpeech() {
            _speechState.value = SpeechState.Listening
        }

        override fun onRmsChanged(rmsdB: Float) {
            _volumeLevel.value = (rmsdB / 10f).coerceIn(0f, 1f)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _speechState.value = SpeechState.Processing("جاري تحليل الجملة...")
        }

        override fun onError(error: Int) {
            val errorMsg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> "لم يتم التعرّف على الصوت بشكل واضح، يُرجى المحاولة مرة أخرى."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "انتهت مهلة الصوت، اضغط على الميكروفون والتحدث مباشرة."
                SpeechRecognizer.ERROR_AUDIO -> "خطأ في التقاط الصوت."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يرجى منح الإذن لاستخدام الميكروفون."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "خطأ شبكة (تأكد من اختيار الحزمة الأوفلاين)."
                else -> "حدث تعذر غير متوقع، يرجى إعادة المحاولة."
            }
            _speechState.value = SpeechState.Error(errorMsg)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                _speechState.value = SpeechState.Success(recognizedText)
            } else {
                _speechState.value = SpeechState.Error("لم يتم استخراج أي نص صريح.")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                _speechState.value = SpeechState.Processing(matches[0])
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
