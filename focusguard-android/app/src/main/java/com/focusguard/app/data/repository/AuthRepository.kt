package com.focusguard.app.data.repository

import com.focusguard.app.data.api.FocusGuardApiService
import com.focusguard.app.data.api.LoginRequest
import com.focusguard.app.data.api.RegisterRequest
import com.focusguard.app.data.local.SessionDataStore
import com.focusguard.app.domain.model.AuthResponse
import com.focusguard.app.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val code: Int? = null) : AuthResult<Nothing>()
}

@Singleton
class AuthRepository @Inject constructor(
    private val api: FocusGuardApiService,
    private val sessionDataStore: SessionDataStore,
) {
    val session: Flow<UserSession?> = sessionDataStore.session

    suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String? = null,
    ): AuthResult<AuthResponse> {
        return try {
            val response = api.register(RegisterRequest(name, email, password, phone))
            if (response.isSuccessful) {
                val dto = response.body()!!
                val authResponse = AuthResponse(dto.accessToken, dto.tokenType, dto.userId, dto.name, dto.email)
                sessionDataStore.saveSession(
                    UserSession(
                        userId = dto.userId,
                        accessToken = dto.accessToken,
                        name = dto.name.ifBlank { name },
                        email = dto.email.ifBlank { email },
                    )
                )
                AuthResult.Success(authResponse)
            } else {
                AuthResult.Error(
                    message = when (response.code()) {
                        409 -> "An account with this email already exists."
                        else -> "Registration failed. Please try again."
                    },
                    code = response.code()
                )
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun login(email: String, password: String): AuthResult<AuthResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val dto = response.body()!!
                val authResponse = AuthResponse(dto.accessToken, dto.tokenType, dto.userId, dto.name, dto.email)
                sessionDataStore.saveSession(
                    UserSession(
                        userId = dto.userId,
                        accessToken = dto.accessToken,
                        name = dto.name,
                        email = dto.email.ifBlank { email },
                    )
                )
                AuthResult.Success(authResponse)
            } else {
                AuthResult.Error(
                    message = when (response.code()) {
                        401 -> "Invalid email or password."
                        else -> "Login failed. Please try again."
                    },
                    code = response.code()
                )
            }
        } catch (e: Exception) {
            AuthResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun logout() = sessionDataStore.clearSession()
}
