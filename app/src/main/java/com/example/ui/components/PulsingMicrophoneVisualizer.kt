package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppColors.FacebookBlue
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visual sizes for the voice interaction orb
 */
enum class VoiceOrbSize(val buttonSize: Dp, val iconSize: Dp, val maxRippleRadius: Dp) {
    COMPACT(buttonSize = 48.dp, iconSize = 24.dp, maxRippleRadius = 80.dp),
    MEDIUM(buttonSize = 68.dp, iconSize = 32.dp, maxRippleRadius = 120.dp),
    LARGE(buttonSize = 84.dp, iconSize = 38.dp, maxRippleRadius = 150.dp),
    HERO(buttonSize = 108.dp, iconSize = 48.dp, maxRippleRadius = 190.dp)
}

/**
 * State mode for voice interaction
 */
enum class VoiceVisualizerState {
    IDLE,
    RECORDING,
    SPEAKING,
    PROCESSING
}

/**
 * Reusable Custom Compose Component for Voice Interaction with Pulsing Microphone Animation.
 * Reacts continuously to recording state, speaking state, and live volume/amplitude levels.
 *
 * @param isRecording Whether the microphone is actively recording/listening.
 * @param isSpeaking Whether TTS or voice output is currently playing.
 * @param isProcessing Whether AI is thinking/processing speech input.
 * @param volumeLevel Current audio volume/amplitude level normalized between 0.0f and 1.0f.
 * @param onClick Triggered when the user taps the mic orb.
 * @param modifier Custom modifier for styling and layout.
 * @param size Predefined or custom size preset for the orb.
 * @param primaryColor Main brand color when in idle mode.
 * @param recordingColor Vibrant pulse color when actively recording.
 * @param speakingColor Vibrant pulse color when AI is speaking.
 * @param showWaveformBars Whether to display animated frequency equalizer bars below or inside the orb.
 * @param showConcentricRings Whether to draw animated outward expanding acoustic ripple rings.
 * @param testTag Test identifier tag for automated UI testing.
 */
