package com.rkdevstudios.tripledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rkdevstudios.tripledger.core.auth.SessionManager
import com.rkdevstudios.tripledger.core.auth.SessionStore
import com.rkdevstudios.tripledger.features.auth.AuthViewModel
import com.rkdevstudios.tripledger.features.auth.data.AuthRepository
import com.rkdevstudios.tripledger.features.workspace.WorkspaceViewModel
import com.rkdevstudios.tripledger.features.workspace.PaymentProofViewModel
import com.rkdevstudios.tripledger.features.workspace.data.PaymentProofRepository
import com.rkdevstudios.tripledger.features.workspace.data.WorkspaceRepository

class ViewModelFactory(
    private val sessionStore: SessionStore,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val paymentProofRepository: PaymentProofRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(authRepository, sessionManager, sessionStore) as T
            }
            modelClass.isAssignableFrom(WorkspaceViewModel::class.java) -> {
                WorkspaceViewModel(workspaceRepository, sessionManager, paymentProofRepository) as T
            }
            modelClass.isAssignableFrom(PaymentProofViewModel::class.java) -> {
                PaymentProofViewModel(paymentProofRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
