package com.focusguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.ui.navigation.BottomNavRoute
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun BottomNavBar(
    currentRoute: BottomNavRoute,
    onNavigate: (BottomNavRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusGuardTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = colors.shadowPrimary)
                .background(colors.surface, RoundedCornerShape(24.dp))
                .border(1.dp, colors.outline, RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Filled.Home, "Home", currentRoute == BottomNavRoute.Home) { onNavigate(BottomNavRoute.Home) }
            NavItem(Icons.Filled.CalendarMonth, "Schedule", currentRoute == BottomNavRoute.Schedule) { onNavigate(BottomNavRoute.Schedule) }
            NavItem(Icons.Filled.Dashboard, "Dashboard", currentRoute == BottomNavRoute.Dashboard) { onNavigate(BottomNavRoute.Dashboard) }
            NavItem(Icons.Filled.Settings, "Settings", currentRoute == BottomNavRoute.Settings) { onNavigate(BottomNavRoute.Settings) }
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = FocusGuardTheme.colors

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) Color.White else colors.onSurfaceVariant,
        animationSpec = tween(200),
        label = "navTint",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isSelected) Modifier
                    .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = colors.shadowPrimary)
                    .background(FgGradients.primaryClay, RoundedCornerShape(16.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (isSelected) 16.dp else 12.dp, vertical = 8.dp)
            .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(20.dp))
            FgText(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = iconTint,
            )
        }
    }
}
