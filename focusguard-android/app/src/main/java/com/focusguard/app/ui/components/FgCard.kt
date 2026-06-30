package com.focusguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun FgCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    shadowColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = FocusGuardTheme.colors
    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(cornerRadius), ambientColor = shadowColor ?: colors.shadowCard)
            .background(colors.surface, RoundedCornerShape(cornerRadius))
            .border(1.dp, colors.outline, RoundedCornerShape(cornerRadius))
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun FgClayCard(
    modifier: Modifier = Modifier,
    gradient: Brush = com.focusguard.app.ui.theme.FgGradients.primaryClay,
    shadowColor: Color? = null,
    cornerRadius: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = FocusGuardTheme.colors
    Box(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(cornerRadius), ambientColor = shadowColor ?: colors.shadowPrimary)
            .background(gradient, RoundedCornerShape(cornerRadius))
            .padding(16.dp),
        content = content,
    )
}
