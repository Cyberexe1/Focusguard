package com.focusguard.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusguard.app.data.local.SessionDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val name: String = "",
    val email: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val s = sessionDataStore.currentSession()
            _uiState.update {
                it.copy(
                    name = s?.name?.ifBlank { "User" } ?: "User",
                    email = s?.email ?: "",
                )
            }
        }
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            sessionDataStore.clearSession()
            onDone()
        }
    }
}
