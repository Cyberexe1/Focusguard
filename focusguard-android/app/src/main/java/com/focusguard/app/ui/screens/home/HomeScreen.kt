package com.focusguard.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.app.domain.model.PriorityLevel
import com.focusguard.app.domain.model.Task
import com.focusguard.app.domain.model.priorityLevel
import com.focusguard.app.ui.components.BottomNavBar
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.navigation.BottomNavRoute
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun HomeScreen(
    onNavigateToAddTask: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onBottomNavClick: (BottomNavRoute) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = FgGradients.backgroundBrush),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item { Spacer(Modifier.statusBarsPadding()) }

            // ── Hero header ──────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = colors.shadowPrimary)
                        .background(FgGradients.heroBrush, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            FgText("Good day 👋", fontSize = 13.sp, color = Color.White.copy(0.75f))
                            FgText(uiState.userName.ifBlank { "User" }, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .background(Color.White.copy(0.20f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.TrendingUp, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                FgText("${uiState.tasks.count { it.status.name != "completed" }} tasks active", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color.White.copy(0.20f), CircleShape)
                                .border(2.dp, Color.White.copy(0.40f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            FgText(uiState.userName.firstOrNull()?.uppercase() ?: "U", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // ── Stats row ─────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        Modifier.weight(1f),
                        Icons.Filled.AssignmentTurnedIn,
                        FgGradients.primaryClay,
                        colors.primaryLight,
                        colors.shadowPrimary,
                        "${uiState.tasks.count { it.status.name != "completed" }}",
                        "Active",
                    )
                    StatCard(
                        Modifier.weight(1f),
                        Icons.Filled.Warning,
                        FgGradients.errorClay,
                        colors.errorContainer,
                        Color(0xFFEF4444).copy(0.15f),
                        "${uiState.tasks.count { it.priorityScore >= 80 }}",
                        "At Risk",
                    )
                    StatCard(
                        Modifier.weight(1f),
                        Icons.Filled.Bolt,
                        FgGradients.blueClay,
                        colors.secondaryLight,
                        colors.shadowBlue,
                        "7PM",
                        "Peak",
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Countdown carousel ────────────────────────────────────────
            if (uiState.tasks.isNotEmpty()) {
                item {
                    DeadlineCountdownCarousel(
                        tasks = uiState.tasks,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            // ── Section heading ───────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FgText("Today's Tasks", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Box(
                        modifier = Modifier
                            .background(colors.primaryContainer, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                    ) {
                        FgText("See All", fontSize = 12.sp, color = colors.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Task list ─────────────────────────────────────────────────
            when {
                uiState.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary, strokeWidth = 2.5.dp)
                    }
                }
                uiState.error != null -> item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .background(colors.errorContainer, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        FgText(uiState.error!!, color = colors.error, fontSize = 13.sp)
                    }
                }
                uiState.tasks.isEmpty() -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.CheckCircleOutline, null, tint = colors.primaryVariant, modifier = Modifier.size(48.dp))
                        FgText("No tasks yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
                        FgText("Tap + to add your first task", fontSize = 13.sp, color = colors.onSurfaceVariant)
                    }
                }
                else -> items(uiState.tasks, key = { it.taskId }) { task ->
                    TaskCard(
                        task = task,
                        onClick = { onNavigateToTaskDetail(task.taskId) },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            // ── AI Insight ────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                AiInsightBanner(modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(12.dp))
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 100.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                .size(56.dp)
                .shadow(16.dp, CircleShape, ambientColor = colors.shadowPrimary)
                .background(FgGradients.primaryClay, CircleShape)
                .clickable { onNavigateToAddTask() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }

        BottomNavBar(
            currentRoute = BottomNavRoute.Home,
            onNavigate = onBottomNavClick,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    gradient: Brush,
    bgColor: Color,
    shadowColor: Color,
    value: String,
    label: String,
) {
    val colors = FocusGuardTheme.colors
    Column(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = shadowColor)
            .background(colors.surface, RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(gradient, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        FgText(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.onBackground)
        FgText(label, fontSize = 10.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusGuardTheme.colors
    val (accentGradient, accentColor, accentBg) = when (task.priorityLevel()) {
        PriorityLevel.HIGH   -> Triple(FgGradients.errorClay,   Color(0xFFEF4444), Color(0xFFFEE2E2))
        PriorityLevel.MEDIUM -> Triple(FgGradients.warningClay, Color(0xFFF59E0B), Color(0xFFFEF3C7))
        PriorityLevel.LOW    -> Triple(FgGradients.successClay, Color(0xFF10B981), Color(0xFFD1FAE5))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = colors.shadowCard)
            .background(colors.surface, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left accent strip
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                .background(accentGradient),
        )
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f).padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(accentBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    FgText("${task.priorityScore}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
                }
                Box(
                    modifier = Modifier
                        .background(colors.primaryContainer, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    FgText(task.category, fontSize = 10.sp, color = colors.primary, fontWeight = FontWeight.Medium)
                }
                // Streak badge
                if (task.checkinStreak > 0) {
                    Box(
                        modifier = Modifier
                            .background(colors.warningContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        FgText("🔥 ${task.checkinStreak}d", fontSize = 10.sp, color = colors.warning, fontWeight = FontWeight.Bold)
                    }
                }
            }
            FgText(task.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Schedule, null, tint = colors.onSurfaceMuted, modifier = Modifier.size(12.dp))
                FgText(task.deadline.take(16), fontSize = 11.sp, color = colors.onSurfaceMuted)
            }
            // Sub-task mini progress bar
            if (task.subTasks.isNotEmpty()) {
                val done = task.subTasks.count { it.done }
                val total = task.subTasks.size
                val pct = done.toFloat() / total
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier.weight(1f).height(4.dp),
                        color = if (pct >= 1f) colors.success else colors.primary,
                        trackColor = colors.primaryContainer,
                    )
                    FgText("$done/$total", fontSize = 10.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = colors.outline, modifier = Modifier.size(18.dp).padding(end = 2.dp))
        Spacer(Modifier.width(12.dp))
    }
}

// ── Countdown carousel ────────────────────────────────────────────────────────

@Composable
private fun DeadlineCountdownCarousel(
    tasks: List<Task>,
    modifier: Modifier = Modifier,
) {
    val colors = FocusGuardTheme.colors
    // Sort by soonest deadline first, cap at 5
    val items = remember(tasks) {
        tasks.sortedBy { it.deadline }.take(5)
    }
    if (items.isEmpty()) return

    var index by remember { mutableStateOf(0) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    // Tick every second for live countdown
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }
    // Auto-advance every 5 seconds
    LaunchedEffect(items.size) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            if (items.size > 1) index = (index + 1) % items.size
        }
    }

    val task = items[index.coerceIn(0, items.lastIndex)]
    val timeUnits = remainingTimeUnits(task.deadline, nowMillis)
    val urgent = timeUnits.urgent
    val gradient = if (urgent) FgGradients.errorClay else FgGradients.blueClay
    val shadowCol = if (urgent) Color(0xFFEF4444).copy(0.18f) else colors.shadowBlue

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = shadowCol)
            .background(gradient, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Timer, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    FgText(
                        if (urgent) "DEADLINE APPROACHING" else "TIME REMAINING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(0.85f),
                        letterSpacing = 1.sp,
                    )
                }
                // Slide indicator dots
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == index) 7.dp else 5.dp)
                                .background(
                                    Color.White.copy(if (i == index) 0.95f else 0.40f),
                                    CircleShape,
                                )
                        )
                    }
                }
            }

            FgText(
                task.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Digital stopwatch display
            if (timeUnits.overdue) {
                FgText("OVERDUE", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (timeUnits.days > 0) {
                        TimeUnitBlock(timeUnits.days, "DAYS")
                        TimeColon()
                    }
                    TimeUnitBlock(timeUnits.hours, "HRS")
                    TimeColon()
                    TimeUnitBlock(timeUnits.minutes, "MIN")
                    TimeColon()
                    TimeUnitBlock(timeUnits.seconds, "SEC")
                }
            }
        }
    }
}

@Composable
private fun TimeUnitBlock(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(
            modifier = Modifier
                .background(Color.White.copy(0.22f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .width(46.dp),
            contentAlignment = Alignment.Center,
        ) {
            FgText(
                value.toString().padStart(2, '0'),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
        FgText(label, fontSize = 8.sp, color = Color.White.copy(0.75f), letterSpacing = 1.sp)
    }
}

@Composable
private fun TimeColon() {
    FgText(":", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(0.60f), modifier = Modifier.padding(bottom = 14.dp))
}

private data class TimeRemaining(
    val days: Long,
    val hours: Long,
    val minutes: Long,
    val seconds: Long,
    val urgent: Boolean,
    val overdue: Boolean,
)

/**
 * Parses the deadline and returns days/hours/minutes/seconds remaining.
 * Handles ISO formats like "2026-07-04T13:58" and "2026-06-27T14:00:00Z".
 */
private fun remainingTimeUnits(deadline: String, nowMillis: Long): TimeRemaining {
    return try {
        var s = deadline.trim()
        if (s.endsWith("Z")) s = s.dropLast(1)
        val normalized = if (s.count { it == ':' } == 1) "$s:00" else s
        val ldt = java.time.LocalDateTime.parse(normalized)
        val deadlineMillis = ldt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
        val diff = deadlineMillis - (nowMillis - java.util.TimeZone.getDefault().getOffset(nowMillis))

        if (diff <= 0) return TimeRemaining(0, 0, 0, 0, urgent = true, overdue = true)

        val totalSec = diff / 1000
        val days = totalSec / 86400
        val hours = (totalSec % 86400) / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        TimeRemaining(days, hours, minutes, seconds, urgent = days == 0L, overdue = false)
    } catch (e: Exception) {
        TimeRemaining(0, 0, 0, 0, urgent = false, overdue = false)
    }
}

@Composable
private fun AiInsightBanner(modifier: Modifier = Modifier) {
    val colors = FocusGuardTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp), ambientColor = colors.shadowPrimary)
            .background(FgGradients.primaryClay, RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color.White.copy(0.20f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Psychology, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            FgText("AI Productivity Insight", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            FgText(
                "You're 15% more productive at 7 PM. Move high-priority tasks there.",
                fontSize = 12.sp,
                color = Color.White.copy(0.80f),
                maxLines = 2,
            )
        }
    }
}
