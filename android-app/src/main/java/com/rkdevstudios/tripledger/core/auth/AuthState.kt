package com.rkdevstudios.tripledger.core.auth

sealed interface AuthState {
    object Loading : AuthState
    data class Authenticated(val session: AuthSession) : AuthState
    object Unauthenticated : AuthState
    object Expired : AuthState
    data class Error(val message: String) : AuthState
}
