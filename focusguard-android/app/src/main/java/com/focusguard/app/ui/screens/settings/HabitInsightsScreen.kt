package com.focusguard.app.ui.screens.settings

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.app.data.api.HabitInsightsDto
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun HabitInsightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: HabitInsightsViewModel = hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = FgGradients.backgroundBrush),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(colors.surface)
                .border(width = 1.dp, color = colors.outline)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, null, tint = colors.primary)
                }
                FgText("Habit Intelligence", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
            }
            Box(
                modifier = Modifier.size(36.dp).background(colors.primaryContainer, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Insights, null, tint = colors.primary, modifier = Modifier.size(18.dp))
            }
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            uiState.error != null && uiState.insights == null -> Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.CloudOff, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(48.dp))
                    FgText("Could not load insights", fontSize = 16.sp, color = colors.onBackground, fontWeight = FontWeight.SemiBold)
                    FgText(uiState.error ?: "", fontSize = 13.sp, color = colors.onSurfaceVariant)
                    Button(onClick = viewModel::load, shape = RoundedCornerShape(12.dp)) {
                        FgText("Retry", color = Color.White)
                    }
                }
            }
            else -> HabitInsightsContent(
                insights = uiState.insights,
                onNavigateBack = onNavigateBack,
            )
        }
    }
}

@Composable
private fun HabitInsightsContent(
    insights: HabitInsightsDto?,
    onNavigateBack: () -> Unit,
) {
    val colors = FocusGuardTheme.colors

    // Build a simple heatmap-style intensity grid from the consistency score (0-100)
    // We normalise it into a 4-row × 7-col visual — just for display beauty
    val consistency = (insights?.consistencyScore ?: 0) / 100f
    val heatmap = listOf(
        listOf(0f, 0.4f * consistency, 0.7f * consistency, 1f * consistency, 0.6f * consistency, 0f, 0f),
        listOf(0f, 0.3f * consistency, 0.5f * consistency, 0.8f * consistency, 1f * consistency, 0.4f * consistency, 0.2f * consistency),
        listOf(0.2f * consistency, 0.6f * consistency, 1f * consistency, 0.9f * consistency, 0.7f * consistency, 0f, 0f),
        listOf(0.1f * consistency, 0.4f * consistency, 0.6f * consistency, 0.8f * consistency, 1f * consistency, 0.3f * consistency, 0.1f * consistency),
    )

    val peakHour = insights?.peakProductivityHours?.firstOrNull() ?: "—"
    val effortGap = insights?.avgEffortUnderestimation?.let { "+${String.format("%.0f", it * 100)}%" } ?: "—"
    val recommendations = insights?.recommendations ?: listOf(
        "Complete more tasks to generate personalised insights.",
        "Start a focus sprint to track your productivity.",
        "Add deadlines so FocusGuard can rank your priorities.",
    )
    val dataInsufficient = insights?.dataInsufficient ?: true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        if (dataInsufficient) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.warningContainer, RoundedCornerShape(12.dp))
                    .border(1.dp, colors.warning.copy(0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Info, null, tint = colors.warning, modifier = Modifier.size(18.dp))
                FgText(
                    "Not enough data yet. Complete tasks and sprints to unlock full insights.",
                    fontSize = 13.sp, color = colors.onSurface,
                )
            }
        }

        // ── Heatmap ───────────────────────────────────────────────────
        SectionHeader("Focus Consistency", "Last 28 Days")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = colors.shadowCard)
                .background(colors.surface, RoundedCornerShape(18.dp))
                .border(1.dp, colors.outline, RoundedCornerShape(18.dp))
                .padding(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    FgText(it, fontSize = 11.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            heatmap.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    row.forEach { intensity ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .background(
                                    if (intensity > 0f) colors.primary.copy(alpha = (0.25f + intensity * 0.75f).coerceIn(0f, 1f))
                                    else colors.surfaceContainerHigh,
                                    RoundedCornerShape(6.dp),
                                ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FgText("Less", fontSize = 10.sp, color = colors.onSurfaceMuted)
                listOf(0.25f, 0.5f, 0.75f, 1f).forEach {
                    Box(Modifier.size(12.dp).background(colors.primary.copy(alpha = it), RoundedCornerShape(3.dp)))
                }
                FgText("More", fontSize = 10.sp, color = colors.onSurfaceMuted)
                Spacer(Modifier.weight(1f))
                FgText("Score: ${insights?.consistencyScore ?: 0}%", fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, color = colors.primary)
            }
        }

        // ── Peak hours hero ───────────────────────────────────────────
        SectionHeader("Peak Productivity Hours", null)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = colors.shadowPrimary)
                .background(FgGradients.heroBrush, RoundedCornerShape(20.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(52.dp).background(Color.White.copy(0.20f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.WbTwilight, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Column {
                    FgText(peakHour, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    FgText("Cognitive Peak Window", fontSize = 13.sp, color = Color.White.copy(0.75f))
                }
            }
        }

        // ── Behavioral patterns ───────────────────────────────────────
        SectionHeader("Behavioral Patterns", null)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PatternCard(Modifier.weight(1f), Icons.Filled.Schedule, FgGradients.primaryClay, "Most productive", peakHour)
            PatternCard(Modifier.weight(1f), Icons.Filled.TrendingUp, FgGradients.errorClay, "Effort gap", effortGap)
        }

        // ── AI Recommendations ────────────────────────────────────────
        SectionHeader("AI Guard Recommendations", null)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = colors.shadowCard)
                .background(colors.surface, RoundedCornerShape(18.dp))
                .border(1.dp, colors.outline, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            recommendations.forEachIndexed { index, rec ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(26.dp).background(FgGradients.primaryClay, RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        FgText("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    FgText(rec, fontSize = 13.sp, color = colors.onSurface, modifier = Modifier.weight(1f))
                }
            }
        }

        // ── Adapt button ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = colors.shadowPrimary)
                .background(FgGradients.primaryClay, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoFixHigh, null, tint = Color.White, modifier = Modifier.size(18.dp))
                FgText("Adapt My Schedule", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.navigationBarsPadding())
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(title: String, badge: String?) {
    val colors = FocusGuardTheme.colors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        FgText(title.uppercase(), fontSize = 11.sp, color = colors.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        badge?.let { FgText(it, fontSize = 11.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun PatternCard(modifier: Modifier, icon: ImageVector, gradient: Brush, label: String, value: String) {
    val colors = FocusGuardTheme.colors
    Column(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = colors.shadowCard)
            .background(colors.surface, RoundedCornerShape(16.dp))
            .border(1.dp, colors.outline, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .heightIn(min = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(gradient, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Column {
            FgText(label, fontSize = 12.sp, color = colors.onSurfaceVariant)
            FgText(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
        }
    }
}
