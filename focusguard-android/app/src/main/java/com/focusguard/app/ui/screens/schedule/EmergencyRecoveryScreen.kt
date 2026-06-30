package com.focusguard.app.ui.screens.schedule

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme

private val EmergencyRed = Color(0xFFFF4757)

@Composable
fun EmergencyRecoveryScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onAccepted: () -> Unit,
    viewModel: EmergencyRecoveryViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    // Pulsing border
    val pulse = rememberInfiniteTransition(label = "pulse")
    val borderAlpha by pulse.animateFloat(
        0.15f, 0.4f,
        infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "border",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = FgGradients.backgroundBrush)
            .border(4.dp, EmergencyRed.copy(alpha = borderAlpha)),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Emergency alert banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(EmergencyRed)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    FgText(
                        if (uiState.taskTitle.isNotBlank())
                            "EMERGENCY MODE — ${uiState.taskTitle.take(30)}"
                        else "EMERGENCY MODE — deadline at risk",
                        color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            // App bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, null, tint = colors.onSurfaceVariant)
                }
                FgText("FocusGuard AI", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                IconButton(onClick = { }) {
                    Icon(Icons.Filled.MoreVert, null, tint = colors.onSurfaceVariant)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // AI plan card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(0.25f))
                        .background(Color.White.copy(0.08f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(colors.primaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = colors.onPrimaryContainer, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        FgText("AI Recovery Plan Generated", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        FgText(
                            uiState.warningMessage.ifBlank { "All non-critical tasks postponed. Focus on your deadline." },
                            fontSize = 13.sp, color = colors.onSurfaceVariant,
                        )
                    }
                }

                when {
                    uiState.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmergencyRed)
                    }
                    uiState.error != null -> {
                        FgText("⚠ ${uiState.error}", fontSize = 13.sp, color = EmergencyRed)
                        TextButton(onClick = viewModel::loadPlan) {
                            FgText("Retry", color = colors.primary)
                        }
                    }
                    uiState.phases.isNotEmpty() -> {
                        FgText("OPTIMIZED EXECUTION PATH", fontSize = 11.sp, color = colors.onSurfaceVariant, letterSpacing = 1.sp)
                        val phaseColors = listOf(EmergencyRed, Color(0xFFF59E0B), Color(0xFF10B981))
                        uiState.phases.forEachIndexed { i, phase ->
                            RecoveryTimelineItem(
                                title = phase.title,
                                timing = phase.timing,
                                accent = phaseColors.getOrElse(i) { colors.primary },
                                subtitle = phase.description,
                            )
                        }
                    }
                    else -> {
                        // fallback static plan if API returned empty
                        FgText("OPTIMIZED EXECUTION PATH", fontSize = 11.sp, color = colors.onSurfaceVariant, letterSpacing = 1.sp)
                        RecoveryTimelineItem("Core Work", "NOW → 2h", EmergencyRed, "Focus on the essential deliverables.")
                        RecoveryTimelineItem("Testing & Fix", "+2h → 1.5h", Color(0xFFF59E0B), "Quick test and fix critical bugs.")
                        RecoveryTimelineItem("Submit", "+3.5h → 30m", Color(0xFF10B981), "Final review and submission.")
                    }
                }
            }
        }

        // Bottom buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFF1A0D2E).copy(alpha = 0.97f))
                .border(width = 1.dp, color = Color.White.copy(0.08f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onAccepted,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                FgText("Accept Recovery Plan", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(colors.primary.copy(0.6f))
                ),
            ) {
                FgText("Modify Plan", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RecoveryTimelineItem(title: String, timing: String, accent: Color, subtitle: String) {
    val colors = FocusGuardTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier
            .width(3.dp)
            .height(80.dp)
            .background(accent, RoundedCornerShape(2.dp)))
        Column(
            modifier = Modifier
                .weight(1f)
                .background(Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                .border(
                    width = 4.dp,
                    color = accent,
                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 12.dp, bottomEnd = 12.dp),
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                FgText(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Box(
                    modifier = Modifier
                        .background(accent.copy(0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    FgText(timing, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accent)
                }
            }
            FgText(subtitle, fontSize = 12.sp, color = colors.onSurfaceVariant)
        }
    }
}
