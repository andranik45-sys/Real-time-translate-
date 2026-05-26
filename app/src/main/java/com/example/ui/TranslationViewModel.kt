package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiTranslationService
import com.example.data.TranslationDatabase
import com.example.data.TranslationEntity
import com.example.data.TranslationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String,
    val locale: Locale
)

enum class TranslationStyle(
    val title: String,
    val description: String,
    val emoji: String,
    val prompt: String
) {
    STANDARD("Direct", "Accurate literal translation", "🎯", ""),
    CASUAL("Casual", "Friendly and conversational slang", "👋", "Translate using a natural, warm, conversational style with common idioms or slang appropriate for friendly exchanges."),
    FORMAL("Formal", "Polite and business level wording", "💼", "Translate using polite, respectful language registers, standard honorifics, and elegant professional wording suitable for business or formal scenarios."),
    POETIC("Poetic", "Creative phrasing rich in emotion", "✨", "Translate with a creative, poetic, and artistic flair to preserve deeper emotional weight or descriptive beauty over strict literal translation.")
}

enum class ContextMod(
    val title: String,
    val description: String,
    val emoji: String,
    val prompt: String
) {
    GENERAL("General", "Standard translation context", "🌐", ""),
    TRAVEL("Travel", "Transit, flights, hotel, customs", "✈️", "The dialogue context is traveling, airport encounters, transportation, flights, and customs."),
    DINING("Dining", "Ordering food, coffee, cafes", "🍳", "The dialogue context is sitting in a restaurant, ordering food or drinks, cafés, and paying bills."),
    SHOPPING("Shopping", "Retail stores, prices, sizing", "🛍️", "The dialogue context is browsing a store, checking prices, retail shopping, apparel sizes, and discounts."),
    MEDICAL("Medical", "Health symptoms, clinic, pharmacy", "🏥", "The dialogue context is medical support, visiting a pharmacy or clinic, describing health symptoms or emergencies.")
}

class TranslationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = TranslationDatabase.getDatabase(application)
    private val repository = TranslationRepository(db.translationDao())
    private val geminiService = GeminiTranslationService()

    // Supported Languages List
    val languages = listOf(
        Language("en", "English", "English", "🇺🇸", Locale.US),
        Language("es", "Spanish", "Español", "🇪🇸", Locale("es", "ES")),
        Language("fr", "French", "Français", "🇫🇷", Locale.FRANCE),
        Language("de", "German", "Deutsch", "🇩🇪", Locale.GERMANY),
        Language("it", "Italian", "Italiano", "🇮🇹", Locale.ITALY),
        Language("ja", "Japanese", "日本語", "🇯🇵", Locale.JAPAN),
        Language("ko", "Korean", "한국어", "🇰🇷", Locale.KOREA),
        Language("zh", "Chinese", "中文", "🇨🇳", Locale.SIMPLIFIED_CHINESE),
        Language("pt", "Portuguese", "Português", "🇵🇹", Locale("pt", "PT")),
        Language("ru", "Russian", "Русский", "🇷🇺", Locale("ru", "RU")),
        Language("hi", "Hindi", "हिन्दी", "🇮🇳", Locale("hi", "IN")),
        Language("ar", "Arabic", "العربية", "🇸🇦", Locale("ar", "SA")),
        Language("hy", "Armenian", "Հայերեն", "🇦🇲", Locale("hy", "AM")),
        Language("tr", "Turkish", "Türkçe", "🇹🇷", Locale("tr", "TR")),
        Language("nl", "Dutch", "Nederlands", "🇳🇱", Locale("nl", "NL")),
        Language("pl", "Polish", "Polski", "🇵🇱", Locale("pl", "PL")),
        Language("vi", "Vietnamese", "Tiếng Việt", "🇻🇳", Locale("vi", "VN")),
        Language("th", "Thai", "ไทย", "🇹🇭", Locale("th", "TH")),
        Language("id", "Indonesian", "Bahasa Indonesia", "🇮🇩", Locale("id", "ID")),
        Language("uk", "Ukrainian", "Українська", "🇺🇦", Locale("uk", "UA")),
        Language("el", "Greek", "Ελληνικά", "🇬🇷", Locale("el", "GR")),
        Language("sv", "Swedish", "Svenska", "🇸🇪", Locale("sv", "SE")),
        Language("he", "Hebrew", "עברית", "🇮🇱", Locale("he", "IL")),
        Language("cs", "Czech", "Čeština", "🇨🇿", Locale("cs", "CZ")),
        Language("da", "Danish", "Dansk", "🇩🇰", Locale("da", "DK")),
        Language("fi", "Finnish", "Suomi", "🇫🇮", Locale("fi", "FI")),
        Language("no", "Norwegian", "Norsk", "🇳🇴", Locale("no", "NO")),
        Language("ro", "Romanian", "Română", "🇷🇴", Locale("ro", "RO")),
        Language("hu", "Hungarian", "Magyar", "🇭🇺", Locale("hu", "HU")),
        Language("ms", "Malay", "Bahasa Melayu", "🇲🇾", Locale("ms", "MY")),
        Language("fa", "Persian", "فارسی", "🇮🇷", Locale("fa", "IR"))
    )

    // Speech & Translation State
    private val _sourceText = MutableStateFlow("")
    val sourceText: StateFlow<String> = _sourceText.asStateFlow()

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText.asStateFlow()

    private val _sourceLanguage = MutableStateFlow(languages[0]) // English
    val sourceLanguage: StateFlow<Language> = _sourceLanguage.asStateFlow()

    private val _targetLanguage = MutableStateFlow(languages[1]) // Spanish
    val targetLanguage: StateFlow<Language> = _targetLanguage.asStateFlow()

    private val _appInterfaceLanguage = MutableStateFlow(languages[0]) // English
    val appInterfaceLanguage: StateFlow<Language> = _appInterfaceLanguage.asStateFlow()

    private val _isSplitEarbudMode = MutableStateFlow(false)
    val isSplitEarbudMode: StateFlow<Boolean> = _isSplitEarbudMode.asStateFlow()

    fun toggleSplitEarbudMode() {
        _isSplitEarbudMode.value = !_isSplitEarbudMode.value
    }

    fun isBluetoothHeadsetsConnected(): Boolean {
        return try {
            val audioManager = getApplication<Application>().getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                devices.any { device ->
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    if (android.os.Build.VERSION.SDK_INT >= 31) {
                        device.type == 26 || device.type == 27 // TYPE_BLE_HEADSET, TYPE_BLE_SPEAKER
                    } else false
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.isBluetoothA2dpOn
            }
        } catch (e: Exception) {
            false
        }
    }

    private val _uiTranslations = MutableStateFlow<Map<String, String>>(emptyMap())
    val uiTranslations: StateFlow<Map<String, String>> = _uiTranslations.asStateFlow()

    fun selectInterfaceLanguage(language: Language) {
        _appInterfaceLanguage.value = language
        updateUiTranslations(language)
    }

    private fun updateUiTranslations(language: Language) {
        val langCode = language.code
        if (langCode == "en") {
            _uiTranslations.value = emptyMap()
            return
        }

        // Apply offline fallback if available
        val fallback = UiLocalizer.getFallback(langCode)
        if (fallback != null) {
            _uiTranslations.value = fallback
        } else {
            // Seed with keys themselves as initial values
            _uiTranslations.value = UiLocalizer.keys.associateWith { it }
        }

        // Fetch dynamic AI translations in parallel asynchronously so the UI isn't blocked
        viewModelScope.launch {
            try {
                val keysToTranslate = UiLocalizer.keys.joinToString("\n") { "- $it" }
                val prompt = """
                    You are an expert app localization translator. Translate the following list of standard application UI labels accurately into ${language.name} (${language.nativeName}).
                    Provide ONLY the raw direct translated equivalents, strictly in order, one per line.
                    Do NOT prefix lines with numbers, dashes, markers, or quotes. Output exactly ${UiLocalizer.keys.size} translated lines.

                    Label list:
                    $keysToTranslate
                """.trimIndent()

                val result = geminiService.translateText(
                    text = prompt,
                    sourceLanguage = "English",
                    targetLanguage = language.name
                )

                result.fold(
                    onSuccess = { rawTranslated ->
                        val lines = rawTranslated.split("\n")
                            .map { it.trim().removePrefix("-").trim().removePrefix("\"").removeSuffix("\"") }
                            .filter { it.isNotEmpty() }
                        
                        // Clean up any extra labels that might have been returned and match elements up
                        if (lines.size >= UiLocalizer.keys.size) {
                            val newMap = UiLocalizer.keys.zip(lines).toMap()
                            _uiTranslations.value = newMap
                        } else if (lines.isNotEmpty()) {
                            val currentMap = _uiTranslations.value.toMutableMap()
                            UiLocalizer.keys.zip(lines).forEach { (key, value) ->
                                currentMap[key] = value
                            }
                            _uiTranslations.value = currentMap
                        }
                    },
                    onFailure = { err ->
                        Log.e("TranslationViewModel", "AI Interface localization error: ${err.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("TranslationViewModel", "Exception loading dynamic localization", e)
            }
        }
    }

    private val _selectedStyle = MutableStateFlow(TranslationStyle.STANDARD)
    val selectedStyle: StateFlow<TranslationStyle> = _selectedStyle.asStateFlow()

    private val _selectedContext = MutableStateFlow(ContextMod.GENERAL)
    val selectedContext: StateFlow<ContextMod> = _selectedContext.asStateFlow()

    fun selectStyle(style: TranslationStyle) {
        _selectedStyle.value = style
        if (_sourceText.value.isNotBlank()) {
            translateCurrentText()
        }
    }

    fun selectContext(context: ContextMod) {
        _selectedContext.value = context
        if (_sourceText.value.isNotBlank()) {
            translateCurrentText()
        }
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Speech Recognition State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _listeningLanguage = MutableStateFlow<Language?>(null)
    val listeningLanguage: StateFlow<Language?> = _listeningLanguage.asStateFlow()

    private val _partialTargetText = MutableStateFlow("")
    val partialTargetText: StateFlow<String> = _partialTargetText.asStateFlow()

    private val _audioVolume = MutableStateFlow(0f) // Normalized RMS value for waveform animation
    val audioVolume: StateFlow<Float> = _audioVolume.asStateFlow()

    // TextToSpeech Speaking States
    private val _isSpeakingSource = MutableStateFlow(false)
    val isSpeakingSource: StateFlow<Boolean> = _isSpeakingSource.asStateFlow()

    private val _isSpeakingTarget = MutableStateFlow(false)
    val isSpeakingTarget: StateFlow<Boolean> = _isSpeakingTarget.asStateFlow()

    // Room Persistent Translation History
    val translationHistory: StateFlow<List<TranslationEntity>> = repository.allTranslations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var autoTranslateJob: Job? = null

    init {
        initSpeechRecognizer()
        initTextToSpeech()
    }

    private fun initSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isRecording.value = true
                            _audioVolume.value = 0f
                        }

                        override fun onBeginningOfSpeech() {
                            _audioVolume.value = 0.5f
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // rmsdB generally ranges from -2 to 10+. Normalize it to 0f - 1f for clean Compose layout
                            val dbNormalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _audioVolume.value = dbNormalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _audioVolume.value = 0f
                        }

                        override fun onError(error: Int) {
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient record permissions (grant RECORD_AUDIO)"
                                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
                                SpeechRecognizer.ERROR_SERVER -> "Server error"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                                else -> "Speech recognition error: $error"
                            }
                            Log.e("TranslationVM", "Speech error: $msg ($error)")
                            _isRecording.value = false
                            _audioVolume.value = 0f
                            // Only set error message if it's not a harmless timeout or no match to avoid intrusive dialogs
                            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                                _errorMessage.value = msg
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognized = matches?.firstOrNull() ?: ""
                            if (recognized.isNotBlank()) {
                                handleSpeechResults(recognized)
                            }
                            _isRecording.value = false
                            _audioVolume.value = 0f
                            _partialTargetText.value = ""
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partialText = matches?.firstOrNull() ?: ""
                            if (partialText.isNotBlank()) {
                                val listening = _listeningLanguage.value ?: _sourceLanguage.value
                                if (listening.code == _sourceLanguage.value.code) {
                                    _sourceText.value = partialText
                                } else {
                                    _partialTargetText.value = partialText
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } else {
                Log.w("TranslationVM", "SpeechRecognizer not available on this device")
            }
        } catch (e: Exception) {
            Log.e("TranslationVM", "Failed to initialize SpeechRecognizer: ${e.message}")
        }
    }

    private fun initTextToSpeech() {
        textToSpeech = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        when (utteranceId) {
                            "source" -> {
                                _isSpeakingSource.value = true
                                _isSpeakingTarget.value = false
                            }
                            "target" -> {
                                _isSpeakingSource.value = false
                                _isSpeakingTarget.value = true
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeakingSource.value = false
                        _isSpeakingTarget.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeakingSource.value = false
                        _isSpeakingTarget.value = false
                    }
                })
            } else {
                Log.e("TranslationVM", "TextToSpeech initialization failed")
            }
        }
    }

    fun updateSourceText(text: String, performInstantTranslation: Boolean = false) {
        _sourceText.value = text
        _errorMessage.value = null // clear past errors

        if (performInstantTranslation) {
            translateCurrentText()
        } else {
            // Debounce manual typing translations by 800ms to reduce API requests
            autoTranslateJob?.cancel()
            autoTranslateJob = viewModelScope.launch {
                delay(800)
                if (text.isNotBlank()) {
                    translateCurrentText()
                }
            }
        }
    }

    fun selectSourceLanguage(language: Language) {
        if (language == _targetLanguage.value) {
            swapLanguages()
            return
        }
        _sourceLanguage.value = language
        if (_sourceText.value.isNotBlank()) {
            translateCurrentText()
        }
    }

    fun selectTargetLanguage(language: Language) {
        if (language == _sourceLanguage.value) {
            swapLanguages()
            return
        }
        _targetLanguage.value = language
        if (_sourceText.value.isNotBlank()) {
            translateCurrentText()
        }
    }

    fun swapLanguages() {
        val oldSourceLang = _sourceLanguage.value
        val oldTargetLang = _targetLanguage.value
        val oldSourceText = _sourceText.value
        val oldTargetText = _translatedText.value

        _sourceLanguage.value = oldTargetLang
        _targetLanguage.value = oldSourceLang
        _sourceText.value = oldTargetText
        _translatedText.value = oldSourceText
    }

    fun clearText() {
        _sourceText.value = ""
        _translatedText.value = ""
        _errorMessage.value = null
        stopSpeaking()
        stopListening()
    }

    fun startListening(language: Language) {
        _listeningLanguage.value = language
        startListening()
    }

    fun prepareToListen(language: Language) {
        _listeningLanguage.value = language
    }

    fun startListening() {
        // Stop currently playing text-to-speech outputs
        stopSpeaking()

        val sr = speechRecognizer
        if (sr == null) {
            _errorMessage.value = "Speech recognition is not available or configured on this virtual device. Please type your text inputs manually!"
            return
        }

        val targetLanguageCode = _listeningLanguage.value?.code ?: _sourceLanguage.value.code
        _partialTargetText.value = ""

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLanguageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            sr.startListening(intent)
            _isRecording.value = true
        } catch (e: Exception) {
            Log.e("TranslationVM", "Error starting listening: ${e.message}")
            _errorMessage.value = "Error starting voice recognition: ${e.message}"
        }
    }

    private fun handleSpeechResults(recognized: String) {
        val listening = _listeningLanguage.value ?: _sourceLanguage.value

        if (listening.code == _sourceLanguage.value.code) {
            // Speaker A (Source)
            _sourceText.value = recognized
            viewModelScope.launch {
                _isLoading.value = true
                _errorMessage.value = null
                val result = geminiService.translateText(
                    text = recognized,
                    sourceLanguage = _sourceLanguage.value.name,
                    targetLanguage = _targetLanguage.value.name,
                    stylePrompt = _selectedStyle.value.prompt,
                    contextPrompt = _selectedContext.value.prompt
                )
                result.fold(
                    onSuccess = { translation ->
                        _translatedText.value = translation
                        _isLoading.value = false
                        saveToHistory(
                            original = recognized,
                            translated = translation,
                            src = _sourceLanguage.value,
                            target = _targetLanguage.value
                        )
                        // Speak translation out loud automatically for hands-free conversational ease
                        speakText(translation, _targetLanguage.value, isSource = false)
                    },
                    onFailure = { error ->
                        _isLoading.value = false
                        val errorMsg = error.message ?: "Unknown connection error occurred"
                        _errorMessage.value = errorMsg
                        if (errorMsg.contains("API key", ignoreCase = true)) {
                            // Offline/demo fallback
                            val style = _selectedStyle.value.title
                            val context = _selectedContext.value.title
                            val mockTranslation = "[Demo - Style: $style, Context: $context] $recognized"
                            _translatedText.value = mockTranslation
                            saveToHistory(
                                original = recognized,
                                translated = mockTranslation,
                                src = _sourceLanguage.value,
                                target = _targetLanguage.value
                            )
                            speakText(mockTranslation, _targetLanguage.value, isSource = false)
                        }
                    }
                )
            }
        } else {
            // Speaker B (Target)
            _partialTargetText.value = recognized
            viewModelScope.launch {
                _isLoading.value = true
                _errorMessage.value = null
                val result = geminiService.translateText(
                    text = recognized,
                    sourceLanguage = _targetLanguage.value.name,
                    targetLanguage = _sourceLanguage.value.name,
                    stylePrompt = _selectedStyle.value.prompt,
                    contextPrompt = _selectedContext.value.prompt
                )
                result.fold(
                    onSuccess = { translation ->
                        _isLoading.value = false
                        saveToHistory(
                            original = recognized,
                            translated = translation,
                            src = _targetLanguage.value,
                            target = _sourceLanguage.value
                        )
                        // Speak translation in source language out loud automatically
                        speakText(translation, _sourceLanguage.value, isSource = true)
                    },
                    onFailure = { error ->
                        _isLoading.value = false
                        val errorMsg = error.message ?: "Unknown connection error occurred"
                        _errorMessage.value = errorMsg
                        if (errorMsg.contains("API key", ignoreCase = true)) {
                            // Offline/demo fallback
                            val style = _selectedStyle.value.title
                            val context = _selectedContext.value.title
                            val mockTranslation = "[Demo - Style: $style, Context: $context] $recognized"
                            saveToHistory(
                                original = recognized,
                                translated = mockTranslation,
                                src = _targetLanguage.value,
                                target = _sourceLanguage.value
                            )
                            speakText(mockTranslation, _sourceLanguage.value, isSource = true)
                        }
                    }
                )
            }
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isRecording.value = false
        _audioVolume.value = 0f
    }

    fun translateCurrentText() {
        val textToTranslate = _sourceText.value.trim()
        if (textToTranslate.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = geminiService.translateText(
                text = textToTranslate,
                sourceLanguage = _sourceLanguage.value.name,
                targetLanguage = _targetLanguage.value.name,
                stylePrompt = _selectedStyle.value.prompt,
                contextPrompt = _selectedContext.value.prompt
            )

            result.fold(
                onSuccess = { translation ->
                    _translatedText.value = translation
                    _isLoading.value = false

                    // Persistence: Auto-Save Translation to history list inside SQLite Room database!
                    saveToHistory(
                        original = textToTranslate,
                        translated = translation,
                        src = _sourceLanguage.value,
                        target = _targetLanguage.value
                    )
                },
                onFailure = { error ->
                    _isLoading.value = false
                    val errorMsg = error.message ?: "Unknown connection error occurred"
                    _errorMessage.value = errorMsg

                    // Provide fully operational interface mock fallback if key is not filled or connection offline
                    if (errorMsg.contains("API key", ignoreCase = true)) {
                        triggerLocalMockFallback(textToTranslate)
                    }
                }
            )
        }
    }

    private fun triggerLocalMockFallback(textToTranslate: String) {
        // Beautiful fallback algorithm to allow robust testing
        val localText = textToTranslate
        val src = _sourceLanguage.value.name
        val dst = _targetLanguage.value.name
        val style = _selectedStyle.value.title
        val context = _selectedContext.value.title
        val mockTranslation = "[Demo Mode - Style: $style, Context: $context] Translating '$localText' from $src to $dst. (Configure your GEMINI_API_KEY in the Secrets panel to view real AI translations)."
        _translatedText.value = mockTranslation

        saveToHistory(
            original = localText,
            translated = mockTranslation,
            src = _sourceLanguage.value,
            target = _targetLanguage.value
        )
    }

    private fun saveToHistory(original: String, translated: String, src: Language, target: Language) {
        viewModelScope.launch {
            try {
                repository.insert(
                    TranslationEntity(
                        originalText = original,
                        translatedText = translated,
                        sourceLangName = src.name,
                        targetLangName = target.name,
                        sourceLangCode = src.code,
                        targetLangCode = target.code,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e("TranslationVM", "Room save error: ${e.message}")
            }
        }
    }

    fun speakText(text: String, language: Language, isSource: Boolean) {
        if (!isTtsInitialized) {
            _errorMessage.value = "Speech output engine is still loading. Please tap again in a moment."
            return
        }

        val speechEngine = textToSpeech
        if (speechEngine == null) {
            _errorMessage.value = "Speech reproduction API not available."
            return
        }

        val alreadySpeaking = if (isSource) _isSpeakingSource.value else _isSpeakingTarget.value
        if (alreadySpeaking) {
            stopSpeaking()
            return
        }

        try {
            // Configure the engine's current language representation
            val result = speechEngine.setLanguage(language.locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // If the exact country locale is missing, fallback to language level locale
                val fallbackLocale = Locale(language.locale.language)
                speechEngine.setLanguage(fallbackLocale)
            }

            val utteranceId = if (isSource) "source" else "target"
            val params = Bundle()
            if (_isSplitEarbudMode.value) {
                // Left Earbud (pan = -1.0f): Source Language (what partner said, translated)
                // Right Earbud (pan = 1.0f): Target Language (what user said, translated)
                val panValue = if (isSource) -1.0f else 1.0f
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_PAN, panValue)
            }
            speechEngine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } catch (e: Exception) {
            Log.e("TranslationVM", "Speech playback error: ${e.message}")
            _errorMessage.value = "Failed to produce vocals: ${e.message}"
        }
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeakingSource.value = false
        _isSpeakingTarget.value = false
    }

    fun selectHistoryItem(item: TranslationEntity) {
        // Find corresponding language matches and populate text boxes to edit or speak again instantly
        val srcLang = languages.find { it.code == item.sourceLangCode } ?: languages[0]
        val targetLang = languages.find { it.code == item.targetLangCode } ?: languages[1]

        _sourceLanguage.value = srcLang
        _targetLanguage.value = targetLang
        _sourceText.value = item.originalText
        _translatedText.value = item.translatedText
        _errorMessage.value = null
    }

    fun toggleFavorite(item: TranslationEntity) {
        viewModelScope.launch {
            repository.updateFavorite(item.id, !item.isFavorite)
        }
    }

    fun deleteHistoryItem(item: TranslationEntity) {
        viewModelScope.launch {
            repository.deleteById(item.id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun onPermissionDenied() {
        _errorMessage.value = "Record Audio permission is required for voice capturing. You can still type text inputs directly."
    }

    override fun onCleared() {
        super.onCleared()
        autoTranslateJob?.cancel()
        speechRecognizer?.apply {
            destroy()
        }
        textToSpeech?.apply {
            stop()
            shutdown()
        }
    }
}
