package com.focusguard.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun FgPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    gradient: Brush = FgGradients.primaryClay,
) {
    val colors = FocusGuardTheme.colors
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "btnScale",
        finishedListener = { pressed = false },
    )

    Surface(
        onClick = { if (!isLoading && enabled) { pressed = true; onClick() } },
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .shadow(
                elevation = if (enabled && !isLoading) 10.dp else 2.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = if (enabled) colors.shadowPrimary else Color.Transparent,
            ),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (enabled && !isLoading) gradient else Brush.linearGradient(listOf(Color(0xFFD1D5DB), Color(0xFF9CA3AF)))),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                FgText(text = text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
