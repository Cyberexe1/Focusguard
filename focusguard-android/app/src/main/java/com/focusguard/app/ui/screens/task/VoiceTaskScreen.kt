package com.focusguard.app.ui.screens.task

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme
import androidx.compose.runtime.collectAsState

@Composable
fun VoiceTaskScreen(
    onNavigateBack: () -> Unit,
    onTaskCaptured: () -> Unit,
    viewModel: AddTaskViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Navigate away once task is saved
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onTaskCaptured()
    }

    var transcript by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isCaptured by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Tap the mic to start") }

    // Pulse animation rings
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseOut), RepeatMode.Restart),
        label = "ring1",
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2000, 700, easing = EaseOut), RepeatMode.Restart),
        label = "ring2",
    )
    val ring3Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2000, 1400, easing = EaseOut), RepeatMode.Restart),
        label = "ring3",
    )
    val ringAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseOut), RepeatMode.Restart),
        label = "alpha1",
    )
    val ringAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, 700, easing = EaseOut), RepeatMode.Restart),
        label = "alpha2",
    )
    val ringAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2000, 1400, easing = EaseOut), RepeatMode.Restart),
        label = "alpha3",
    )

    // Mic permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening(context, onResult = { text ->
            transcript = text
            isListening = false
            statusText = "Task Captured!"
            isCaptured = true
        }, onError = {
            isListening = false
            statusText = "Could not understand. Tap to retry."
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = FgGradients.backgroundBrush),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = colors.primary)
            }
            FgText(text = "Voice Task Capture", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
            IconButton(onClick = { }) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = colors.onSurfaceVariant)
            }
        }

        // Center content
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Mic button with pulse rings
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isListening) {
                    // Animated rings
                    listOf(ring1Scale to ringAlpha1, ring2Scale to ringAlpha2, ring3Scale to ringAlpha3).forEach { (scale, alpha) ->
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .scale(scale)
                                .border(
                                    2.dp,
                                    colors.primary.copy(alpha = alpha.coerceIn(0f, 1f)),
                                    CircleShape,
                                ),
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        if (!isCaptured) {
                            if (!isListening) {
                                isListening = true
                                statusText = "Listening..."
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier.size(120.dp),
                    containerColor = if (isCaptured) colors.inversePrimary else colors.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp),
                ) {
                    Icon(
                        imageVector = if (isCaptured) Icons.Filled.Check else Icons.Filled.Mic,
                        contentDescription = "Mic",
                        modifier = Modifier.size(52.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            FgText(
                text = statusText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCaptured) colors.primary else Color.White,
            )

            Spacer(Modifier.height(20.dp))

            // Transcript card
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .background(
                        color = colors.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(20.dp)
                    .widthIn(max = 360.dp)
                    .defaultMinSize(minHeight = 90.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                FgText(
                    text = transcript.ifEmpty { "Your speech will appear here..." },
                    fontSize = 15.sp,
                    color = if (transcript.isEmpty()) colors.onSurfaceVariant else colors.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Psychology, null, tint = colors.primary, modifier = Modifier.size(14.dp))
                FgText(text = "FocusGuard AI analyzing context...", fontSize = 12.sp, color = colors.outline)
            }
        }

        // Bottom action buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF0F0A1E).copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurfaceVariant),
            ) {
                FgText(text = "Cancel", color = colors.onSurfaceVariant, fontSize = 15.sp)
            }
            Button(
                onClick = {
                    if (isCaptured && transcript.isNotBlank()) {
                        viewModel.onRawTextChange(transcript)
                        viewModel.analyzeTask()
                    }
                },
                modifier = Modifier
                    .weight(2f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                enabled = (isCaptured || isListening) && !uiState.isAnalyzing,
            ) {
                if (uiState.isAnalyzing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    FgText(text = if (isCaptured) "Save Task" else "Done", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun startListening(
    context: android.content.Context,
    onResult: (String) -> Unit,
    onError: () -> Unit,
) {
    val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }
    speechRecognizer.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) { onError(); speechRecognizer.destroy() }
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            onResult(text)
            speechRecognizer.destroy()
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotEmpty()) onResult(text)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    speechRecognizer.startListening(intent)
}
