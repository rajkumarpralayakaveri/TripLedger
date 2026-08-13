package com.rkdevstudios.tripledger.core.auth

import java.time.Instant

data class AuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Instant?,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?
)
