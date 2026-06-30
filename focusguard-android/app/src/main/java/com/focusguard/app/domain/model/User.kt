package com.focusguard.app.domain.model

data class AuthResponse(
    val accessToken: String,
    val tokenType: String,
    val userId: String,
    val name: String = "",
    val email: String = "",
)

data class UserSession(
    val userId: String,
    val accessToken: String,
    val name: String = "",
    val email: String = "",
)
