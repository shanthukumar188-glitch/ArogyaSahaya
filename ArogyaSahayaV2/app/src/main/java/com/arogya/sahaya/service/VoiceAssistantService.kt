package com.arogya.sahaya.service

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.arogya.sahaya.data.model.VoiceHistory
import com.arogya.sahaya.data.repository.VoiceRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class VoiceAssistantService : Service() {

    @Inject lateinit var voiceRepository: VoiceRepository

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = OkHttpClient()

    // Callbacks to update UI
    var onListeningStarted: (() -> Unit)? = null
    var onSpeechResult: ((String) -> Unit)? = null
    var onAIResponse: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onSpeakingStarted: (() -> Unit)? = null
    var onSpeakingDone: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        initTTS()
    }

    private fun initTTS() {
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("hi", "IN") // Hindi India for rural users
                val res = textToSpeech?.isLanguageAvailable(Locale("hi", "IN"))
                if (res == TextToSpeech.LANG_NOT_SUPPORTED || res == TextToSpeech.LANG_MISSING_DATA) {
                    textToSpeech?.language = Locale.ENGLISH
                }
                textToSpeech?.setSpeechRate(0.85f)
                textToSpeech?.setPitch(1.05f)
            }
        }
    }

    fun startListening(patientName: String, conditions: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            onError?.invoke("Speech recognition not available on this device")
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { onListeningStarted?.invoke() }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Couldn't understand. Please try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Please speak clearly."
                    SpeechRecognizer.ERROR_NETWORK -> "Network error. Check your internet connection."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error. Check microphone permissions."
                    else -> "Voice recognition error. Please try again."
                }
                onError?.invoke(msg)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull() ?: return
                onSpeechResult?.invoke(spokenText)
                processQuery(spokenText, patientName, conditions)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your health question...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    internal fun processQuery(query: String, patientName: String, conditions: String) {
        serviceScope.launch {
            try {
                val response = callGeminiAPI(query, patientName, conditions)
                withContext(Dispatchers.Main) {
                    onAIResponse?.invoke(response)
                    speak(response)
                }
                voiceRepository.saveHistory(VoiceHistory(query = query, response = response))
            } catch (e: Exception) {
                val fallback = getFallbackResponse(query)
                withContext(Dispatchers.Main) {
                    onAIResponse?.invoke(fallback)
                    speak(fallback)
                }
                voiceRepository.saveHistory(VoiceHistory(query = query, response = fallback))
            }
        }
    }

    private fun callGeminiAPI(query: String, patientName: String, conditions: String): String {
        // System prompt tailored for elderly rural Indian healthcare
        val systemPrompt = """You are Arogya Mitra, a compassionate AI health assistant for elderly patients in rural India.
Patient: $patientName. Known conditions: ${conditions.ifBlank { "None specified" }}.
Guidelines:
- Give simple, clear health advice in easy English (avoid complex medical jargon)
- Always recommend consulting a doctor for serious concerns
- Be warm, respectful and patient — this user may be elderly
- Keep answers concise (2-4 sentences max)
- For medication questions, give general info only, not specific prescriptions
- If asked about emergency, always say call 112 first
- You can answer in Hindi/Kannada if the user writes in those languages
- DO NOT diagnose diseases. Provide general wellness guidance only."""

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemPrompt\n\nPatient asks: $query")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 200)
            })
        }

        // NOTE: Replace with your actual Gemini API key from https://aistudio.google.com
        val apiKey = "YOUR_GEMINI_API_KEY_HERE"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: throw IOException("Empty response")

        if (!response.isSuccessful) {
            Log.e("VoiceAI", "API error: $body")
            throw IOException("API error ${response.code}")
        }

        val json = JSONObject(body)
        return json
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
    }

    // Offline fallback responses for common queries
    private fun getFallbackResponse(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("bp") || q.contains("blood pressure") ->
                "Normal blood pressure is 120/80 mmHg. If your readings are consistently high or low, please consult your doctor. Keep a daily log in the Vitals section."
            q.contains("sugar") || q.contains("glucose") || q.contains("diabetes") ->
                "For diabetic patients, fasting blood sugar should ideally be 70-100 mg/dL. Avoid sugary foods, walk 30 minutes daily, and take medicines on time."
            q.contains("medicine") || q.contains("pill") || q.contains("tablet") ->
                "Please check your medicine schedule in the Medicines section. Always take medicines at the same time each day and never skip a dose without doctor's advice."
            q.contains("pain") || q.contains("hurt") ->
                "For mild pain, rest and drink water. If pain is severe, chest pain, or difficulty breathing — call 112 immediately or press the SOS button."
            q.contains("sleep") || q.contains("insomnia") ->
                "For better sleep, avoid tea or coffee after 4 PM, sleep and wake at the same time daily, and keep your room cool and dark."
            q.contains("diet") || q.contains("food") || q.contains("eat") ->
                "Eat small meals every 3-4 hours. Include vegetables, dal, and whole grains. Avoid fried and salty foods. Drink at least 6-8 glasses of water daily."
            q.contains("exercise") || q.contains("walk") ->
                "30 minutes of light walking daily is excellent for health. Start slowly and increase gradually. Avoid exercise if you have chest pain or dizziness."
            q.contains("emergency") || q.contains("help") ->
                "For emergencies, press the big SOS button in the app or call 112. Your emergency contact will be notified immediately."
            else ->
                "I'm here to help with your health questions. You can ask me about medicines, blood pressure, diet, exercise, or any health concern. For serious symptoms, please consult your doctor."
        }
    }

    fun speak(text: String) {
        onSpeakingStarted?.invoke()
        // Clean text for TTS (remove emojis and special chars)
        val cleanText = text.replace(Regex("[^\\p{L}\\p{N}\\p{P}\\s]"), "")
        textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "arogya_tts") {
            onSpeakingDone?.invoke()
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        serviceScope.cancel()
    }
}

// Extension to handle TTS completion callback
private fun TextToSpeech.speak(
    text: String,
    queueMode: Int,
    params: android.os.Bundle?,
    utteranceId: String,
    onDone: () -> Unit
) {
    setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) { onDone() }
        override fun onError(utteranceId: String?) { onDone() }
    })
    speak(text, queueMode, params, utteranceId)
}
