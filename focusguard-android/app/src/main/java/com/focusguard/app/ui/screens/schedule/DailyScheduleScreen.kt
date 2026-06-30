package com.focusguard.app.ui.screens.schedule

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.app.notifications.ScheduleNotifier
import com.focusguard.app.ui.components.BottomNavBar
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.navigation.BottomNavRoute
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme
import java.util.Calendar

/** A user-entered schedule block. startMin/endMin are minutes-of-day (0..1439). */
data class ScheduleBlockInput(
    val id: Int,
    var name: String = "",
    var startMin: Int? = null,
    var endMin: Int? = null,
    var repeatDays: Int = 1,   // how many days this block repeats (1 = today only)
)

@Composable
fun DailyScheduleScreen(
    onNavigateBack: () -> Unit,
    onBottomNavClick: (BottomNavRoute) -> Unit,
    viewModel: DailyScheduleViewModel = hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val context = LocalContext.current
    var showForm by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val savedBlocks = uiState.userBlocks

    Box(modifier = Modifier.fillMaxSize().background(brush = FgGradients.backgroundBrush)) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(colors.surface)
                    .border(width = 1.dp, color = colors.outline)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, null, tint = colors.primary)
                    }
                    Column {
                        FgText("Today's Schedule", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                        FgText("Tap + to add timed reminders", fontSize = 11.sp, color = colors.onSurfaceVariant)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(FgGradients.primaryClay, RoundedCornerShape(11.dp))
                        .clickable { showForm = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            // ── Date strip (real current week) ────────────────────────────
            val weekDays = remember {
                val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
                val cal = Calendar.getInstance()
                // Rewind to Sunday of the current week
                val todayDow = cal.get(Calendar.DAY_OF_WEEK) - 1  // 0=Sun
                cal.add(Calendar.DAY_OF_MONTH, -todayDow)
                (0..6).map { offset ->
                    val c = cal.clone() as Calendar
                    c.add(Calendar.DAY_OF_MONTH, offset)
                    Triple(
                        dayLabels[offset],
                        c.get(Calendar.DAY_OF_MONTH).toString(),
                        offset == todayDow,  // isToday
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                weekDays.forEach { (day, date, isToday) ->
                    Column(
                        modifier = Modifier
                            .then(
                                if (isToday) Modifier
                                    .shadow(6.dp, RoundedCornerShape(12.dp), ambientColor = colors.shadowPrimary)
                                    .background(FgGradients.primaryClay, RoundedCornerShape(12.dp))
                                else Modifier
                                    .background(colors.surfaceContainerLow, RoundedCornerShape(12.dp))
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        FgText(day, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = if (isToday) Color.White.copy(0.85f) else colors.onSurfaceVariant)
                        FgText(date, fontSize = 15.sp, fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isToday) Color.White else colors.onBackground)
                    }
                }
            }

            // ── Schedule list ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 100.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (savedBlocks.isEmpty()) {
                    // Default sample timeline
                    SampleTimeline()
                } else {
                    DayTimeline(savedBlocks)
                }
            }
        }

        // Regenerate / Add button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                .shadow(12.dp, RoundedCornerShape(999.dp), ambientColor = colors.shadowPrimary)
                .background(FgGradients.primaryClay, RoundedCornerShape(999.dp))
                .clickable { showForm = true }
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoFixHigh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                FgText("Regenerate Schedule", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        BottomNavBar(currentRoute = BottomNavRoute.Schedule, onNavigate = onBottomNavClick, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showForm) {
        ScheduleFormSheet(
            initialBlocks = savedBlocks,
            onDismiss = { showForm = false },
            onSave = { blocks ->
                viewModel.saveUserBlocks(blocks) { ok ->
                    Toast.makeText(
                        context,
                        if (ok) "Schedule saved" else "Saved on device (sync failed)",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                showForm = false
            },
        )
    }
}

// ── Schedule form bottom sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleFormSheet(
    initialBlocks: List<ScheduleBlockInput> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (List<ScheduleBlockInput>) -> Unit,
) {
    val colors = FocusGuardTheme.colors
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var nextId by remember { mutableStateOf((initialBlocks.maxOfOrNull { it.id } ?: 0) + 1) }
    val blocks = remember {
        mutableStateListOf<ScheduleBlockInput>().apply {
            if (initialBlocks.isNotEmpty()) addAll(initialBlocks) else add(ScheduleBlockInput(id = 0))
        }
    }

    // Notification permission launcher (Android 13+)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled silently */ }

    fun pickTime(initialMin: Int?, onPicked: (Int) -> Unit) {
        val cal = Calendar.getInstance()
        val h = initialMin?.div(60) ?: cal.get(Calendar.HOUR_OF_DAY)
        val m = initialMin?.rem(60) ?: cal.get(Calendar.MINUTE)
        TimePickerDialog(context, { _, hour, minute -> onPicked(hour * 60 + minute) }, h, m, false).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FgText("Create Schedule", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.onSurface)
            FgText("Add timed blocks. You'll get a notification when each one starts.", fontSize = 12.sp, color = colors.onSurfaceVariant)

            blocks.forEachIndexed { index, block ->
                ScheduleBlockEditor(
                    block = block,
                    index = index,
                    canRemove = blocks.size > 1,
                    onNameChange = { blocks[index] = block.copy(name = it) },
                    onPickStart = { pickTime(block.startMin) { blocks[index] = block.copy(startMin = it) } },
                    onPickEnd = { pickTime(block.endMin) { blocks[index] = block.copy(endMin = it) } },
                    onRemove = { blocks.removeAt(index) },
                    onRepeatDaysChange = { blocks[index] = block.copy(repeatDays = it) },
                )
            }

            // Add another schedule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryContainer, RoundedCornerShape(12.dp))
                    .border(1.dp, colors.primaryLight, RoundedCornerShape(12.dp))
                    .clickable { blocks.add(ScheduleBlockInput(id = nextId)); nextId++ }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Add, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    FgText("Add another schedule", color = colors.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Save
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = colors.shadowPrimary)
                    .background(FgGradients.primaryClay, RoundedCornerShape(14.dp))
                    .clickable {
                        val valid = blocks.filter { it.name.isNotBlank() && it.startMin != null }
                        if (valid.isEmpty()) {
                            Toast.makeText(context, "Add a name and start time", Toast.LENGTH_SHORT).show()
                            return@clickable
                        }
                        // Request notification permission on Android 13+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        // Schedule a notification for each valid block's start time
                        var scheduledCount = 0
                        valid.forEach { b ->
                            val triggerAt = nextTriggerMillis(b.startMin!!)
                            ScheduleNotifier.scheduleAt(
                                context = context,
                                notifId = 1000 + b.id,
                                title = "⏰ ${b.name}",
                                message = "Your scheduled block \"${b.name}\" is starting now. Time to focus!",
                                triggerAtMillis = triggerAt,
                            )
                            scheduledCount++
                        }
                        Toast.makeText(context, "$scheduledCount reminder(s) set", Toast.LENGTH_SHORT).show()
                        onSave(valid)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    FgText("Save & Set Reminders", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScheduleBlockEditor(
    block: ScheduleBlockInput,
    index: Int,
    canRemove: Boolean,
    onNameChange: (String) -> Unit,
    onPickStart: () -> Unit,
    onPickEnd: () -> Unit,
    onRemove: () -> Unit,
    onRepeatDaysChange: (Int) -> Unit,
) {
    val colors = FocusGuardTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceContainerLow, RoundedCornerShape(14.dp))
            .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FgText("Schedule ${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.primary, letterSpacing = 0.5.sp)
            if (canRemove) {
                Icon(
                    Icons.Filled.DeleteOutline, null,
                    tint = colors.error,
                    modifier = Modifier.size(20.dp).clickable { onRemove() },
                )
            }
        }

        // Name field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(colors.surface, RoundedCornerShape(10.dp))
                .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = block.name,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.onSurface, fontSize = 14.sp),
                cursorBrush = SolidColor(colors.primary),
                decorationBox = { inner ->
                    if (block.name.isEmpty()) {
                        FgText("Schedule name (e.g. Study DSA)", color = colors.onSurfaceMuted, fontSize = 14.sp)
                    }
                    inner()
                },
            )
        }

        // Time pickers
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimePickField(
                label = "Start time",
                value = block.startMin?.let { formatMin(it) } ?: "Select",
                modifier = Modifier.weight(1f),
                onClick = onPickStart,
            )
            TimePickField(
                label = "End time",
                value = block.endMin?.let { formatMin(it) } ?: "Select",
                modifier = Modifier.weight(1f),
                onClick = onPickEnd,
            )
        }

        // Repeat days stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                FgText("Repeat for", fontSize = 11.sp, color = colors.onSurfaceVariant)
                FgText(
                    if (block.repeatDays == 1) "Today only" else "${block.repeatDays} days",
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (block.repeatDays > 1) colors.primaryContainer else colors.surfaceContainerLow,
                            RoundedCornerShape(8.dp),
                        )
                        .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                        .clickable(enabled = block.repeatDays > 1) { onRepeatDaysChange(block.repeatDays - 1) },
                    contentAlignment = Alignment.Center,
                ) {
                    FgText("−", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = if (block.repeatDays > 1) colors.primary else colors.onSurfaceMuted)
                }
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .background(colors.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FgText("${block.repeatDays}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.primary)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (block.repeatDays < 30) colors.primaryContainer else colors.surfaceContainerLow,
                            RoundedCornerShape(8.dp),
                        )
                        .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                        .clickable(enabled = block.repeatDays < 30) { onRepeatDaysChange(block.repeatDays + 1) },
                    contentAlignment = Alignment.Center,
                ) {
                    FgText("+", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = if (block.repeatDays < 30) colors.primary else colors.onSurfaceMuted)
                }
            }
        }
    }
}

