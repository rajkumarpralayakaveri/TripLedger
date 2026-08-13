package com.rkdevstudios.tripledger.core.auth

import android.content.Context
import android.content.SharedPreferences
import java.time.Instant

class SharedPreferencesSessionStore(context: Context) : SessionStore {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override fun saveSession(session: AuthSession) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, session.accessToken)
            putString(KEY_REFRESH_TOKEN, session.refreshToken)
            putLong(KEY_EXPIRES_AT, session.expiresAt?.toEpochMilli() ?: 0L)
            putString(KEY_USER_ID, session.userId)
            putString(KEY_USER_NAME, session.userName)
            putString(KEY_USER_AVATAR, session.userAvatarUrl)
            apply()
        }
    }

    override fun getSession(): AuthSession? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, null)
        val expiresMs = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val userId = prefs.getString(KEY_USER_ID, "") ?: ""
        val name = prefs.getString(KEY_USER_NAME, "") ?: ""
        val avatar = prefs.getString(KEY_USER_AVATAR, null)

        val expiresAt = if (expiresMs > 0L) Instant.ofEpochMilli(expiresMs) else null

        return AuthSession(
            accessToken = token,
            refreshToken = refresh,
            expiresAt = expiresAt,
            userId = userId,
            userName = name,
            userAvatarUrl = avatar
        )
    }

    override fun isLoggedIn(): Boolean = getSession() != null

    override fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "tripledger_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_AVATAR = "user_avatar"
    }
}
