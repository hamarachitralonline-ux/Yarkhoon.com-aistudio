package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.AppColors.FacebookBlue
import com.example.data.Group
import com.example.data.KhowarDatasetEntry
import com.example.data.MarketplaceItem
import com.example.data.Post
import com.example.data.ServiceListing
import com.example.data.User
import com.example.ui.SocialMediaViewModel
import com.example.ui.components.PulsingMicrophoneVisualizer
import com.example.ui.components.VoiceOrbSize
import com.example.voice.*

enum class VoiceAssistantTab {
    LIVE_VOICE,
    VOICE_SEARCH,
    TRANSLATE,
    KHOWAR_DATASET
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceLiveScreen(
    viewModel: SocialMediaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val voiceManager = viewModel.voiceAssistantManager

    val selectedLanguage by voiceManager.selectedLanguage.collectAsState()
    val isListening by voiceManager.isListening.collectAsState()
    val isSpeaking by voiceManager.isSpeaking.collectAsState()
    val isProcessing by voiceManager.isProcessing.collectAsState()
    val isContinuousConversation by voiceManager.isContinuousConversation.collectAsState()
    val isRecordingAudio by voiceManager.isRecordingAudio.collectAsState()
    val recordingDurationSec by voiceManager.recordingDurationSec.collectAsState()
    val isAudioPlaying by voiceManager.isAudioPlaying.collectAsState()
    val activePlayingUrl by voiceManager.activePlayingUrl.collectAsState()
    val liveTranscriptText by voiceManager.liveTranscriptText.collectAsState()
    val audioAmplitude by voiceManager.audioAmplitude.collectAsState()
    val statusMessage by voiceManager.statusMessage.collectAsState()
    val conversationMessages by voiceManager.conversationMessages.collectAsState()

    val isLiveVoiceActive by viewModel.isLiveVoiceActive.collectAsState()
    val voiceSearchResult by viewModel.voiceSearchResult.collectAsState()
    val isVoiceSearching by viewModel.isVoiceSearching.collectAsState()
    val voiceTranslationResult by viewModel.voiceTranslationResult.collectAsState()
    val isVoiceTranslating by viewModel.isVoiceTranslating.collectAsState()
    val khowarDataset by viewModel.allKhowarDatasetEntries.collectAsState()

    var activeTab by remember { mutableStateOf(VoiceAssistantTab.LIVE_VOICE) }
    var textSearchInput by remember { mutableStateOf("") }
    var translationInputText by remember { mutableStateOf("") }
    var translationSourceLang by remember { mutableStateOf(VoiceLanguage.KHOWAR) }
    var translationTargetLang by remember { mutableStateOf(VoiceLanguage.ENGLISH) }

    var pendingAudioAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingAudioAction?.invoke()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is needed to talk with Yarkhoon AI Voice Assistant",
                Toast.LENGTH_SHORT
            ).show()
        }
        pendingAudioAction = null
    }

    fun executeWithAudioPermission(action: () -> Unit) {
        val hasAudioPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPerm) {
            action()
        } else {
            pendingAudioAction = action
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Dataset contribution dialog state
    var showContributeDialog by remember { mutableStateOf(false) }
    var newKhowarText by remember { mutableStateOf("") }
    var newRomanText by remember { mutableStateOf("") }
    var newUrduText by remember { mutableStateOf("") }
    var newEnglishText by remember { mutableStateOf("") }
    var newDialect by remember { mutableStateOf("Yarkhoon / Upper Chitral") }
    var newCategory by remember { mutableStateOf("GREETING") }
    var recordedAudioPath by remember { mutableStateOf("") }
    var recordedDurationMs by remember { mutableLongStateOf(0L) }

    // Visual pulse animation based on amplitude and listening state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient"
    )
    val effectiveScale = if (isListening) 1.0f + (audioAmplitude * 0.45f) else if (isLiveVoiceActive) ambientPulse else 1.0f

    val quickSpokenPrompts = remember(selectedLanguage) {
        when (selectedLanguage) {
            VoiceLanguage.KHOWAR -> listOf(
                "یارخونو سڑک حال کیا شیر؟",
                "کھوارو مژی شستہ الفاظ مہ لوزاوے",
                "بونی اور مستوجو بارا تیکی معلومات دے",
                "بروغل فیسٹیول کورا بیران؟"
            )
            VoiceLanguage.URDU -> listOf(
                "یارخون میں موسم کا کیا حال ہے؟",
                "مجھے چترال کی روایتی موسیقی کے بارے میں بتائیں",
                "کھوار زبان کے بنیادی الفاظ سکھائیں",
                "پوسٹس اور گروپس میں سرچ کریں"
            )
            VoiceLanguage.ENGLISH -> listOf(
                "What's the weather and road status in Yarkhoon Valley?",
                "Teach me 5 essential Khowar greetings and phrases",
                "Search posts about Chitral cultural festivals",
                "Translate 'Welcome to Yarkhoon' into Khowar"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Yarkhoon AI Voice",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isLiveVoiceActive || isListening) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isLiveVoiceActive || isListening) Color(0xFF10B981) else Color.Gray)
                                    )
                                    Text(
                                        if (isListening) "LISTENING" else if (isLiveVoiceActive) "ACTIVE" else "READY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLiveVoiceActive || isListening) Color(0xFF10B981) else Color.Gray
                                    )
                                }
                            }
                        }
                        Text(
                            "Khowar • Urdu • English AI Assistant",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Export dataset button
                    IconButton(
                        onClick = {
                            viewModel.exportKhowarDatasetJsonl(context) { file ->
                                if (file != null) {
                                    Toast.makeText(context, "Dataset exported to ${file.name}", Toast.LENGTH_LONG).show()
                                    try {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Khowar Dataset"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export saved: ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Export error", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = "Export Khowar Dataset", tint = FacebookBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Language Selector Pills Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Spoken Language:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VoiceLanguage.values().forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                voiceManager.setLanguage(lang, lang.locale)
                                Toast.makeText(context, "${lang.flagEmoji} ${lang.displayName} (${lang.locale.toLanguageTag()})", Toast.LENGTH_SHORT).show()
                            },
                            label = {
                                Text(
                                    "${lang.flagEmoji} ${lang.displayName} [${lang.locale.toLanguageTag()}]",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FacebookBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Tabs Selector
            TabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = FacebookBlue
            ) {
                Tab(
                    selected = activeTab == VoiceAssistantTab.LIVE_VOICE,
                    onClick = { activeTab = VoiceAssistantTab.LIVE_VOICE },
                    text = { Text("Live Voice", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == VoiceAssistantTab.VOICE_SEARCH,
                    onClick = { activeTab = VoiceAssistantTab.VOICE_SEARCH },
                    text = { Text("Voice Search", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == VoiceAssistantTab.TRANSLATE,
                    onClick = { activeTab = VoiceAssistantTab.TRANSLATE },
                    text = { Text("Translate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.Translate, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = activeTab == VoiceAssistantTab.KHOWAR_DATASET,
                    onClick = { activeTab = VoiceAssistantTab.KHOWAR_DATASET },
                    text = { Text("Khowar Dataset", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Filled.LibraryBooks, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Tab Content
            when (activeTab) {
                VoiceAssistantTab.LIVE_VOICE -> {
                    LiveVoiceAssistantContent(
                        viewModel = viewModel,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        isProcessing = isProcessing,
                        isContinuous = isContinuousConversation,
                        onToggleContinuous = { voiceManager.setContinuousConversation(!isContinuousConversation) },
                        audioAmplitude = audioAmplitude,
                        statusMessage = statusMessage,
                        liveTranscriptText = liveTranscriptText,
                        selectedLanguage = selectedLanguage,
                        quickPrompts = quickSpokenPrompts,
                        onToggleListening = {
                            if (isListening) {
                                voiceManager.stopListening()
                            } else {
                                executeWithAudioPermission {
                                    voiceManager.startListening(
                                        lang = selectedLanguage,
                                        locale = selectedLanguage.locale,
                                        onFinalResult = { speech ->
                                            if (speech.isNotBlank()) {
                                                viewModel.submitLiveVoiceTurn(speech)
                                            }
                                        }
                                    )
                                }
                            }
                        },
                        onSelectQuickPrompt = { prompt ->
                            viewModel.submitLiveVoiceTurn(prompt)
                        }
                    )
                }
                VoiceAssistantTab.VOICE_SEARCH -> {
                    VoiceSearchContent(
                        viewModel = viewModel,
                        isSearching = isVoiceSearching,
                        searchResult = voiceSearchResult,
                        isListening = isListening,
                        onStartVoiceSearch = {
                            executeWithAudioPermission {
                                voiceManager.startListening(
                                    lang = selectedLanguage,
                                    locale = selectedLanguage.locale,
                                    onFinalResult = { query ->
                                        if (query.isNotBlank()) {
                                            viewModel.executeUniversalVoiceSearch(query)
                                        }
                                    }
                                )
                            }
                        },
                        onManualSearch = { q ->
                            viewModel.executeUniversalVoiceSearch(q)
                        }
                    )
                }
                VoiceAssistantTab.TRANSLATE -> {
                    VoiceTranslateContent(
                        viewModel = viewModel,
                        translationResult = voiceTranslationResult,
                        isTranslating = isVoiceTranslating,
                        isListening = isListening,
                        sourceLang = translationSourceLang,
                        targetLang = translationTargetLang,
                        onSourceLangChange = { translationSourceLang = it },
                        onTargetLangChange = { translationTargetLang = it },
                        onSwapLanguages = {
                            val temp = translationSourceLang
                            translationSourceLang = translationTargetLang
                            translationTargetLang = temp
                        },
                        onStartVoiceTranslation = {
                            executeWithAudioPermission {
                                voiceManager.startListening(
                                    lang = translationSourceLang,
                                    locale = translationSourceLang.locale,
                                    onFinalResult = { text ->
                                        if (text.isNotBlank()) {
                                            translationInputText = text
                                            viewModel.executeVoiceTranslation(text, translationSourceLang, translationTargetLang)
                                        }
                                    }
                                )
                            }
                        },
                        onTranslateText = { text ->
                            viewModel.executeVoiceTranslation(text, translationSourceLang, translationTargetLang)
                        }
                    )
                }
                VoiceAssistantTab.KHOWAR_DATASET -> {
                    KhowarDatasetContent(
                        viewModel = viewModel,
                        datasetEntries = khowarDataset,
                        onOpenContributeDialog = { showContributeDialog = true },
                        onVote = { entry -> viewModel.voteKhowarDatasetEntry(entry) },
                        onDelete = { entryId -> viewModel.deleteKhowarDatasetEntry(entryId) },
                        onSpeak = { text, lang -> voiceManager.speakText(text, lang) }
                    )
                }
            }
        }
    }

    // Contribute Khowar Phrase & Audio Recording Dialog
    if (showContributeDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isRecordingAudio) {
                    showContributeDialog = false
                    voiceManager.cancelAudioRecording()
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = FacebookBlue)
                    Text("Contribute Khowar Data", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "Help enrich the Khowar AI model by contributing authentic Chitrali phrases and audio recordings for training & fine-tuning.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = newKhowarText,
                            onValueChange = { newKhowarText = it },
                            label = { Text("Khowar Text (کھوار رسم الخط)") },
                            placeholder = { Text("مثال: تہ کیا حال شیر؟") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = newRomanText,
                            onValueChange = { newRomanText = it },
                            label = { Text("Roman Khowar (Optional)") },
                            placeholder = { Text("e.g., Ta kya haal sher?") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = newUrduText,
                            onValueChange = { newUrduText = it },
                            label = { Text("Urdu Meaning (اردو ترجمہ)") },
                            placeholder = { Text("آپ کا کیا حال ہے؟") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = newEnglishText,
                            onValueChange = { newEnglishText = it },
                            label = { Text("English Meaning") },
                            placeholder = { Text("How are you?") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        // Dialect selection
                        Text("Dialect / Region:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Yarkhoon", "Mastuj", "Booni", "Chitral Town", "Drosh").forEach { d ->
                                FilterChip(
                                    selected = newDialect.startsWith(d),
                                    onClick = { newDialect = "$d / Upper Chitral" },
                                    label = { Text(d, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    item {
                        // Audio recording button for phonetic dataset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Audio Voice Sample (Optional)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (isRecordingAudio) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color.Red)
                                        )
                                        Text(
                                            "Recording: ${recordingDurationSec}s",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Red,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            voiceManager.stopAudioRecording { file, durationMs ->
                                                if (file != null) {
                                                    recordedAudioPath = file.absolutePath
                                                    recordedDurationMs = durationMs
                                                    Toast.makeText(context, "Audio recorded (${durationMs / 1000}s)", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) {
                                        Icon(Icons.Filled.Stop, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Stop Recording")
                                    }
                                } else {
                                    if (recordedAudioPath.isNotBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    voiceManager.playAudioFile(recordedAudioPath)
                                                }
                                            ) {
                                                Icon(
                                                    if (isAudioPlaying && activePlayingUrl == recordedAudioPath) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                                    contentDescription = null,
                                                    tint = FacebookBlue
                                                )
                                            }
                                            Text("Audio ready (${recordedDurationMs / 1000}s)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            IconButton(onClick = { recordedAudioPath = "" }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Clear", tint = Color.Red, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                executeWithAudioPermission {
                                                    voiceManager.startAudioRecording()
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Filled.Mic, contentDescription = null, tint = FacebookBlue)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Record Pronunciation")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKhowarText.isNotBlank() && (newUrduText.isNotBlank() || newEnglishText.isNotBlank())) {
                            viewModel.addKhowarDatasetContribution(
                                khowarText = newKhowarText,
                                khowarRoman = newRomanText,
                                urduTranslation = newUrduText,
                                englishTranslation = newEnglishText,
                                audioFilePath = recordedAudioPath,
                                audioDurationMs = recordedDurationMs,
                                dialect = newDialect,
                                category = newCategory
                            )
                            Toast.makeText(context, "Shukriya! Khowar phrase submitted.", Toast.LENGTH_SHORT).show()
                            showContributeDialog = false
                            newKhowarText = ""
                            newRomanText = ""
                            newUrduText = ""
                            newEnglishText = ""
                            recordedAudioPath = ""
                        } else {
                            Toast.makeText(context, "Please enter Khowar text and translation", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue)
                ) {
                    Text("Submit Phrase")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showContributeDialog = false
                        voiceManager.cancelAudioRecording()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LiveVoiceAssistantContent(
    viewModel: SocialMediaViewModel,
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean = false,
    isContinuous: Boolean = true,
    onToggleContinuous: () -> Unit = {},
    audioAmplitude: Float,
    statusMessage: String,
    liveTranscriptText: String,
    selectedLanguage: VoiceLanguage,
    quickPrompts: List<String>,
    onToggleListening: () -> Unit,
    onSelectQuickPrompt: (String) -> Unit
) {
    val transcript = viewModel.liveVoiceTranscript.collectAsState().value
    val listState = rememberLazyListState()

    LaunchedEffect(transcript.size) {
        if (transcript.isNotEmpty()) {
            listState.animateScrollToItem(transcript.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Continuous back-and-forth toggle pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Filled.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isContinuous) FacebookBlue else MaterialTheme.colorScheme.outline
                )
                Text(
                    "Continuous Conversation",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Switch(
                checked = isContinuous,
                onCheckedChange = { onToggleContinuous() },
                modifier = Modifier.scale(0.8f).testTag("continuous_conversation_switch")
            )
        }

        // Quick Speech-to-Text Language & Locale selector row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VoiceLanguage.values().forEach { lang ->
                val isSelected = selectedLanguage == lang
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.setVoiceLanguage(lang, lang.locale)
                    },
                    label = {
                        Text(
                            "${lang.flagEmoji} ${lang.displayName} (${lang.locale.toLanguageTag()})",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.padding(horizontal = 3.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FacebookBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Reusable Pulsing Microphone Visualizer with dynamic soundwave ripple physics & waveform bars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            PulsingMicrophoneVisualizer(
                isRecording = isListening,
                isSpeaking = isSpeaking,
                isProcessing = isProcessing,
                volumeLevel = audioAmplitude,
                size = VoiceOrbSize.LARGE,
                showWaveformBars = true,
                showConcentricRings = true,
                onClick = onToggleListening,
                testTag = "voice_assistant_mic_orb"
            )
        }

        // Live status banner
        Text(
            text = if (isListening && liveTranscriptText.isNotBlank()) "\"$liveTranscriptText\"" else statusMessage,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isListening) FacebookBlue else if (isProcessing) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick suggestions row
        Text(
            "Try asking in ${selectedLanguage.nativeName}:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickPrompts) { prompt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.clickable { onSelectQuickPrompt(prompt) }
                ) {
                    Text(
                        prompt,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Conversation transcript list
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            if (transcript.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = FacebookBlue.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        Text(
                            "Tap the microphone orb to speak in\n${selectedLanguage.displayName} (${selectedLanguage.nativeName})",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transcript) { (speaker, text) ->
                        val isUser = speaker == "You"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                ),
                                color = if (isUser) FacebookBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        speaker,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUser) Color.White.copy(alpha = 0.8f) else FacebookBlue
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text,
                                        fontSize = 14.sp,
                                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Optional Quick Text-to-Dialogue Bar (fallback if device microphone or speech recognizer service is unavailable)
        var chatInput by remember { mutableStateOf("") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                placeholder = { Text("Or type a prompt in ${selectedLanguage.nativeName}...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("voice_chat_text_fallback_input"),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )
            FilledIconButton(
                onClick = {
                    if (chatInput.isNotBlank()) {
                        val text = chatInput.trim()
                        chatInput = ""
                        viewModel.submitLiveVoiceTurn(text)
                    }
                },
                enabled = chatInput.isNotBlank() && !isProcessing,
                modifier = Modifier.size(44.dp).testTag("voice_chat_send_button"),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = FacebookBlue)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun VoiceSearchContent(
    viewModel: SocialMediaViewModel,
    isSearching: Boolean,
    searchResult: VoiceSearchResult?,
    isListening: Boolean,
    onStartVoiceSearch: () -> Unit,
    onManualSearch: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search Input Bar with Voice Mic Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Voice search posts, groups, users...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onManualSearch(searchQuery) }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Search", tint = FacebookBlue)
                        }
                    }
                }
            )

            // Pulsing Mic Visualizer for voice search
            PulsingMicrophoneVisualizer(
                isRecording = isListening,
                volumeLevel = if (isListening) 0.65f else 0f,
                size = VoiceOrbSize.COMPACT,
                showConcentricRings = true,
                onClick = onStartVoiceSearch,
                testTag = "voice_search_mic_button"
            )
        }

        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(color = FacebookBlue)
                    Text("Searching across Yarkhoon ecosystem...", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else if (searchResult != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Spoken AI Summary
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FacebookBlue.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FacebookBlue.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = FacebookBlue, modifier = Modifier.size(16.dp))
                                Text("Voice Search Summary", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FacebookBlue)
                            }
                            Text(searchResult.summarySpeech, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Matched Posts
                if (searchResult.matchedPosts.isNotEmpty()) {
                    item {
                        Text("Matched Posts (${searchResult.matchedPosts.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    items(searchResult.matchedPosts) { post ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(post.content, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // Matched Groups
                if (searchResult.matchedGroups.isNotEmpty()) {
                    item {
                        Text("Matched Groups (${searchResult.matchedGroups.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    items(searchResult.matchedGroups) { group ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Groups, contentDescription = null, tint = FacebookBlue)
                                Column {
                                    Text(group.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${group.memberCount} members • ${group.location}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                // Matched Users
                if (searchResult.matchedUsers.isNotEmpty()) {
                    item {
                        Text("Matched Users (${searchResult.matchedUsers.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    items(searchResult.matchedUsers) { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = FacebookBlue)
                                Column {
                                    Text(user.fullName.ifBlank { user.username }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("@${user.username} • ${user.bio}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.MicNone, contentDescription = null, tint = FacebookBlue.copy(alpha = 0.5f), modifier = Modifier.size(56.dp))
                    Text(
                        "Tap the mic to search posts, groups,\nmarketplace items, and valley people by voice.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceTranslateContent(
    viewModel: SocialMediaViewModel,
    translationResult: VoiceTranslationResult?,
    isTranslating: Boolean,
    isListening: Boolean,
    sourceLang: VoiceLanguage,
    targetLang: VoiceLanguage,
    onSourceLangChange: (VoiceLanguage) -> Unit,
    onTargetLangChange: (VoiceLanguage) -> Unit,
    onSwapLanguages: () -> Unit,
    onStartVoiceTranslation: () -> Unit,
    onTranslateText: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Language Selector Row with Swap Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = FacebookBlue.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("From", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(sourceLang.nativeName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FacebookBlue)
                }
            }

            IconButton(onClick = onSwapLanguages) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap Languages", tint = FacebookBlue)
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = FacebookBlue.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("To", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    Text(targetLang.nativeName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FacebookBlue)
                }
            }
        }

        // Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Speak or type phrase in ${sourceLang.nativeName}...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Mic Button
                    Button(
                        onClick = onStartVoiceTranslation,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isListening) Color(0xFFEF4444) else FacebookBlue)
                    ) {
                        Icon(if (isListening) Icons.Filled.Mic else Icons.Filled.MicNone, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isListening) "Listening..." else "Speak to Translate")
                    }

                    // Translate text button
                    if (textInput.isNotBlank()) {
                        IconButton(onClick = { onTranslateText(textInput) }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Translate", tint = FacebookBlue)
                        }
                    }
                }
            }
        }

        // Output Result Card
        if (isTranslating) {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CircularProgressIndicator(color = FacebookBlue)
                    Text("Translating with Khowar Linguistic Engine...", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else if (translationResult != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FacebookBlue.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, FacebookBlue.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Translation (${translationResult.targetLanguage.displayName}):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = FacebookBlue
                        )
                        Row {
                            IconButton(onClick = {
                                viewModel.voiceAssistantManager.speakText(translationResult.translatedText, translationResult.targetLanguage)
                            }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = FacebookBlue)
                            }
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(translationResult.translatedText))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = FacebookBlue)
                            }
                        }
                    }

                    Text(
                        translationResult.translatedText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (translationResult.phoneticRoman.isNotBlank()) {
                        Text(
                            "Pronunciation: ${translationResult.phoneticRoman}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (translationResult.sourceOriginal.isNotBlank()) {
                        Text(
                            "Original: \"${translationResult.sourceOriginal}\"",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KhowarDatasetContent(
    viewModel: SocialMediaViewModel,
    datasetEntries: List<KhowarDatasetEntry>,
    onOpenContributeDialog: () -> Unit,
    onVote: (KhowarDatasetEntry) -> Unit,
    onDelete: (String) -> Unit,
    onSpeak: (String, VoiceLanguage) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredEntries = remember(datasetEntries, searchQuery) {
        if (searchQuery.isBlank()) datasetEntries
        else datasetEntries.filter {
            it.khowarText.contains(searchQuery, true) ||
            it.khowarRomanText.contains(searchQuery, true) ||
            it.urduTranslation.contains(searchQuery, true) ||
            it.englishTranslation.contains(searchQuery, true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header info & Contribute Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Khowar Linguistic Dataset", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${datasetEntries.size} curated phrases for AI fine-tuning", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }

            Button(
                onClick = onOpenContributeDialog,
                colors = ButtonDefaults.buttonColors(containerColor = FacebookBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Contribute", fontSize = 12.sp)
            }
        }

        // Search Filter
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search phrases in Khowar, Urdu or English...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
        )

        // Dataset Entries List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredEntries) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = FacebookBlue.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    entry.dialect,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FacebookBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onSpeak(entry.khowarText, VoiceLanguage.KHOWAR) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = FacebookBlue, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onVote(entry) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.ThumbUp, contentDescription = "Vote", tint = FacebookBlue, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text("${entry.votesCount}", fontSize = 11.sp, color = FacebookBlue)
                                    }
                                }
                            }
                        }

                        // Khowar Text
                        Text(
                            entry.khowarText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (entry.khowarRomanText.isNotBlank()) {
                            Text("Roman: ${entry.khowarRomanText}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }

                        // Translations
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (entry.urduTranslation.isNotBlank()) {
                                Text("اردو: ${entry.urduTranslation}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (entry.englishTranslation.isNotBlank()) {
                                Text("EN: ${entry.englishTranslation}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