@Composable
private fun TimePickField(label: String, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = FocusGuardTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FgText(label, fontSize = 11.sp, color = colors.onSurfaceVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(colors.surface, RoundedCornerShape(10.dp))
                .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Schedule, null, tint = colors.primary, modifier = Modifier.size(16.dp))
            FgText(value, fontSize = 14.sp, color = if (value == "Select") colors.onSurfaceMuted else colors.onSurface, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DayTimeline(blocks: List<ScheduleBlockInput>) {
    val colors = FocusGuardTheme.colors
    val sorted = blocks.sortedBy { it.startMin ?: 0 }

    val minStart = sorted.minOf { it.startMin ?: 0 }
    val maxEnd = sorted.maxOf { it.endMin ?: ((it.startMin ?: 0) + 60) }

    val startHour = (minStart / 60).coerceIn(0, 23)
    val endHour = ((maxEnd + 59) / 60).coerceIn(startHour + 1, 24)

    val dpPerMin = 1.15f
    val labelWidth = 60.dp
    val totalMinutes = (endHour - startHour) * 60
    val railHeight = (totalMinutes * dpPerMin).dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(railHeight),
    ) {
        // ── Hour grid: label + horizontal line at each hour mark ──────────
        for (h in startHour..endHour) {
            val offsetY = ((h - startHour) * 60 * dpPerMin).dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = offsetY),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FgText(
                    formatHourLabel(h),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.width(labelWidth),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(colors.outline),
                )
            }
        }

        // ── Event blocks positioned against the rail ──────────────────────
        sorted.forEach { block ->
            val start = block.startMin ?: return@forEach
            val end = (block.endMin ?: (start + 60)).coerceAtLeast(start + 30)
            val top = ((start - startHour * 60) * dpPerMin).dp
            val blockHeight = ((end - start) * dpPerMin).dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = labelWidth + 8.dp, end = 2.dp)
                    .offset(y = top)
                    .height(blockHeight)
                    .padding(vertical = 2.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = colors.shadowCard)
                        .background(colors.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.outline, RoundedCornerShape(12.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                            .background(FgGradients.primaryClay),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            FgText(block.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface, maxLines = 1)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Schedule, null, tint = colors.onSurfaceMuted, modifier = Modifier.size(12.dp))
                                FgText(
                                    "${formatMin(start)} – ${formatMin(end)}",
                                    fontSize = 11.sp,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                )
                                if (block.repeatDays > 1) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(colors.primaryContainer, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        FgText("${block.repeatDays}d", fontSize = 9.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Icon(Icons.Filled.NotificationsActive, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleTimeline() {
    val colors = FocusGuardTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FgText("No schedules yet", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
        FgText("Tap \"Regenerate Schedule\" or + to add timed blocks with reminders.", fontSize = 13.sp, color = colors.onSurfaceVariant)
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────────

private fun formatMin(min: Int): String {
    val h24 = min / 60
    val m = min % 60
    val ampm = if (h24 < 12) "AM" else "PM"
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return "%02d:%02d %s".format(h12, m, ampm)
}

private fun formatHourLabel(hour24: Int): String {
    val h = hour24 % 24
    val ampm = if (h < 12) "AM" else "PM"
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "$h12 $ampm"
}

/** Next epoch-millis for a given minute-of-day; if already past today, schedule for tomorrow. */
private fun nextTriggerMillis(minuteOfDay: Int): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        set(Calendar.MINUTE, minuteOfDay % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (target.timeInMillis <= now.timeInMillis) {
        target.add(Calendar.DAY_OF_MONTH, 1)
    }
    return target.timeInMillis
}
