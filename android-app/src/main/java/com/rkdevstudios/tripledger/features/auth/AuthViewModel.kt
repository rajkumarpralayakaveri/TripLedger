package com.rkdevstudios.tripledger.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkdevstudios.tripledger.core.auth.AuthState
import com.rkdevstudios.tripledger.core.auth.SessionManager
import com.rkdevstudios.tripledger.core.auth.SessionStore
import com.rkdevstudios.tripledger.features.auth.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val session = sessionStore.getSession()
            if (session != null) {
                _authState.value = AuthState.Authenticated(session)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun loginWithEmail(email: String, authCode: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, authCode)
            result.fold(
                onSuccess = {
                    val session = sessionStore.getSession()
                    if (session != null) {
                        _authState.value = AuthState.Authenticated(session)
                    } else {
                        _authState.value = AuthState.Error("Session failed to save")
                    }
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Authentication failed")
                }
            )
        }
    }

    fun registerWithEmail(name: String, email: String, authCode: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(name, email, authCode)
            result.fold(
                onSuccess = {
                    val session = sessionStore.getSession()
                    if (session != null) {
                        _authState.value = AuthState.Authenticated(session)
                    } else {
                        _authState.value = AuthState.Error("Session failed to save")
                    }
                },
                onFailure = { error ->
                    _authState.value = AuthState.Error(error.message ?: "Registration failed")
                }
            )
        }
    }

    fun loginWithGoogle(idToken: String) {
        // Fallback or not implemented for Google login on backend yet
        _authState.value = AuthState.Error("Google Login is not supported in this release")
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Unauthenticated
        }
    }
}
