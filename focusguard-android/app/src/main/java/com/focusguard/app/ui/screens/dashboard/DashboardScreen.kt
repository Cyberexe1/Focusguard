package com.focusguard.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.collectAsState
import com.focusguard.app.ui.components.BottomNavBar
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.navigation.BottomNavRoute
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun DashboardScreen(
    onNavigateBack: () -> Unit,
    onViewHabits: () -> Unit,
    onBottomNavClick: (BottomNavRoute) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val daily = uiState.daily
    var selectedTab by remember { mutableIntStateOf(uiState.selectedTab) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = FgGradients.backgroundBrush),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.statusBarsPadding())

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    FgText("Dashboard", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = colors.onBackground)
                    FgText("Your productivity overview", fontSize = 13.sp, color = colors.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(colors.primaryContainer, CircleShape)
                        .border(1.dp, colors.primaryLight, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Share, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Period toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = colors.shadowCard)
                        .background(colors.surface, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                ) {
                    listOf("Daily", "Weekly").forEachIndexed { index, label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (selectedTab == index) Modifier
                                        .shadow(6.dp, RoundedCornerShape(10.dp), ambientColor = colors.shadowPrimary)
                                        .background(FgGradients.primaryClay, RoundedCornerShape(10.dp))
                                    else Modifier
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            FgText(
                                label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = if (selectedTab == index) Color.White else colors.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Score gauge hero card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = colors.shadowPrimary)
                        .background(FgGradients.heroBrush, RoundedCornerShape(24.dp))
                        .padding(24.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FgText("Productivity", fontSize = 13.sp, color = Color.White.copy(0.75f))
                            FgText("${daily?.productivityScore ?: 91}", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(0.20f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                            ) {
                                FgText("↑ 12% vs yesterday", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { (daily?.productivityScore ?: 91) / 100f },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 8.dp,
                                color = Color.White,
                                trackColor = Color.White.copy(0.20f),
                            )
                            FgText("SCORE", fontSize = 9.sp, color = Color.White.copy(0.70f), letterSpacing = 1.sp)
                        }
                    }
                }

                // Stats grid
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashStat(Modifier.weight(1f), Icons.Filled.CheckCircle, FgGradients.successClay, colors.successContainer, "${daily?.tasksCompleted ?: 0}", "Completed")
                    DashStat(Modifier.weight(1f), Icons.Filled.Schedule, FgGradients.blueClay, colors.secondaryLight, "${daily?.focusHours ?: 0}h", "Focus")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashStat(Modifier.weight(1f), Icons.Filled.Shield, FgGradients.primaryClay, colors.primaryLight, "${daily?.deadlinesSaved ?: 0}", "Saved")
                    DashStat(Modifier.weight(1f), Icons.Filled.Bolt, FgGradients.warningClay, colors.warningContainer, "${daily?.sprintSuccessRate?.toInt() ?: 0}%", "Sprint")
                }

                // Bar chart card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = colors.shadowCard)
                        .background(colors.surface, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        FgText("Tasks by Category", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Icon(Icons.Filled.BarChart, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        listOf(
                            Triple("Hackathon", 0.9f, FgGradients.primaryClay),
                            Triple("Assign.", 0.6f, FgGradients.blueClay),
                            Triple("Personal", 0.38f, FgGradients.successClay),
                        ).forEach { (label, h, grad) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Box(Modifier.fillMaxWidth().fillMaxHeight(h).background(grad, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
                                Spacer(Modifier.height(5.dp))
                                FgText(label, fontSize = 10.sp, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }

                // AI Insight card — shows first real recommendation if available
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = colors.shadowPrimary)
                        .background(colors.primaryContainer, RoundedCornerShape(18.dp))
                        .border(1.dp, colors.primaryLight, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(FgGradients.primaryClay, RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                        FgText("Focus Insight", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                        FgText(
                            uiState.habits?.recommendations?.firstOrNull()
                                ?: uiState.habits?.peakProductivityHours?.firstOrNull()?.let { "Peak hours: $it — schedule deep work then." }
                                ?: "Add more tasks and sprints to generate insights.",
                            fontSize = 12.sp, color = colors.onSurfaceVariant
                        )
                    }
                }

                // View habits button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = colors.shadowPrimary)
                        .background(FgGradients.primaryClay, RoundedCornerShape(14.dp))
                        .clickable { onViewHabits() },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Psychology, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        FgText("View Habit Intelligence", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        }

        BottomNavBar(currentRoute = BottomNavRoute.Dashboard, onNavigate = onBottomNavClick, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun DashStat(modifier: Modifier, icon: ImageVector, gradient: Brush, bgColor: Color, value: String, label: String) {
    val colors = FocusGuardTheme.colors
    Row(
        modifier = modifier
            .height(80.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = colors.shadowCard)
            .background(colors.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(36.dp).background(gradient, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Column {
            FgText(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colors.onBackground)
            FgText(label, fontSize = 11.sp, color = colors.onSurfaceVariant)
        }
    }
}
