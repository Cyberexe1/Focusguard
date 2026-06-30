package com.focusguard.app.ui.screens.splash

import androidx.lifecycle.ViewModel
import com.focusguard.app.data.local.SessionDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionDataStore: SessionDataStore,
) : ViewModel() {

    /** Returns true if a saved session exists (user is logged in). */
    suspend fun isLoggedIn(): Boolean = sessionDataStore.currentSession() != null
}