@Composable
fun PulsingMicrophoneVisualizer(
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    isSpeaking: Boolean = false,
    isProcessing: Boolean = false,
    volumeLevel: Float = 0f,
    onClick: () -> Unit = {},
    size: VoiceOrbSize = VoiceOrbSize.MEDIUM,
    primaryColor: Color = FacebookBlue,
    recordingColor: Color = Color(0xFFEF4444),
    speakingColor: Color = Color(0xFF10B981),
    processingColor: Color = Color(0xFF8B5CF6),
    showWaveformBars: Boolean = false,
    showConcentricRings: Boolean = true,
    statusText: String? = null,
    testTag: String = "pulsing_microphone_visualizer"
) {
    val clampedVolume = volumeLevel.coerceIn(0f, 1f)

    // Smooth volume transition using spring animation for natural physics
    val animatedVolume by animateFloatAsState(
        targetValue = clampedVolume,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "smooth_volume"
    )

    // Determine current visual state
    val visualState = remember(isRecording, isSpeaking, isProcessing) {
        when {
            isRecording -> VoiceVisualizerState.RECORDING
            isSpeaking -> VoiceVisualizerState.SPEAKING
            isProcessing -> VoiceVisualizerState.PROCESSING
            else -> VoiceVisualizerState.IDLE
        }
    }

    // Dynamic Active Colors
    val activeColor by animateColorAsState(
        targetValue = when (visualState) {
            VoiceVisualizerState.RECORDING -> recordingColor
            VoiceVisualizerState.SPEAKING -> speakingColor
            VoiceVisualizerState.PROCESSING -> processingColor
            VoiceVisualizerState.IDLE -> primaryColor
        },
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "orb_active_color"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")

    // Ambient breathing pulse for idle or baseline
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambient_pulse"
    )

    // Continuous rotation for processing state
    val processingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "processing_rotation"
    )

    // Acoustic ripple phase animations (3 staggered concentric waves)
    val ripplePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_1"
    )
    val ripplePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_2"
    )
    val ripplePhase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_3"
    )

    // Dynamic Scale reactive to audio amplitude
    val buttonScale = when (visualState) {
        VoiceVisualizerState.RECORDING -> 1.0f + (animatedVolume * 0.35f)
        VoiceVisualizerState.SPEAKING -> 1.0f + (animatedVolume * 0.20f).coerceAtLeast(0.05f)
        VoiceVisualizerState.PROCESSING -> 1.02f
        VoiceVisualizerState.IDLE -> ambientPulse
    }

    Column(
        modifier = modifier.testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(size.maxRippleRadius * 1.5f)
                .wrapContentSize(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            // Concentric Expanding Soundwave Ripples (Canvas)
            if (showConcentricRings && (isRecording || isSpeaking || isProcessing)) {
                Canvas(
                    modifier = Modifier.size(size.maxRippleRadius * 1.5f)
                ) {
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)
                    val baseRadius = size.buttonSize.toPx() / 2f
                    val maxRadius = size.maxRippleRadius.toPx()
                    val volumeBoost = (animatedVolume * 24.dp.toPx())

                    val phases = listOf(ripplePhase1, ripplePhase2, ripplePhase3)
                    phases.forEach { phase ->
                        val currentRadius = baseRadius + (maxRadius - baseRadius + volumeBoost) * phase
                        val alpha = ((1f - phase) * 0.45f).coerceIn(0f, 1f)

                        // Outer glowing aura
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activeColor.copy(alpha = alpha),
                                    activeColor.copy(alpha = alpha * 0.3f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = currentRadius
                            ),
                            radius = currentRadius,
                            center = center
                        )

                        // Crisply outlined acoustic wave
                        drawCircle(
                            color = activeColor.copy(alpha = alpha * 0.75f),
                            radius = currentRadius,
                            center = center,
                            style = Stroke(width = (2.dp.toPx() * (1f - phase * 0.5f)))
                        )
                    }
                }
            }

            // Processing Ring Shimmer
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .size(size.buttonSize + 16.dp)
                        .graphicsLayer { rotationZ = processingRotation }
                        .drawBehind {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        processingColor.copy(alpha = 0.8f),
                                        processingColor,
                                        Color.Transparent
                                    )
                                ),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                )
            }

            // Center Dynamic Glowing Orb Button
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = if (isRecording || isSpeaking) 12.dp else 6.dp,
                modifier = Modifier
                    .size(size.buttonSize)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .clickable { onClick() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activeColor.copy(alpha = 0.9f),
                                    activeColor
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconVector = when (visualState) {
                        VoiceVisualizerState.RECORDING -> Icons.Filled.Mic
                        VoiceVisualizerState.SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
                        VoiceVisualizerState.PROCESSING -> Icons.Filled.GraphicEq
                        VoiceVisualizerState.IDLE -> Icons.Filled.MicNone
                    }

                    Icon(
                        imageVector = iconVector,
                        contentDescription = when (visualState) {
                            VoiceVisualizerState.RECORDING -> "Recording active - tap to stop"
                            VoiceVisualizerState.SPEAKING -> "AI Speaking"
                            VoiceVisualizerState.PROCESSING -> "AI Processing"
                            VoiceVisualizerState.IDLE -> "Tap to start voice"
                        },
                        tint = Color.White,
                        modifier = Modifier.size(size.iconSize)
                    )
                }
            }
        }

        // Optional Live Equalizer Waveform Bars
        if (showWaveformBars) {
            Spacer(modifier = Modifier.height(8.dp))
            LiveWaveformBarVisualizer(
                isLive = isRecording || isSpeaking,
                volumeLevel = animatedVolume,
                activeColor = activeColor,
                modifier = Modifier.fillMaxWidth(0.7f).height(32.dp)
            )
        }

        // Optional Status subtitle
        if (!statusText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isRecording) recordingColor else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Live Animated Audio Equalizer Bars Reacting to Amplitude & Rhythm.
 */
@Composable
fun LiveWaveformBarVisualizer(
    isLive: Boolean,
    volumeLevel: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    activeColor: Color = FacebookBlue,
    idleColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val totalSpacing = width * 0.3f
        val barSpacing = totalSpacing / (barCount - 1).coerceAtLeast(1)
        val barWidth = (width - totalSpacing) / barCount

        val centerY = height / 2f
        val clampedVol = volumeLevel.coerceIn(0.05f, 1f)

        for (i in 0 until barCount) {
            val normalizedX = i.toFloat() / barCount.toFloat()
            // Harmonic wave modulation
            val wave = if (isLive) {
                val sinVal = sin(phase + (normalizedX * Math.PI * 3.5)).toFloat()
                val cosVal = cos((phase * 1.3) + (normalizedX * Math.PI * 2.0)).toFloat()
                (sinVal * 0.6f + cosVal * 0.4f).coerceIn(-1f, 1f)
            } else {
                0.15f
            }

            val amplitudeScale = if (isLive) (0.25f + clampedVol * 0.75f) else 0.15f
            val barHeight = (height * 0.85f * (0.2f + Math.abs(wave) * 0.8f) * amplitudeScale)
                .coerceIn(4.dp.toPx(), height)

            val x = i * (barWidth + barSpacing)
            val y = centerY - (barHeight / 2f)

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isLive) {
                        listOf(
                            activeColor.copy(alpha = 0.7f),
                            activeColor,
                            activeColor.copy(alpha = 0.7f)
                        )
                    } else {
                        listOf(idleColor, idleColor)
                    },
                    startY = y,
                    endY = y + barHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
