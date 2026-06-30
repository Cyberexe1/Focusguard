package com.focusguard.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.repository.AuthRepository
import com.focusguard.app.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val registerSuccess: Boolean = false,
    val passwordStrength: Int = 0,  // 0=none, 1=weak, 2=medium, 3=strong
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, error = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, error = null) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = v) }
    fun onPasswordChange(v: String) = _uiState.update {
        it.copy(password = v, error = null, passwordStrength = calculateStrength(v))
    }

    private fun calculateStrength(password: String): Int = when {
        password.isEmpty() -> 0
        password.length < 5 -> 1
        password.length < 10 -> 2
        else -> 3
    }

    fun register() {
        val state = _uiState.value
        when {
            state.name.isBlank() -> { _uiState.update { it.copy(error = "Name is required.") }; return }
            state.email.isBlank() -> { _uiState.update { it.copy(error = "Email is required.") }; return }
            state.password.length < 8 -> { _uiState.update { it.copy(error = "Password must be at least 8 characters.") }; return }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.register(
                name = state.name.trim(),
                email = state.email.trim(),
                password = state.password,
                phone = state.phone.takeIf { it.isNotBlank() },
            )) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
                is AuthResult.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }
}
