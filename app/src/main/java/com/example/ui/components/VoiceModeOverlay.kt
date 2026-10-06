package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BleBlue
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.IndustrialRed
import com.example.ui.theme.IndustrialYellow
import com.example.ui.theme.StatusConnected
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.HapticManager
import com.example.voice.VoiceState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceModeOverlay(
    voiceState: VoiceState,
    audioRmsDb: Float,
    lastVoiceFeedback: String?,
    onToggleListening: () -> Unit,
    onPhraseSimulate: (String) -> Unit,
    onLaunchSpeechIntent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }
    val isListening = voiceState is VoiceState.Listening
    var typedCommand by remember { mutableStateOf("") }

    // Pulsing Mic Animation
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.95f))
            .testTag("voice_mode_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, BleBlue.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = BleBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "VOICE COMMAND CONTROL",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = BleBlue,
                            letterSpacing = 1.sp
                        )
                    )
                }

                // Voice status label
                Text(
                    text = when (voiceState) {
                        is VoiceState.Listening -> "● LISTENING (Speak now)…"
                        is VoiceState.Processing -> "PROCESSING AUDIO…"
                        is VoiceState.Recognized -> "✓ COMMAND SENT"
                        is VoiceState.Error -> "RECOGNIZER PAUSED"
                        else -> "TAP MIC TO SPEAK"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (voiceState) {
                            is VoiceState.Listening -> StatusConnected
                            is VoiceState.Recognized -> IndustrialYellow
                            is VoiceState.Error -> IndustrialAmber
                            else -> TextSecondary
                        }
                    )
                )
            }

            // Error or Informative Message Banner if any error occurred
            if (voiceState is VoiceState.Error) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(IndustrialAmber.copy(alpha = 0.15f))
                        .border(1.dp, IndustrialAmber, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = IndustrialAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = voiceState.message,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = IndustrialAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    // Fallback button to launch system speech dialog
                    Button(
                        onClick = {
                            hapticManager.vibrateClick()
                            onLaunchSpeechIntent()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IndustrialAmber,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("LAUNCH SYSTEM MIC", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Main Central Interactive Zone: Pulsing Mic & Live Transcription + Manual Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT: Prominent Central Microphone Graphic Button & Audio Meter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(170.dp)
                ) {
                    Box(
                        modifier = Modifier.size(130.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Ripple Halo
                        if (isListening) {
                            Box(
                                modifier = Modifier
                                    .size(125.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(BleBlue.copy(alpha = 0.25f))
                            )
                        }

                        // Main Microphone Button
                        Box(
                            modifier = Modifier
                                .size(95.dp)
                                .shadow(12.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = if (isListening) listOf(BleBlue, DarkBackground)
                                        else listOf(DarkSurfaceVariant, DarkBackground)
                                    )
                                )
                                .border(
                                    width = 3.dp,
                                    color = if (isListening) BleBlue else DarkSurfaceBorder,
                                    shape = CircleShape
                                )
                                .clickable {
                                    hapticManager.vibrateClick()
                                    onToggleListening()
                                }
                                .testTag("voice_mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Voice Control",
                                tint = if (isListening) TextPrimary else TextMuted,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isListening) "LISTENING • TAP TO STOP" else "TAP MIC TO SPEAK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isListening) BleBlue else TextMuted,
                            fontSize = 10.sp
                        )
                    )
                }

                // RIGHT: Speech Output, Feedback, Phrases & Quick Text Input
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Feedback Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LATEST RECOGNIZED TELEMETRY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                )
                                Text(
                                    text = lastVoiceFeedback ?: "Awaiting spoken command…",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (lastVoiceFeedback != null) IndustrialYellow else TextSecondary
                                    )
                                )
                            }

                            if (isListening) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = BleBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${audioRmsDb.toInt()} dB",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = BleBlue,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Text Command Input (guarantees voice control works even without functional mic hardware)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = typedCommand,
                            onValueChange = { typedCommand = it },
                            placeholder = { Text("Type voice command (e.g. forward, stop, dig)...", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (typedCommand.isNotBlank()) {
                                    hapticManager.vibrateClick()
                                    onPhraseSimulate(typedCommand)
                                    typedCommand = ""
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BleBlue,
                                unfocusedBorderColor = DarkSurfaceBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = BleBlue,
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("voice_text_input")
                        )

                        Button(
                            onClick = {
                                if (typedCommand.isNotBlank()) {
                                    hapticManager.vibrateClick()
                                    onPhraseSimulate(typedCommand)
                                    typedCommand = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BleBlue,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("voice_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RUN", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Structural Voice Phrases (Instant tap to dispatch)
                    Text(
                        text = "REGISTERED STRUCTURAL PHRASES (Tap phrase to test or dispatch):",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        VoiceCommandChip("move forward", "RV:1", onPhraseSimulate)
                        VoiceCommandChip("reverse", "RV:2", onPhraseSimulate)
                        VoiceCommandChip("turn left", "RV:3", onPhraseSimulate)
                        VoiceCommandChip("turn right", "RV:4", onPhraseSimulate)
                        VoiceCommandChip("stop", "RV:0,BS:0,BM:0", onPhraseSimulate)
                        VoiceCommandChip("dig", "AUTO:1", onPhraseSimulate)
                        VoiceCommandChip("confirm", "CONFIRM:1", onPhraseSimulate)
                    }
                }
            }

            // Footer note with System Speech Dialog launcher option
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Android SpeechRecognizer active • Phrases: forward, reverse, left, right, stop, dig, confirm",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                )

                Text(
                    text = "System Speech Dialog",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BleBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier
                        .clickable {
                            hapticManager.vibrateClick()
                            onLaunchSpeechIntent()
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceCommandChip(
    phrase: String,
    payload: String,
    onClick: (String) -> Unit
) {
    val context = LocalContext.current
    val hapticManager = remember { HapticManager.getInstance(context) }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, BleBlue.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .clickable {
                hapticManager.vibrateClick()
                onClick(phrase)
            }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "\"$phrase\"",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 10.sp
            )
        )
        Text(
            text = "→ $payload",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = BleBlue,
                fontSize = 9.sp
            )
        )
    }
}
