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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.ui.components.BottomNavBar
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.navigation.BottomNavRoute
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onSignOut: () -> Unit,
    onBottomNavClick: (BottomNavRoute) -> Unit,
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val uiState by viewModel.uiState.collectAsState()
    val displayName = uiState.name.ifBlank { "User" }
    val initial = displayName.firstOrNull()?.uppercase() ?: "U"
    var pushNotifications by remember { mutableStateOf(true) }
    var voiceAlerts by remember { mutableStateOf(true) }
    var escalationCalls by remember { mutableStateOf(false) }
    var autoGenerate by remember { mutableStateOf(true) }
    var emergencyDeploy by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier.fillMaxSize().background(brush = FgGradients.backgroundBrush),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.statusBarsPadding())

            // ── Profile hero ──────────────────────────────────────────────
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.White.copy(0.25f), CircleShape)
                            .border(2.dp, Color.White.copy(0.50f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        FgText(initial, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FgText(displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        FgText(uiState.email.ifBlank { "—" }, fontSize = 12.sp, color = Color.White.copy(0.70f))
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(0.20f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            FgText("Pro Member", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Color.White.copy(0.60f))
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsSection("Notifications") {
                    SettingsToggle(Icons.Filled.Notifications, FgGradients.primaryClay, "Push Notifications", null, pushNotifications) { pushNotifications = it }
                    SettingsDivider()
                    SettingsToggle(Icons.Filled.RecordVoiceOver, FgGradients.blueClay, "AI Voice Alerts", null, voiceAlerts) { voiceAlerts = it }
                    SettingsDivider()
                    SettingsToggle(Icons.Filled.EmergencyShare, FgGradients.errorClay, "Escalation Calls", "AI calls when risk > 90%", escalationCalls) { escalationCalls = it }
                }

                SettingsSection("Scheduling") {
                    SettingsToggle(Icons.Filled.AutoAwesome, FgGradients.successClay, "Auto-generate Sessions", null, autoGenerate) { autoGenerate = it }
                    SettingsDivider()
                    SettingsNavRow(Icons.Filled.Schedule, FgGradients.warningClay, "Preferred Hours", "7 PM – 10 PM") { }
                    SettingsDivider()
                    // Effort buffer
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(32.dp).background(FgGradients.warningClay, RoundedCornerShape(9.dp)), Alignment.Center) {
                                    Icon(Icons.Filled.BatteryChargingFull, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                                FgText("Effort Buffer", fontSize = 14.sp, color = colors.onSurface)
                            }
                            FgText("28%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(colors.primaryLight, RoundedCornerShape(3.dp))) {
                            Box(modifier = Modifier.fillMaxWidth(0.28f).height(5.dp).background(FgGradients.primaryClay, RoundedCornerShape(3.dp)))
                        }
                    }
                }

                SettingsSection("AI Behavior") {
                    SettingsToggle(Icons.Filled.Shield, FgGradients.primaryClay, "Emergency Auto-deploy", null, emergencyDeploy) { emergencyDeploy = it }
                }

                SettingsSection("Account") {
                    SettingsNavRow(Icons.Filled.Lock, FgGradients.blueClay, "Change Password", null) { }
                    SettingsDivider()
                    SettingsNavRow(Icons.Filled.CalendarToday, FgGradients.successClay, "Connected Calendars", "3 Active") { onNavigateToCalendar() }
                    SettingsDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(32.dp).background(FgGradients.errorClay, RoundedCornerShape(9.dp)), Alignment.Center) {
                            Icon(Icons.Filled.Logout, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                        TextButton(onClick = { viewModel.signOut(onSignOut) }, contentPadding = PaddingValues(0.dp)) {
                            FgText("Sign Out", fontSize = 15.sp, color = colors.error, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
            }
        }

        BottomNavBar(currentRoute = BottomNavRoute.Settings, onNavigate = onBottomNavClick, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = FocusGuardTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FgText(title.uppercase(), fontSize = 11.sp, color = colors.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(18.dp), ambientColor = colors.shadowCard)
                .background(colors.surface, RoundedCornerShape(18.dp))
                .border(1.dp, colors.outline, RoundedCornerShape(18.dp)),
            content = content,
        )
    }
}

@Composable
private fun SettingsDivider() {
    val colors = FocusGuardTheme.colors
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(color = colors.outline))
}

@Composable
private fun SettingsToggle(icon: ImageVector, gradient: Brush, label: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = FocusGuardTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(gradient, RoundedCornerShape(10.dp)), Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Column {
                FgText(label, fontSize = 14.sp, color = colors.onSurface, fontWeight = FontWeight.Medium)
                subtitle?.let { FgText(it, fontSize = 11.sp, color = colors.onSurfaceVariant) }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.primary,
                uncheckedTrackColor = colors.outline,
                uncheckedThumbColor = Color.White,
            ),
        )
    }
}

@Composable
private fun SettingsNavRow(icon: ImageVector, gradient: Brush, label: String, value: String?, onClick: () -> Unit) {
    val colors = FocusGuardTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(gradient, RoundedCornerShape(10.dp)), Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            FgText(label, fontSize = 14.sp, color = colors.onSurface, fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            value?.let { FgText(it, fontSize = 12.sp, color = colors.onSurfaceVariant) }
            Icon(Icons.Filled.ChevronRight, null, tint = colors.onSurfaceMuted, modifier = Modifier.size(16.dp))
        }
    }
}

