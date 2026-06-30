package com.focusguard.app.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.app.ui.components.FgInputField
import com.focusguard.app.ui.components.FgPrimaryButton
import com.focusguard.app.ui.components.FgText
import com.focusguard.app.ui.theme.FgGradients
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun RegisterScreen(
    onNavigateToHome: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val colors = FocusGuardTheme.colors
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.registerSuccess) {
        if (uiState.registerSuccess) onNavigateToHome()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = FgGradients.backgroundBrush),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.ArrowBack, null, tint = colors.primary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp),
        ) {
            FgText("Create Account", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = colors.onBackground)
            Spacer(Modifier.height(4.dp))
            FgText("Start protecting your deadlines", fontSize = 14.sp, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))

            FieldGroup("Full Name") {
                FgInputField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    placeholder = "John Doe",
                    leadingIcon = Icons.Filled.Person,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
            }
            Spacer(Modifier.height(18.dp))
            FieldGroup("Email Address") {
                FgInputField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    placeholder = "you@example.com",
                    leadingIcon = Icons.Filled.Email,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
            }
            Spacer(Modifier.height(18.dp))
            FieldGroup("Phone Number") {
                FgInputField(
                    value = uiState.phone,
                    onValueChange = viewModel::onPhoneChange,
                    placeholder = "+1 (555) 000-0000",
                    leadingIcon = Icons.Filled.Phone,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(4.dp))
                FgText("Used for AI accountability calls in Phase 3.", fontSize = 11.sp, color = colors.warning.copy(0.85f))
            }
            Spacer(Modifier.height(18.dp))
            FieldGroup("Password") {
                FgInputField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    placeholder = "Min. 8 characters",
                    leadingIcon = Icons.Filled.Lock,
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                )
                Spacer(Modifier.height(8.dp))
                PasswordStrength(uiState.passwordStrength)
            }

            uiState.error?.let {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.errorContainer, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                ) {
                    FgText(it, color = colors.error, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
            FgPrimaryButton("Create Account", onClick = viewModel::register, isLoading = uiState.isLoading)

            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                FgText("Already have an account? ", fontSize = 14.sp, color = colors.onSurfaceVariant)
                FgText(
                    "Sign In",
                    fontSize = 14.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onNavigateBack)
                        .padding(8.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun FieldGroup(label: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = FocusGuardTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FgText(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurfaceVariant)
        content()
    }
}

@Composable
private fun PasswordStrength(strength: Int) {
    val colors = FocusGuardTheme.colors
    val barColors = listOf(
        if (strength >= 1) colors.error else colors.outline,
        if (strength >= 2) colors.warning else colors.outline,
        if (strength >= 3) colors.success else colors.outline,
    )
    val label = when (strength) {
        1 -> "Weak password"
        2 -> "Medium — add symbols or numbers"
        3 -> "Strong password"
        else -> ""
    }
    val labelColor = when (strength) {
        1 -> colors.error; 2 -> colors.warning; 3 -> colors.success; else -> colors.outline
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        barColors.forEach { color ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(color, RoundedCornerShape(2.dp)),
            )
        }
    }
    if (strength > 0) {
        Spacer(Modifier.height(4.dp))
        FgText(label, fontSize = 11.sp, color = labelColor, fontWeight = FontWeight.Medium)
    }
}
