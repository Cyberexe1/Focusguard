package com.focusguard.app.ui.screens.schedule

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.theme.FocusGuardTheme
import kotlinx.coroutines.delay

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState

@Composable
fun FocusSprintScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onEmergency: () -> Unit,
    viewModel: FocusSprintViewModel = hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    // Auto-start sprint when screen opens
    LaunchedEffect(Unit) {
        if (uiState.sprint == null && !uiState.isStarting) {
            viewModel.startSprint()
        }
    }

    // Navigate away if ended
    LaunchedEffect(uiState.isEnded) {
        if (uiState.isEnded) onNavigateBack()
    }

    // Escalate to emergency if no progress reported
    LaunchedEffect(uiState.escalationRequired) {
        if (uiState.escalationRequired) onEmergency()
    }

    // Live countdown — 2 hours in seconds
    var totalSeconds by remember { mutableIntStateOf(7200) }
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning) {
        while (isRunning && totalSeconds > 0) {
            delay(1000L)
            totalSeconds--
        }
    }

    val progress = totalSeconds / 7200f

    // Pulsing timer glow
    val glowAnim = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowAnim.animateFloat(
        0.2f, 0.5f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = "glowAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Filled.ArrowBack, null, tint = colors.primary)
            }
            FgText("FocusGuard AI", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.primary)
            IconButton(onClick = { }) {
                Icon(Icons.Filled.MoreVert, null, tint = colors.onSurfaceVariant)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FgText(uiState.taskTitle.ifBlank { "Focus Sprint" }, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            FgText("Focus Sprint — 2 hours", fontSize = 14.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(36.dp))

            // Timer ring
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    color = colors.primary,
                    trackColor = colors.surfaceContainerHigh,
                )
                // Inner glow circle
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(
                            color = colors.primary.copy(alpha = glowAlpha),
                            shape = CircleShape,
                        )
                )
                Box(
                    modifier = Modifier
                        .size(218.dp)
                        .background(colors.surfaceContainerLow, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val h = totalSeconds / 3600
                        val m = (totalSeconds % 3600) / 60
                        val s = totalSeconds % 60
                        FgText(
                            text = "%d:%02d:%02d".format(h, m, s),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                        FgText("REMAINING", fontSize = 11.sp, color = colors.primary, letterSpacing = 2.sp)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Checkpoint card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(colors.surface, RoundedCornerShape(14.dp))
                    .border(1.dp, colors.outline.copy(0.3f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FgText("Progress at Checkpoint 2", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    Icon(Icons.Filled.Bolt, null, tint = colors.tertiary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.logCheckpoint(true) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2ED573).copy(alpha = 0.12f),
                            contentColor = Color(0xFF2ED573),
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Color(0xFF2ED573).copy(0.4f))),
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        FgText("Made Progress", color = Color(0xFF2ED573), fontSize = 13.sp)
                    }
                    Button(
                        onClick = { viewModel.logCheckpoint(false) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.error.copy(alpha = 0.12f),
                            contentColor = colors.error,
                        ),
                    ) {
                        Icon(Icons.Filled.Cancel, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        FgText("No Progress", color = colors.error, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            TextButton(onClick = { viewModel.endSprint(50); onNavigateBack() }) {
                FgText("End Sprint Early", color = colors.onSurfaceVariant, fontSize = 14.sp)
            }
        }
    }
}
