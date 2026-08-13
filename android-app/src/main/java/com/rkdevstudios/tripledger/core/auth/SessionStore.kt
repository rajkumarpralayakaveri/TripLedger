package com.rkdevstudios.tripledger.core.auth

interface SessionStore {
    fun saveSession(session: AuthSession)
    fun getSession(): AuthSession?
    fun isLoggedIn(): Boolean
    fun clearSession()
}
