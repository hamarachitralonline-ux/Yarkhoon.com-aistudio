package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AppColors.FacebookBlue
import com.example.ui.SocialMediaViewModel
import com.example.voice.VoiceAssistantMode
import com.example.voice.VoiceLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantBottomSheet(
    viewModel: SocialMediaViewModel,
    onDismiss: () -> Unit,
    onOpenFullScreen: () -> Unit
) {
    val context = LocalContext.current
    val voiceManager = viewModel.voiceAssistantManager
    val isListening by voiceManager.isListening.collectAsState()
    val isSpeaking by voiceManager.isSpeaking.collectAsState()
    val isProcessing by voiceManager.isProcessing.collectAsState()
    val selectedLanguage by voiceManager.selectedLanguage.collectAsState()
    val liveTranscriptText by voiceManager.liveTranscriptText.collectAsState()
    val statusMessage by voiceManager.statusMessage.collectAsState()
    val audioAmplitude by voiceManager.audioAmplitude.collectAsState()
    val transcript = viewModel.liveVoiceTranscript.collectAsState().value

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
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

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_bs")
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_bs"
    )
    val effectiveScale = if (isListening) 1.0f + (audioAmplitude * 0.45f) else ambientPulse

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with language pill and full screen button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Yarkhoon AI Voice", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FacebookBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            selectedLanguage.nativeName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FacebookBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onOpenFullScreen) {
                        Icon(Icons.Filled.OpenInFull, contentDescription = "Full Screen", tint = FacebookBlue)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            }

            // Reusable Pulsing Microphone Visualizer with live acoustic waves & waveform bars
            PulsingMicrophoneVisualizer(
                isRecording = isListening,
                isSpeaking = isSpeaking,
                isProcessing = isProcessing,
                volumeLevel = audioAmplitude,
                size = VoiceOrbSize.MEDIUM,
                showWaveformBars = true,
                showConcentricRings = true,
                onClick = {
                    if (isListening) {
                        voiceManager.stopListening()
                    } else {
                        val hasAudioPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasAudioPerm) {
                            voiceManager.startListening(
                                lang = selectedLanguage,
                                locale = selectedLanguage.locale,
                                onFinalResult = { speech ->
                                    if (speech.isNotBlank()) {
                                        viewModel.submitLiveVoiceTurn(speech)
                                    }
                                }
                            )
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                testTag = "voice_bottom_sheet_mic_orb",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            // Status message
            Text(
                text = if (isListening && liveTranscriptText.isNotBlank()) "\"$liveTranscriptText\"" else statusMessage,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isListening) FacebookBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Language Switcher Chips (English, Khowar, Urdu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VoiceLanguage.values().forEach { lang ->
                    val isSelected = selectedLanguage == lang
                    FilterChip(
                        selected = isSelected,
                        onClick = { voiceManager.setLanguage(lang, lang.locale) },
                        label = {
                            Text(
                                "${lang.flagEmoji} ${lang.displayName} (${lang.locale.toLanguageTag()})",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.padding(horizontal = 4.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FacebookBlue,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Recent Transcript Snippet
            if (transcript.isNotEmpty()) {
                val lastItem = transcript.last()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(lastItem.first, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FacebookBlue)
                        Text(lastItem.second, fontSize = 12.sp, maxLines = 3)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
