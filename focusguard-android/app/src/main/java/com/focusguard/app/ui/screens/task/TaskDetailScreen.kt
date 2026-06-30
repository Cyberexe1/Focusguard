package com.focusguard.app.ui.screens.task

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.app.domain.model.SubTask
import com.focusguard.app.domain.model.Task
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.theme.FgElevation
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun TaskDetailScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onStartSprint: () -> Unit = {},
    onGenerateRecovery: () -> Unit = {},
    viewModel: TaskDetailViewModel = hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    // Dismiss snack on check-in streak
    var showStreakBanner by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.checkInStreakMsg) {
        if (uiState.checkInStreakMsg.isNotBlank()) {
            showStreakBanner = true
            kotlinx.coroutines.delay(3000)
            showStreakBanner = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── App bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.ArrowBack, null, tint = colors.primary)
                }
                FgText(
                    text = uiState.task?.title?.take(22) ?: "Task Detail",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.onSurface,
                )
                IconButton(onClick = { }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Edit, null, tint = colors.primary)
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 2.5.dp)
                }
                uiState.task == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    FgText(uiState.error ?: "Task not found", color = colors.error)
                }
                else -> DetailContent(
                    task = uiState.task!!,
                    uiState = uiState,
                    onMarkComplete = viewModel::markComplete,
                    onStartSprint = onStartSprint,
                    onGenerateRecovery = onGenerateRecovery,
                    onAddSubTask = viewModel::addSubTask,
                    onToggleSubTask = viewModel::toggleSubTask,
                    onDeleteSubTask = viewModel::deleteSubTask,
                    onCheckIn = viewModel::dailyCheckIn,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Streak banner ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showStreakBanner,
            enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp, start = 16.dp, end = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = colors.shadowPrimary)
                    .background(FgGradients.primaryClay, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FgText(uiState.checkInStreakMsg, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DetailContent(
    task: Task,
    uiState: TaskDetailUiState,
    onMarkComplete: () -> Unit,
    onStartSprint: () -> Unit,
    onGenerateRecovery: () -> Unit,
    onAddSubTask: (String) -> Unit,
    onToggleSubTask: (String, Boolean) -> Unit,
    onDeleteSubTask: (String) -> Unit,
    onCheckIn: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusGuardTheme.colors
    val riskScore = task.priorityScore
    val riskColor = when {
        riskScore >= 80 -> colors.error
        riskScore >= 50 -> colors.warning
        else -> colors.success
    }
    val riskLabel = when {
        riskScore >= 80 -> "HIGH RISK"
        riskScore >= 50 -> "MEDIUM"
        else -> "LOW RISK"
    }
    val doneCount = task.subTasks.count { it.done }
    val totalSubs = task.subTasks.size
    val subProgress = if (totalSubs > 0) doneCount.toFloat() / totalSubs else 0f

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp)
                .navigationBarsPadding()
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Hero card ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = FgElevation.cardShadowColor)
                    .background(colors.surface, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .background(colors.warningContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            FgText(task.status.name.replace("_", " ").replaceFirstChar { it.uppercase() },
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.warning)
                        }
                        FgText(task.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        FgText("Category: ${task.category}", fontSize = 12.sp, color = colors.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(start = 12.dp)) {
                        Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { riskScore / 100f }, modifier = Modifier.fillMaxSize(),
                                color = riskColor, trackColor = riskColor.copy(0.1f), strokeWidth = 5.dp)
                            FgText("$riskScore", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = riskColor)
                        }
                        FgText(riskLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = riskColor, letterSpacing = 0.5.sp)
                    }
                }
                HorizontalDivider(color = colors.outline.copy(0.3f))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    InfoItem(Icons.Filled.CalendarMonth, "Deadline", task.deadline.take(10))
                    InfoItem(Icons.Filled.HourglassEmpty, "Effort", "${task.effortHours}h remaining")
                    if (task.checkinStreak > 0) {
                    InfoItem(Icons.Filled.Whatshot, "Streak", "${task.checkinStreak}d 🔥")
                    }
                }
            }

            // ── Daily check-in card ───────────────────────────────────────
            CheckInCard(
                checkedInToday = uiState.checkedInToday,
                streak = task.checkinStreak,
                onCheckIn = onCheckIn,
            )

            // ── Sub-tasks card ────────────────────────────────────────────
            SubTasksCard(
                subTasks = task.subTasks,
                isSaving = uiState.isSubTaskSaving,
                doneCount = doneCount,
                totalCount = totalSubs,
                progress = subProgress,
                onAdd = onAddSubTask,
                onToggle = onToggleSubTask,
                onDelete = onDeleteSubTask,
            )

            // ── Risk analysis card ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = FgElevation.cardShadowColor)
                    .background(colors.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Psychology, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    FgText("AI Risk Analysis", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(8.dp).background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(colors.success, colors.warning, colors.error)),
                        shape = RoundedCornerShape(4.dp),
                    ),
                ) {
                    Box(modifier = Modifier.size(14.dp).align(Alignment.CenterStart)
                        .offset(x = ((riskScore / 100f) * 280).dp - 7.dp)
                        .background(Color.White, RoundedCornerShape(7.dp))
                        .border(2.dp, riskColor, RoundedCornerShape(7.dp)))
                }
                RiskFactor("Time Pressure", (riskScore * 1.05f).coerceAtMost(100f).toInt(), colors.error)
                RiskFactor("Progress Deficit", ((1f - subProgress) * riskScore).toInt(), colors.warning)
                RiskFactor("Task Load", (riskScore * 0.75f).toInt(), colors.warning)
                if (task.priorityRankReason.isNotBlank()) {
                    Row(modifier = Modifier.fillMaxWidth().background(riskColor.copy(0.06f), RoundedCornerShape(10.dp)).padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Info, null, tint = riskColor, modifier = Modifier.size(14.dp))
                        FgText(task.priorityRankReason, fontSize = 12.sp, color = colors.onSurfaceVariant, modifier = Modifier.weight(1f))
                    }
                }
            }

            // ── Action buttons ────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onStartSprint, modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    elevation = ButtonDefaults.buttonElevation(6.dp)) {
                    Icon(Icons.Filled.Bolt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    FgText("Start Focus Sprint", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(onClick = onGenerateRecovery, modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(colors.primary.copy(0.4f)))) {
                    Icon(Icons.Filled.AutoFixHigh, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    FgText("Generate Recovery Plan", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // ── Sticky complete button ────────────────────────────────────────
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .shadow(12.dp, ambientColor = FgElevation.cardShadowColor)
            .background(colors.surface).navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp)) {
            Button(onClick = onMarkComplete, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.success),
                elevation = ButtonDefaults.buttonElevation(4.dp)) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                FgText("Mark as Complete", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Check-in card ─────────────────────────────────────────────────────────────

@Composable
private fun CheckInCard(checkedInToday: Boolean, streak: Int, onCheckIn: (String) -> Unit) {
    val colors = FocusGuardTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = colors.shadowPrimary)
            .background(
                if (checkedInToday) colors.successContainer else colors.primaryContainer,
                RoundedCornerShape(16.dp),
            )
            .border(1.dp, if (checkedInToday) colors.success.copy(0.3f) else colors.primaryLight, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(
                if (checkedInToday) FgGradients.successClay else FgGradients.primaryClay, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center) {
                Icon(if (checkedInToday) Icons.Filled.CheckCircle else Icons.Filled.Today, null,
                    tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column {
                FgText(if (checkedInToday) "Checked in today ✓" else "Daily Check-in",
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = if (checkedInToday) colors.success else colors.primary)
                FgText(
                    if (checkedInToday) "${streak}d streak 🔥" else "Tap to confirm you worked today",
                    fontSize = 12.sp, color = colors.onSurfaceVariant,
                )
            }
        }
        if (!checkedInToday) {
            Box(modifier = Modifier
                .background(FgGradients.primaryClay, RoundedCornerShape(10.dp))
                .clickable { onCheckIn("") }
                .padding(horizontal = 14.dp, vertical = 8.dp)) {
                FgText("Check In", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Sub-tasks card ────────────────────────────────────────────────────────────

@Composable
private fun SubTasksCard(
    subTasks: List<SubTask>,
    isSaving: Boolean,
    doneCount: Int,
    totalCount: Int,
    progress: Float,
    onAdd: (String) -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    val colors = FocusGuardTheme.colors
    var newTitle by remember { mutableStateOf("") }
    var showInput by remember { mutableStateOf(false) }
    val focusReq = remember { FocusRequester() }

    LaunchedEffect(showInput) {
        if (showInput) runCatching { focusReq.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = FgElevation.cardShadowColor)
            .background(colors.surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckBox, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                FgText("Sub-tasks", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                if (totalCount > 0) {
                    Box(modifier = Modifier.background(colors.primaryContainer, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        FgText("$doneCount/$totalCount", fontSize = 11.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Box(modifier = Modifier.size(30.dp).background(FgGradients.primaryClay, CircleShape)
                .clickable { showInput = !showInput }, contentAlignment = Alignment.Center) {
                Icon(if (showInput) Icons.Filled.Close else Icons.Filled.Add, null,
                    tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        // Progress bar
        if (totalCount > 0) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = colors.success,
                    trackColor = colors.successContainer,
                )
                FgText("${(progress * 100).toInt()}% complete", fontSize = 11.sp, color = colors.onSurfaceVariant)
            }
        }

        // Add input
        AnimatedVisibility(visible = showInput) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).background(colors.surfaceContainerLow, RoundedCornerShape(10.dp))
                    .border(1.dp, colors.outline, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    BasicTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusReq),
                        textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp),
                        cursorBrush = SolidColor(colors.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newTitle.isNotBlank()) { onAdd(newTitle); newTitle = ""; showInput = false }
                        }),
                        decorationBox = { inner ->
                            if (newTitle.isEmpty()) FgText("e.g. Write intro section", color = colors.onSurfaceMuted, fontSize = 14.sp)
                            inner()
                        },
                    )
                }
                Box(modifier = Modifier.size(40.dp).background(
                    if (newTitle.isNotBlank()) FgGradients.primaryClay else androidx.compose.ui.graphics.Brush.linearGradient(listOf(colors.outline, colors.outline)),
                    RoundedCornerShape(10.dp)).clickable(enabled = newTitle.isNotBlank() && !isSaving) {
                    onAdd(newTitle); newTitle = ""; showInput = false
                }, contentAlignment = Alignment.Center) {
                    if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Sub-task list
        if (subTasks.isEmpty() && !showInput) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center) {
                FgText("No sub-tasks yet — tap + to break this down", fontSize = 13.sp, color = colors.onSurfaceVariant)
            }
        } else {
            subTasks.forEach { sub ->
                SubTaskRow(sub = sub, onToggle = { onToggle(sub.id, it) }, onDelete = { onDelete(sub.id) })
            }
        }
    }
}

@Composable
private fun SubTaskRow(sub: SubTask, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val colors = FocusGuardTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Checkbox(
            checked = sub.done,
            onCheckedChange = onToggle,
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = colors.success,
                uncheckedColor = colors.outline,
                checkmarkColor = Color.White,
            ),
        )
        FgText(
            sub.title,
            fontSize = 14.sp,
            color = if (sub.done) colors.onSurfaceMuted else colors.onSurface,
            modifier = Modifier.weight(1f).alpha(if (sub.done) 0.6f else 1f),
            textDecoration = if (sub.done) TextDecoration.LineThrough else TextDecoration.None,
        )
        Icon(Icons.Filled.DeleteOutline, null, tint = colors.outline,
            modifier = Modifier.size(16.dp).clickable { onDelete() })
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun InfoItem(icon: ImageVector, label: String, value: String) {
    val colors = FocusGuardTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = colors.primary, modifier = Modifier.size(18.dp))
        Column {
            FgText(label, fontSize = 10.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.Medium)
            FgText(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
        }
    }
}

@Composable
private fun RiskFactor(label: String, pct: Int, color: Color) {
    val colors = FocusGuardTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FgText(label, fontSize = 13.sp, color = colors.onSurfaceVariant)
            FgText("$pct%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth().height(5.dp),
            color = color, trackColor = color.copy(0.1f))
    }
}
