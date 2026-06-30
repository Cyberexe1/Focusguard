package com.focusguard.app.ui.screens.task

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.app.domain.model.Task
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.theme.FgElevation
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun AddTaskScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onTaskSaved: () -> Unit,
    viewModel: AddTaskViewModel = hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onTaskSaved()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.5f)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(20.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), ambientColor = FgElevation.cardShadowColor)
                .background(colors.surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .imePadding()
                .navigationBarsPadding(),
        ) {
            // Drag handle
            Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(36.dp).height(4.dp).background(colors.outline.copy(0.4f), RoundedCornerShape(2.dp)))
            }

            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FgText("Add New Task", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.onSurface)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.surfaceContainerLow, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Close, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Input area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainerLow, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.outline.copy(0.4f), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                ) {
                    TextField(
                        value = uiState.rawText,
                        onValueChange = viewModel::onRawTextChange,
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        placeholder = {
                            FgText("Describe your task... e.g. Submit hackathon before Sunday 2 PM", color = colors.onSurfaceVariant.copy(0.5f), fontSize = 14.sp)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onNavigateToVoice,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(colors.primary.copy(0.4f))),
                    ) {
                        Icon(Icons.Filled.Mic, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        FgText("Voice", color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = viewModel::analyzeTask,
                        enabled = !uiState.isAnalyzing,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        elevation = ButtonDefaults.buttonElevation(4.dp),
                    ) {
                        if (uiState.isAnalyzing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            FgText("AI Analyze", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                uiState.error?.let {
                    FgText(it, color = colors.error, fontSize = 13.sp)
                }

                uiState.analyzedTask?.let { task ->
                    AiTaskPreview(task)
                }
            }
        }

        // Save button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.surface)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Button(
                onClick = viewModel::confirmSave,
                enabled = uiState.analyzedTask != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    disabledContainerColor = colors.outline,
                ),
                elevation = ButtonDefaults.buttonElevation(6.dp),
            ) {
                FgText("Save Task", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AiTaskPreview(task: Task) {
    val colors = FocusGuardTheme.colors
    val scoreColor = when {
        task.priorityScore >= 80 -> colors.error
        task.priorityScore >= 50 -> colors.warning
        else -> colors.success
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(14.dp), ambientColor = colors.primary.copy(0.06f))
            .background(colors.surface, RoundedCornerShape(14.dp))
            .border(1.dp, colors.primary.copy(0.2f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.Verified, null, tint = colors.primary, modifier = Modifier.size(15.dp))
            FgText("AI ANALYSIS COMPLETE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.primary, letterSpacing = 0.8.sp)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FgText(task.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.CalendarToday, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(13.dp))
                    FgText(task.deadline.take(16), fontSize = 12.sp, color = colors.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(scoreColor.copy(0.08f), RoundedCornerShape(26.dp))
                        .border(2.dp, scoreColor.copy(0.3f), RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    FgText("${task.priorityScore}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = scoreColor)
                }
                FgText("Score", fontSize = 10.sp, color = scoreColor, fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider(color = colors.outline.copy(0.3f))

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FgText("Effort", fontSize = 11.sp, color = colors.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.HourglassEmpty, null, tint = colors.onSurface, modifier = Modifier.size(14.dp))
                    FgText("${task.effortHours}h", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FgText("Category", fontSize = 11.sp, color = colors.onSurfaceVariant)
                Box(
                    modifier = Modifier
                        .background(colors.primaryContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    FgText(task.category, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                }
            }
        }

        if (task.priorityRankReason.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scoreColor.copy(0.05f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Info, null, tint = scoreColor, modifier = Modifier.size(15.dp))
                FgText(task.priorityRankReason, fontSize = 12.sp, color = colors.onSurfaceVariant)
            }
        }
    }
}
