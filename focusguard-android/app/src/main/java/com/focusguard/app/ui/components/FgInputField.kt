package com.focusguard.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.app.ui.theme.FocusGuardTheme

@Composable
fun FgInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
) {
    val colors = FocusGuardTheme.colors
    var isFocused by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) colors.primary else colors.outline,
        animationSpec = tween(200),
        label = "inputBorder",
    )

    val visualTransformation = if (isPassword && !passwordVisible)
        PasswordVisualTransformation() else VisualTransformation.None

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = if (isFocused) 8.dp else 2.dp,
                shape = RoundedCornerShape(13.dp),
                ambientColor = if (isFocused) colors.shadowPrimary else colors.shadowCard,
            )
            .background(colors.surface, RoundedCornerShape(13.dp))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(13.dp),
            )
            .padding(horizontal = 14.dp)
            .onFocusChanged { isFocused = it.isFocused },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isFocused) colors.primary else colors.onSurfaceMuted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Normal),
                cursorBrush = SolidColor(colors.primary),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                singleLine = true,
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        FgText(text = placeholder, color = colors.onSurfaceMuted, fontSize = 15.sp)
                    }
                    inner()
                },
            )
            if (isPassword) {
                IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = colors.onSurfaceMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else if (trailingIcon != null && onTrailingIconClick != null) {
                IconButton(onClick = onTrailingIconClick, modifier = Modifier.size(36.dp)) {
                    Icon(trailingIcon, null, tint = colors.onSurfaceMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
