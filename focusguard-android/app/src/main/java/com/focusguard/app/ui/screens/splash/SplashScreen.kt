package com.focusguard.app.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors

    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(20f) }

    val loaderAnim = rememberInfiniteTransition(label = "loader")
    val loaderX by loaderAnim.animateFloat(
        initialValue = -1f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "loaderX",
    )

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(600))
        offsetY.animateTo(0f, tween(600, easing = EaseOut))
        delay(1300)
        if (viewModel.isLoggedIn()) onNavigateToHome() else onNavigateToLogin()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(brush = FgGradients.backgroundBrush),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha.value).offset(y = offsetY.value.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .shadow(16.dp, RoundedCornerShape(26.dp), ambientColor = colors.shadowPrimary)
                    .background(FgGradients.primaryClay, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Shield, null, tint = Color.White, modifier = Modifier.size(46.dp))
            }
            Spacer(Modifier.height(24.dp))
            FgText("FocusGuard AI", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            FgText("Your AI-powered deadline guardian", fontSize = 14.sp, color = colors.onSurfaceVariant)
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .width(130.dp)
                .height(4.dp)
                .background(colors.primaryLight, RoundedCornerShape(2.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.38f)
                    .offset(x = (loaderX * 130).coerceIn(-130f, 130f).dp)
                    .background(FgGradients.primaryClay, RoundedCornerShape(2.dp)),
            )
        }

        FgText(
            text = "v3.0.0",
            fontSize = 10.sp,
            color = colors.onSurfaceMuted,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        )
    }
}
