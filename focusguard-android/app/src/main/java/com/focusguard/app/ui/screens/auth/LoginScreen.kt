package com.focusguard.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.app.ui.components.FgInputField
import com.focusguard.app.ui.components.FgPrimaryButton
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = FgGradients.backgroundBrush),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.statusBarsPadding())
            Spacer(Modifier.height(48.dp))

            // Logo mark
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = colors.shadowPrimary)
                    .background(FgGradients.primaryClay, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Shield, null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(14.dp))
            FgText("FocusGuard AI", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.onBackground)

            Spacer(Modifier.height(44.dp))

            // Section heading — left aligned
            Column(modifier = Modifier.fillMaxWidth()) {
                FgText("Welcome back", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = colors.onBackground)
                Spacer(Modifier.height(4.dp))
                FgText("Sign in to continue", fontSize = 14.sp, color = colors.onSurfaceVariant)
            }

            Spacer(Modifier.height(28.dp))

            // Fields
            FgInputField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = "Email address",
                leadingIcon = Icons.Filled.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(14.dp))
            FgInputField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "Password",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            )

            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                FgText(
                    "Forgot password?",
                    color = colors.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .wrapContentSize(),
                )
            }

            // Error banner
            uiState.error?.let { error ->
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.errorContainer, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    FgText(error, color = colors.error, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.height(28.dp))
            FgPrimaryButton("Sign In", onClick = viewModel::login, isLoading = uiState.isLoading)

            Spacer(Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FgText("Don't have an account? ", fontSize = 14.sp, color = colors.onSurfaceVariant)
                FgText(
                    "Sign Up",
                    fontSize = 14.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onNavigateToRegister)
                        .padding(8.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
