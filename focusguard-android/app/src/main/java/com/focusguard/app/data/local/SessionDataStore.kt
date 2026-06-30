package com.focusguard.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focusguard.app.domain.model.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
    private val KEY_NAME = stringPreferencesKey("name")
    private val KEY_EMAIL = stringPreferencesKey("email")

    val session: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        val userId = prefs[KEY_USER_ID] ?: return@map null
        val token = prefs[KEY_ACCESS_TOKEN] ?: return@map null
        UserSession(
            userId = userId,
            accessToken = token,
            name = prefs[KEY_NAME] ?: "",
            email = prefs[KEY_EMAIL] ?: "",
        )
    }

    /** One-shot read of the current session (null if logged out). */
    suspend fun currentSession(): UserSession? = session.first()

    suspend fun saveSession(session: UserSession) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = session.userId
            prefs[KEY_ACCESS_TOKEN] = session.accessToken
            prefs[KEY_NAME] = session.name
            prefs[KEY_EMAIL] = session.email
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    /** Returns "Bearer <token>" for use in API headers */
    suspend fun bearerToken(): String? {
        val token = session.first()?.accessToken
        return token?.let { "Bearer $it" }
    }
}
