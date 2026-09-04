package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.GeminiService
import com.example.data.Group
import com.example.data.MarketplaceItem
import com.example.data.Post
import com.example.data.ServiceListing
import com.example.data.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Modes for the Voice Assistant
 */
enum class VoiceAssistantMode(val title: String, val description: String) {
    CONVERSATION("Live Dialogue", "Real-time AI voice conversation with Khowar/Urdu/English support"),
    SEARCH("Voice Search", "Search posts, groups, marketplace, services, and users"),
    TRANSLATOR("Voice Translator", "Bidirectional voice-to-voice translation (Khowar ⇄ Urdu ⇄ English)"),
    CONTRIBUTE("Dataset Contributor", "Record & contribute Khowar recordings and text to train models")
}

/**
 * Result of a Voice Search query across the Yarkhoon ecosystem.
 */
data class VoiceSearchResult(
    val spokenQuery: String,
    val detectedLanguage: VoiceLanguage,
    val matchedPosts: List<Post> = emptyList(),
    val matchingPosts: List<Post> = matchedPosts,
    val matchedGroups: List<Group> = emptyList(),
    val matchingGroups: List<Group> = matchedGroups,
    val matchedUsers: List<User> = emptyList(),
    val matchingUsers: List<User> = matchedUsers,
    val matchedMarketplace: List<MarketplaceItem> = emptyList(),
    val matchingMarketplace: List<MarketplaceItem> = matchedMarketplace,
    val matchedServices: List<ServiceListing> = emptyList(),
    val matchingServices: List<ServiceListing> = matchedServices,
    val summarySpeech: String = ""
)

/**
 * Result of Voice-to-Voice Multilingual Translation
 */
data class VoiceTranslationResult(
    val sourceText: String = "",
    val sourceOriginal: String = sourceText,
    val sourceLang: VoiceLanguage = VoiceLanguage.KHOWAR,
    val targetLang: VoiceLanguage = VoiceLanguage.ENGLISH,
    val targetLanguage: VoiceLanguage = targetLang,
    val translatedText: String = "",
    val phoneticRoman: String = "",
    val romanizedText: String = phoneticRoman,
    val linguisticNotes: String = ""
)

/**
 * Voice Assistant Manager provides comprehensive audio STT, TTS, Recording, and AI processing.
 */
class VoiceAssistantManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = "VoiceAssistantManager"

    // Speech Recognizer
    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizerInitialized = false

    // Text to Speech
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // Media Recorder for Voice Messaging & Dataset Audio
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime = 0L

    // Media Player for Audio Note Playback
    private var mediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Observable States
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _isContinuousConversation = MutableStateFlow(true)
    val isContinuousConversation: StateFlow<Boolean> = _isContinuousConversation.asStateFlow()

    private var onTtsDoneCallback: (() -> Unit)? = null

    private val _isRecordingVoiceMessage = MutableStateFlow(false)
    val isRecordingVoiceMessage: StateFlow<Boolean> = _isRecordingVoiceMessage.asStateFlow()
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingVoiceMessage.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()
    val isAudioPlaying: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    private val _activePlayingUrl = MutableStateFlow<String?>("")
    val activePlayingUrl: StateFlow<String?> = _activePlayingUrl.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f) // Normalized 0.0f to 1.0f
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(VoiceLanguage.KHOWAR)
    val selectedLanguage: StateFlow<VoiceLanguage> = _selectedLanguage.asStateFlow()

    private val _selectedLocale = MutableStateFlow(VoiceLanguage.KHOWAR.locale)
    val selectedLocale: StateFlow<Locale> = _selectedLocale.asStateFlow()

    private val _currentMode = MutableStateFlow(VoiceAssistantMode.CONVERSATION)
    val currentMode: StateFlow<VoiceAssistantMode> = _currentMode.asStateFlow()

    private val _statusText = MutableStateFlow("Tap microphone to talk in Khowar, Urdu, or English")
    val statusText: StateFlow<String> = _statusText.asStateFlow()
    val statusMessage: StateFlow<String> = _statusText.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()
    val liveTranscriptText: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _conversationMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val conversationMessages: StateFlow<List<Pair<String, String>>> = _conversationMessages.asStateFlow()

    // Amplitude and timer poller runnable for recording
    private val recordingPoller = object : Runnable {
        override fun run() {
            if (_isRecordingVoiceMessage.value && mediaRecorder != null) {
                try {
                    val maxAmp = mediaRecorder?.maxAmplitude ?: 0
                    val normalized = (maxAmp.toFloat() / 32767f).coerceIn(0f, 1f)
                    _audioAmplitude.value = normalized
                    val duration = ((System.currentTimeMillis() - recordingStartTime) / 1000L).toInt()
                    _recordingDurationSec.value = duration
                } catch (e: Exception) {
                    // Ignore transient errors
                }
                mainHandler.postDelayed(this, 100)
            }
        }
    }

    init {
        initTts()
        initSpeechRecognizer()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsReady = true
                    tts?.language = Locale("ur", "PK")
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }
                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                            mainHandler.post {
                                val callback = onTtsDoneCallback
                                onTtsDoneCallback = null
                                callback?.invoke()
                            }
                        }
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                            mainHandler.post {
                                onTtsDoneCallback = null
                            }
                        }
                    })
                    Log.d(TAG, "TTS initialized successfully.")
                } else {
                    Log.w(TAG, "TTS initialization failed: $status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS initialization exception: ${e.localizedMessage}")
        }
    }

    private fun initSpeechRecognizer() {
        try {
            if (speechRecognizer != null) {
                try {
                    speechRecognizer?.destroy()
                } catch (e: Exception) {
                    Log.w(TAG, "Error cleaning up previous speechRecognizer: ${e.localizedMessage}")
                }
                speechRecognizer = null
            }

            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
                isRecognizerInitialized = true
                Log.d(TAG, "SpeechRecognizer created successfully.")
            } else {
                Log.w(TAG, "SpeechRecognizer service not available or not installed on device/emulator.")
                _statusText.value = "Speech recognition service unavailable on device. You can also type or use quick prompts."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating SpeechRecognizer: ${e.localizedMessage}")
            _statusText.value = "SpeechRecognizer init error: ${e.localizedMessage}"
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
                _statusText.value = "Listening in ${selectedLanguage.value.displayName} (${selectedLanguage.value.nativeName})... Speak now!"
            }

            override fun onBeginningOfSpeech() {
                _statusText.value = "Listening to your voice..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1f)
                _audioAmplitude.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
                _audioAmplitude.value = 0f
                _statusText.value = "Processing voice turn..."
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _audioAmplitude.value = 0f
                val currentText = _liveTranscript.value.trim()
                if (currentText.isNotBlank()) {
                    Log.d(TAG, "Submitting partial transcript despite speech recognition error code $error: $currentText")
                    val cb = onSpeechRecognizedCallback
                    onSpeechRecognizedCallback = null
                    cb?.invoke(currentText)
                } else {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Tap mic to speak clearly."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Tap mic to try again."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                        SpeechRecognizer.ERROR_NETWORK -> "Network issue. Using offline vocabulary."
                        else -> "Tap microphone to speak."
                    }
                    _statusText.value = message
                    Log.w(TAG, "SpeechRecognizer error: $error ($message)")
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                _audioAmplitude.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = if (!matches.isNullOrEmpty()) matches[0] else _liveTranscript.value
                val processedTranscript = KhowarLanguageEngine.normalizeKhowarSpeech(recognized.trim())
                if (processedTranscript.isNotBlank()) {
                    _liveTranscript.value = processedTranscript
                    _statusText.value = "Recognized: \"$processedTranscript\""
                    val cb = onSpeechRecognizedCallback
                    onSpeechRecognizedCallback = null
                    cb?.invoke(processedTranscript)
                } else {
                    _statusText.value = "Tap microphone to speak"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val part = matches[0]
                    _liveTranscript.value = part
                    _statusText.value = "\"$part\""
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private var onSpeechRecognizedCallback: ((String) -> Unit)? = null

    fun setLanguage(language: VoiceLanguage, customLocale: Locale? = null) {
        _selectedLanguage.value = language
        val targetLocale = customLocale ?: language.locale
        _selectedLocale.value = targetLocale
        _statusText.value = "Active language set to ${language.displayName} (${language.nativeName}) [${targetLocale.toLanguageTag()}]"
        if (isTtsReady && tts != null) {
            try {
                tts?.language = targetLocale
            } catch (e: Exception) {
                Log.w(TAG, "Error setting TTS language: ${e.localizedMessage}")
            }
        }
    }

    fun setLocale(locale: Locale) {
        val resolvedLang = VoiceLanguage.fromLocale(locale)
        setLanguage(resolvedLang, locale)
    }

    fun toggleLanguage(): VoiceLanguage {
        val nextLang = when (_selectedLanguage.value) {
            VoiceLanguage.KHOWAR -> VoiceLanguage.URDU
            VoiceLanguage.URDU -> VoiceLanguage.ENGLISH
            VoiceLanguage.ENGLISH -> VoiceLanguage.KHOWAR
        }
        setLanguage(nextLang)
        return nextLang
    }

    fun setMode(mode: VoiceAssistantMode) {
        _currentMode.value = mode
        _statusText.value = "Mode switched to ${mode.title}: ${mode.description}"
    }

    /**
     * Start speech recognition listening with explicit locale and language parameters.
     * Allows seamless toggling between English, Khowar, and Urdu.
     *
     * @param lang Target [VoiceLanguage] (defaults to current selectedLanguage).
     * @param locale Optional explicit [Locale] parameter passed to the speech-to-text request.
     * @param onFinalResult Callback invoked with the recognized speech transcript.
     */
    fun startListening(
        lang: VoiceLanguage = selectedLanguage.value,
        locale: Locale? = null,
        onFinalResult: ((String) -> Unit)? = null,
        onResult: ((String) -> Unit)? = onFinalResult
    ) {
        val targetCallback = onFinalResult ?: onResult ?: {}
        onSpeechRecognizedCallback = targetCallback
        val effectiveLocale = locale ?: lang.locale
        setLanguage(lang, effectiveLocale)
        stopSpeaking()
        _liveTranscript.value = ""

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initSpeechRecognizer()
                }

                val currentRecognizer = speechRecognizer
                if (currentRecognizer == null) {
                    Log.e(TAG, "SpeechRecognizer is null even after initSpeechRecognizer.")
                    _isListening.value = false
                    _statusText.value = "Speech recognition service not ready. Tap microphone or use quick prompts below."
                    return@post
                }

                val targetTag = effectiveLocale.toLanguageTag()

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    // Inject explicit locale parameter into the Speech-to-Text intent
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetTag)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                    putExtra("android.speech.extra.LOCALE", targetTag)

                    when (lang) {
                        VoiceLanguage.KHOWAR -> {
                            // Khowar speech recognition uses the Urdu/Perso-Arabic phonetic engine with dialect fallbacks
                            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ur-PK", "en-US", "kho-PK", "kho"))
                        }
                        VoiceLanguage.URDU -> {
                            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("ur-PK", "en-US"))
                        }
                        VoiceLanguage.ENGLISH -> {
                            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-US", "en-GB"))
                        }
                    }
                }

                currentRecognizer.startListening(intent)
                _isListening.value = true
                _statusText.value = "Listening in ${lang.displayName} (${lang.nativeName})... Speak now!"
                Log.d(TAG, "SpeechRecognizer.startListening initiated with locale $targetTag")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition: ${e.localizedMessage}")
                _isListening.value = false
                _statusText.value = "Speech recognition error: ${e.localizedMessage}. Tap microphone to retry."
            }
        }
    }

    /**
     * Stop speech recognition listening.
     */
    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping speech recognition: ${e.localizedMessage}")
            } finally {
                _isListening.value = false
                _audioAmplitude.value = 0f
                val currentText = _liveTranscript.value.trim()
                if (currentText.isNotBlank()) {
                    val cb = onSpeechRecognizedCallback
                    onSpeechRecognizedCallback = null
                    cb?.invoke(currentText)
                }
            }
        }
    }

    /**
     * Clean text for natural spoken audio output (strip Markdown, URLs, emoji clutter).
     */
    private fun cleanTextForSpeech(rawText: String): String {
        return rawText
            .replace(Regex("\\*\\*|\\*|_|#|`"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .replace(Regex("https?://\\S+"), "")
            .replace(Regex("[🏔️✨❤️🌟🔥👍💬🔗💡🛠️🎙️]"), "")
            .trim()
    }

    /**
     * Speak response aloud via Text-to-Speech.
     */
    fun speak(text: String, language: VoiceLanguage = selectedLanguage.value, onDone: (() -> Unit)? = null) {
        speakText(text, language, onDone)
    }

    fun speakText(
        text: String,
        language: VoiceLanguage = selectedLanguage.value,
        onDone: (() -> Unit)? = null
    ) {
        val clean = cleanTextForSpeech(text)
        if (clean.isBlank()) {
            onDone?.invoke()
            return
        }
        stopSpeaking()
        onTtsDoneCallback = onDone

        if (isTtsReady && tts != null) {
            val hasUrduOrKhowar = clean.any { it in '\u0600'..'\u06FF' }
            val ttsLocale = if (hasUrduOrKhowar || language == VoiceLanguage.KHOWAR || language == VoiceLanguage.URDU) {
                Locale("ur", "PK")
            } else {
                Locale.US
            }
            try {
                tts?.language = ttsLocale
            } catch (e: Exception) {
                Log.w(TAG, "Error setting TTS locale: ${e.localizedMessage}")
            }

            val utteranceId = "yarkhoon_tts_${System.currentTimeMillis()}"
            val res = tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (res == TextToSpeech.SUCCESS) {
                _isSpeaking.value = true
            } else {
                Log.w(TAG, "TTS speak returned code $res. Invoking onDone fallback.")
                _isSpeaking.value = false
                onTtsDoneCallback = null
                onDone?.invoke()
            }
        } else {
            Log.w(TAG, "TTS not ready to speak.")
            _isSpeaking.value = false
            onTtsDoneCallback = null
            onDone?.invoke()
        }
    }

    fun setProcessing(processing: Boolean) {
        _isProcessing.value = processing
        if (processing) {
            _statusText.value = "Thinking in ${selectedLanguage.value.displayName} & Gemini..."
        }
    }

    fun setContinuousConversation(enabled: Boolean) {
        _isContinuousConversation.value = enabled
    }

    /**
     * Stop ongoing TTS speech output.
     */
    fun stopSpeaking() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    // ==================== VOICE MESSAGING & RECORDING ====================

    fun startAudioRecording(): File? = startVoiceRecording()

    fun startVoiceRecording(): File? {
        try {
            val audioDir = File(context.cacheDir, "voice_recordings").apply { mkdirs() }
            val file = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = file
            recordingStartTime = System.currentTimeMillis()
            _recordingDurationSec.value = 0

            mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            _isRecordingVoiceMessage.value = true
            mainHandler.post(recordingPoller)
            Log.d(TAG, "Started voice recording to ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start voice recording: ${e.localizedMessage}")
            _isRecordingVoiceMessage.value = false
            return null
        }
    }

    fun stopAudioRecording(onResult: ((File?, Long) -> Unit)? = null): Pair<File?, Int> {
        val pair = stopVoiceRecording()
        onResult?.invoke(pair.first, pair.second * 1000L)
        return pair
    }

    fun stopVoiceRecording(): Pair<File?, Int> {
        return try {
            mainHandler.removeCallbacks(recordingPoller)
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            _isRecordingVoiceMessage.value = false
            _audioAmplitude.value = 0f

            val durationSec = ((System.currentTimeMillis() - recordingStartTime) / 1000L).toInt().coerceAtLeast(1)
            val file = currentRecordingFile
            currentRecordingFile = null
            _recordingDurationSec.value = 0
            Log.d(TAG, "Finished voice recording. Duration: ${durationSec}s, File: ${file?.absolutePath}")
            Pair(file, durationSec)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping voice recording: ${e.localizedMessage}")
            mediaRecorder = null
            _isRecordingVoiceMessage.value = false
            _audioAmplitude.value = 0f
            _recordingDurationSec.value = 0
            Pair(null, 0)
        }
    }

    fun cancelAudioRecording() = cancelVoiceRecording()

    fun cancelVoiceRecording() {
        try {
            mainHandler.removeCallbacks(recordingPoller)
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            currentRecordingFile?.delete()
            currentRecordingFile = null
            _isRecordingVoiceMessage.value = false
            _audioAmplitude.value = 0f
            _recordingDurationSec.value = 0
        } catch (e: Exception) {
            Log.w(TAG, "Error canceling recording: ${e.localizedMessage}")
        }
    }

    /**
     * Play an audio file or URL.
     */
    fun playAudioFile(filePath: String, onCompletion: () -> Unit = {}) {
        stopAudioPlayback()
        try {
            _activePlayingUrl.value = filePath
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _isPlayingAudio.value = false
                    _activePlayingUrl.value = ""
                    onCompletion()
                }
                start()
            }
            _isPlayingAudio.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio: ${e.localizedMessage}")
            _isPlayingAudio.value = false
            _activePlayingUrl.value = ""
        }
    }

    /**
     * Stop audio playback.
     */
    fun stopAudioPlayback() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing mediaPlayer: ${e.localizedMessage}")
        } finally {
            _isPlayingAudio.value = false
            _activePlayingUrl.value = ""
        }
    }

    // ==================== ADVANCED AI VOICE DIALOGUE & TRANSLATION ====================

    suspend fun processVoiceDialogueTurn(
        spokenText: String,
        conversationHistory: List<Pair<String, String>>,
        targetLang: VoiceLanguage = selectedLanguage.value
    ): String = withContext(Dispatchers.IO) {
        val model = "gemini-2.5-flash"
        val systemPrompt = """
            You are the Yarkhoon AI Voice Assistant, an intelligent spoken voice companion for the Chitral & Yarkhoon Valley social platform.
            Primary Language: Khowar (کھوار / Chitrali). Fallback Languages: Urdu and English.
            Knowledge Base: Deep expertise in Khowar vocabulary, Chitral culture (Polo, Ghalmandi, Chitrali Sitar, Broghil Festival, Shandur, Tirich Mir), geography (Yarkhoon, Mastuj, Booni, Drosh, Lotkoh), tourism, road updates, and local community life.
            
            Current Spoken Language Mode: ${targetLang.displayName} (${targetLang.nativeName}).
            
            Strict Spoken & Low-Bandwidth Guidelines:
            1. If spoken/prompted in Khowar or about Khowar, respond in natural, authentic Khowar with both Nastaliq script and Roman Khowar pronunciation, plus short Urdu/English meaning.
            2. If spoken in Urdu or English, reply directly in that language with warm, friendly tone.
            3. LOW-BANDWIDTH & FAST SPEECH: Keep replies under 2-3 short sentences. Avoid verbose essays so audio synthesis is fast and clear over mountain 2G/3G/4G networks.
            4. If the user asks for help or greetings, welcome them warmly in Chitrali style ("جوشپہ کوسوری؟ / Assalam-o-Alaikum").
        """.trimIndent()

        try {
            val response = GeminiService.generateChatResponse(
                model = model,
                systemInstruction = systemPrompt,
                conversationHistory = conversationHistory + Pair("user", spokenText),
                enableSearchGrounding = false
            )
            if (response.error != null) {
                Log.w(TAG, "Gemini API error (${response.error}), falling back to localized Chitral response")
                getOfflineVoiceResponse(spokenText, targetLang)
            } else {
                response.text.trim()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini online dialogue unavailable, evaluating offline local knowledge fallback: ${e.localizedMessage}")
            getOfflineVoiceResponse(spokenText, targetLang)
        }
    }

    /**
     * Resilient offline fallback engine for zero/low connectivity mountain environments.
     * Matches user input against local Khowar lexicon and Chitrali knowledge.
     */
    private fun getOfflineVoiceResponse(query: String, lang: VoiceLanguage): String {
        val cleanQuery = query.trim().lowercase()
        val lexiconMatches = KhowarLanguageEngine.searchLocalLexicon(cleanQuery)

        if (lexiconMatches.isNotEmpty()) {
            val item = lexiconMatches.first()
            return when (lang) {
                VoiceLanguage.KHOWAR -> "${item.khowarNastaliq} (${item.khowarRoman}) - ${item.urduMeaning}"
                VoiceLanguage.URDU -> "کھوار میں: ${item.khowarNastaliq} (${item.khowarRoman}) - معنی: ${item.urduMeaning}"
                VoiceLanguage.ENGLISH -> "In Khowar: ${item.khowarNastaliq} (${item.khowarRoman}) means '${item.englishMeaning}'"
            }
        }

        // Common greeting and valley intent fallbacks
        if (cleanQuery.contains("salam") || cleanQuery.contains("hello") || cleanQuery.contains("سلام") || cleanQuery.contains("جوشپہ")) {
            return when (lang) {
                VoiceLanguage.KHOWAR -> "سلام! جوشپہ کوسوری؟ مہ نام یارخون AI شیر۔ (Salam! Joshpa kosori? Mah nam Yarkhoon AI sher.)"
                VoiceLanguage.URDU -> "وعلیکم السلام! یارخون اے آئی میں خوش آمدید، میں آپ کی کیا مدد کر سکتا ہوں؟"
                VoiceLanguage.ENGLISH -> "Hello and welcome to Yarkhoon AI Voice Assistant! How can I assist you in Chitral today?"
            }
        }

        if (cleanQuery.contains("yarkhoon") || cleanQuery.contains("یارخون") || cleanQuery.contains("weather") || cleanQuery.contains("موسم")) {
            return when (lang) {
                VoiceLanguage.KHOWAR -> "یارخونو موسم گارم اور خوبصورت شیر، ہوش کیرو! (Yarkhoono mosam shashta sher.)"
                VoiceLanguage.URDU -> "یارخون اور چترال میں موسم خوشگوار ہے اور سڑکیں کھلی ہیں۔"
                VoiceLanguage.ENGLISH -> "Yarkhoon Valley weather is pleasant with clear skies and active mountain passes."
            }
        }

        return when (lang) {
            VoiceLanguage.KHOWAR -> "مہ مہربانی! تان سوال دوبارہ لوزاوے۔ (Please repeat your question in Khowar or Urdu.)"
            VoiceLanguage.URDU -> "آپ کی بات موصول ہو گئی ہے۔ براہ کرم اپنا سوال دوبارہ دہرائیں۔"
            VoiceLanguage.ENGLISH -> "I heard your voice. Please repeat your query or select a quick topic below."
        }
    }

    suspend fun translateSpokenText(
        text: String,
        sourceLang: VoiceLanguage,
        targetLang: VoiceLanguage
    ): VoiceTranslationResult = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            return@withContext VoiceTranslationResult(
                sourceText = text,
                sourceOriginal = text,
                sourceLang = sourceLang,
                targetLang = targetLang,
                targetLanguage = targetLang,
                translatedText = ""
            )
        }

        val prompt = """
            Translate the following text from ${sourceLang.displayName} to ${targetLang.displayName}.
            
            Source Text: "$text"
            
            Special Khowar Linguistic Rules:
            - If translating TO Khowar, provide authentic Khowar vocabulary in Nastaliq script along with Roman Khowar pronunciation in parentheses.
            - If translating FROM Khowar, accurately convey the cultural and idiomatic meaning.
            - Provide ONLY the translation directly without conversational filler.
        """.trimIndent()

        val response = GeminiService.generateChatResponse(
            model = "gemini-2.5-flash",
            systemInstruction = "You are a professional multilingual translator specializing in Khowar (Chitrali), Urdu, and English.",
            conversationHistory = listOf(Pair("user", prompt)),
            enableSearchGrounding = false
        )

        val cleanTranslation = response.text.trim()
        val romanized = if (targetLang == VoiceLanguage.KHOWAR) KhowarLanguageEngine.normalizeKhowarSpeech(cleanTranslation) else ""

        VoiceTranslationResult(
            sourceText = text,
            sourceOriginal = text,
            sourceLang = sourceLang,
            targetLang = targetLang,
            targetLanguage = targetLang,
            translatedText = cleanTranslation,
            phoneticRoman = romanized,
            romanizedText = romanized,
            linguisticNotes = "Translated between ${sourceLang.displayName} and ${targetLang.displayName}"
        )
    }

    suspend fun executeVoiceSearch(
        voiceQuery: String,
        allPosts: List<Post>,
        allGroups: List<Group>,
        allUsers: List<User>,
        allMarketplace: List<MarketplaceItem>,
        allServices: List<ServiceListing>
    ): VoiceSearchResult = withContext(Dispatchers.IO) {
        val query = voiceQuery.trim().lowercase()
        val detectedLang = selectedLanguage.value

        val matchedUsers = allUsers.filter {
            it.fullName.lowercase().contains(query) ||
            it.username.lowercase().contains(query) ||
            it.bio.lowercase().contains(query) ||
            it.location.lowercase().contains(query)
        }.take(5)

        val matchedPosts = allPosts.filter {
            it.content.lowercase().contains(query) ||
            it.authorName.lowercase().contains(query)
        }.take(5)

        val matchedGroups = allGroups.filter {
            it.name.lowercase().contains(query) ||
            it.description.lowercase().contains(query) ||
            it.category.lowercase().contains(query)
        }.take(5)

        val matchedMarketplace = allMarketplace.filter {
            it.title.lowercase().contains(query) ||
            it.description.lowercase().contains(query) ||
            it.category.lowercase().contains(query)
        }.take(5)

        val matchedServices = allServices.filter {
            it.serviceType.lowercase().contains(query) ||
            it.description.lowercase().contains(query) ||
            it.providerName.lowercase().contains(query)
        }.take(5)

        val totalFound = matchedUsers.size + matchedPosts.size + matchedGroups.size + matchedMarketplace.size + matchedServices.size

        val summarySpeech = if (totalFound > 0) {
            when (detectedLang) {
                VoiceLanguage.KHOWAR -> "تہ آواز سرا $totalFound نتیجہ ملین یارخون پٹفارما۔ پوسٹ، گروپ او ممبرز تیار شینی۔"
                VoiceLanguage.URDU -> "آپ کی آواز کی تلاش کے مطابق $totalFound نتائج ملے ہیں، جن میں پوسٹس، گروپس اور صارفین شامل ہیں۔"
                VoiceLanguage.ENGLISH -> "Found $totalFound results for \"$voiceQuery\" across posts, groups, marketplace, services, and users."
            }
        } else {
            when (detectedLang) {
                VoiceLanguage.KHOWAR -> "کوئی نتیجہ نو ملیا، دبارہ کوشش کرور۔"
                VoiceLanguage.URDU -> "کوئی نتیجہ نہیں ملا۔ براہ کرم دوبارہ کوشش کریں۔"
                VoiceLanguage.ENGLISH -> "No matching results found for \"$voiceQuery\". Please try another search term."
            }
        }

        VoiceSearchResult(
            spokenQuery = voiceQuery,
            detectedLanguage = detectedLang,
            matchedPosts = matchedPosts,
            matchingPosts = matchedPosts,
            matchedGroups = matchedGroups,
            matchingGroups = matchedGroups,
            matchedUsers = matchedUsers,
            matchingUsers = matchedUsers,
            matchedMarketplace = matchedMarketplace,
            matchingMarketplace = matchedMarketplace,
            matchedServices = matchedServices,
            matchingServices = matchedServices,
            summarySpeech = summarySpeech
        )
    }

    fun destroy() {
        try {
            stopListening()
            stopSpeaking()
            stopAudioPlayback()
            mediaRecorder?.release()
            mediaRecorder = null
            speechRecognizer?.destroy()
            speechRecognizer = null
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.w(TAG, "Error in destroy: ${e.localizedMessage}")
        }
    }
}
