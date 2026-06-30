package com.focusguard.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.app.data.api.CalendarEventDto
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.theme.FgElevation
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun CalendarSyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalendarSyncViewModel = hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    // State for the add event form
    var newTitle by remember { mutableStateOf("") }
    var newStart by remember { mutableStateOf("") }
    var newEnd by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.statusBarsPadding())

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.ArrowBack, null, tint = colors.primary)
                    }
                    Column {
                        FgText("My Calendar", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.onBackground)
                        FgText("Built-in FocusGuard scheduler", fontSize = 12.sp, color = colors.onSurfaceVariant)
                    }
                }
                IconButton(
                    onClick = viewModel::loadEvents,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Filled.Refresh, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Info banner — explains the no-OAuth approach
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.primaryContainer, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.primary.copy(0.2f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.Info, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FgText("No sign-in required", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                        FgText(
                            "Add your classes, meetings, and blocked time here. FocusGuard schedules around them automatically. Export to .ics to sync with Google Calendar or Outlook.",
                            fontSize = 12.sp, color = colors.onPrimaryContainer,
                        )
                    }
                }

                // Error
                uiState.error?.let { error ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.errorContainer, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                    ) {
                        FgText(error, color = colors.error, fontSize = 13.sp)
                    }
                }

                // Add event form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp), ambientColor = FgElevation.cardShadowColor)
                        .background(colors.surface, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FgText("Add Event", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { FgText("Event title (e.g. CS Lecture, Team Meeting)", fontSize = 12.sp, color = colors.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = newStart,
                        onValueChange = { newStart = it },
                        label = { FgText("Start (2024-12-22T19:00:00)", fontSize = 12.sp, color = colors.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = newEnd,
                        onValueChange = { newEnd = it },
                        label = { FgText("End (2024-12-22T21:00:00)", fontSize = 12.sp, color = colors.onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface,
                        ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank() && newStart.isNotBlank() && newEnd.isNotBlank()) {
                                viewModel.addEvent(newTitle, newStart, newEnd)
                                newTitle = ""; newStart = ""; newEnd = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        elevation = ButtonDefaults.buttonElevation(4.dp),
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        FgText("Add to Calendar", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Upcoming events
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FgText("UPCOMING EVENTS", fontSize = 11.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                        FgText("${uiState.events.size} events", fontSize = 11.sp, color = colors.onSurfaceVariant)
                    }

                    if (uiState.isLoading) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp)
                        }
                    } else if (uiState.events.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceContainerLow, RoundedCornerShape(12.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            FgText("No events yet. Add your classes and meetings above.", fontSize = 13.sp, color = colors.onSurfaceVariant)
                        }
                    } else {
                        uiState.events.forEach { event ->
                            EventRow(event = event, onDelete = { viewModel.deleteEvent(event.eventId) })
                        }
                    }
                }

                // Export section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = FgElevation.cardShadowColor)
                        .background(colors.surface, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FgText("Export to Your Calendar App", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    FgText(
                        "Download your FocusGuard schedule as a .ics file and import it into Google Calendar, Outlook, or Apple Calendar.",
                        fontSize = 12.sp, color = colors.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExportStepChip("1. Sync schedule")
                        ExportStepChip("2. GET /calendar/export.ics")
                        ExportStepChip("3. Import .ics")
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEventDto, onDelete: () -> Unit) {
    val colors = FocusGuardTheme.colors
    val typeColor = when (event.eventType) {
        "focus_block" -> colors.primary
        "deadline"    -> colors.error
        else          -> colors.onSurfaceVariant
    }
    val typeLabel = when (event.eventType) {
        "focus_block" -> "Focus"
        "deadline"    -> "Deadline"
        else          -> "Event"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp), ambientColor = FgElevation.cardShadowColor)
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(typeColor, CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            FgText(event.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            FgText(
                "${event.startTime.take(16).replace("T", " ")} → ${event.endTime.take(16).replace("T", " ")}",
                fontSize = 11.sp, color = colors.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .background(typeColor.copy(0.1f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            FgText(typeLabel, fontSize = 10.sp, color = typeColor, fontWeight = FontWeight.SemiBold)
        }
        // Only user events can be deleted manually
        if (event.eventType == "user_event") {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.DeleteOutline, null, tint = colors.error.copy(0.7f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ExportStepChip(label: String) {
    val colors = FocusGuardTheme.colors
    Box(
        modifier = Modifier
            .background(colors.primaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        FgText(label, fontSize = 10.sp, color = colors.primary, fontWeight = FontWeight.SemiBold)
    }
}
