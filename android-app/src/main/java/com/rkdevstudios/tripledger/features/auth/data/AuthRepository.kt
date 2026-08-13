package com.rkdevstudios.tripledger.features.auth.data

import com.rkdevstudios.tripledger.core.auth.SessionManager
import com.rkdevstudios.tripledger.features.auth.data.api.AuthApiService
import com.rkdevstudios.tripledger.features.auth.data.api.LoginRequestDto
import com.rkdevstudios.tripledger.features.auth.data.api.LogoutRequestDto

class AuthRepository(
    private val authApiService: AuthApiService,
    private val sessionManager: SessionManager
) {
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val response = authApiService.login(LoginRequestDto(email, password))
            if (response.success && response.data != null) {
                val data = response.data
                sessionManager.saveSession(
                    accessToken = data.accessToken,
                    refreshToken = data.refreshToken,
                    userId = data.user.id,
                    userName = data.user.name,
                    avatarUrl = data.user.avatarUrl
                )
                Result.success(Unit)
            } else {
                val errMsg = response.error?.message ?: "Login failed"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        val refreshToken = sessionManager.getRefreshToken()
        sessionManager.clearSession()
        if (!refreshToken.isNullOrBlank()) {
            try {
                authApiService.logout(LogoutRequestDto(refreshToken))
            } catch (e: Exception) {
                // Ignore API logout errors since local state is already cleared
            }
        }
        return Result.success(Unit)
    }
}
