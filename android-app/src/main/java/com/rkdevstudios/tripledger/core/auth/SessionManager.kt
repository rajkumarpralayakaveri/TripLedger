package com.rkdevstudios.tripledger.core.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

class SessionManager(private val sessionStore: SessionStore) {

    private val _isLoggedIn = MutableStateFlow(sessionStore.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun getAccessToken(): String? {
        return sessionStore.getSession()?.accessToken
    }

    fun getRefreshToken(): String? {
        return sessionStore.getSession()?.refreshToken
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        userName: String,
        avatarUrl: String?
    ) {
        val session = AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = Instant.now().plusSeconds(3600), // Access tokens expire in 1 hour
            userId = userId,
            userName = userName,
            userAvatarUrl = avatarUrl
        )
        sessionStore.saveSession(session)
        _isLoggedIn.value = true
    }

    fun updateAccessToken(newAccessToken: String, newRefreshToken: String) {
        val currentSession = sessionStore.getSession() ?: return
        val updatedSession = currentSession.copy(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresAt = Instant.now().plusSeconds(3600)
        )
        sessionStore.saveSession(updatedSession)
        _isLoggedIn.value = true
    }

    fun clearSession() {
        sessionStore.clearSession()
        _isLoggedIn.value = false
    }
}
