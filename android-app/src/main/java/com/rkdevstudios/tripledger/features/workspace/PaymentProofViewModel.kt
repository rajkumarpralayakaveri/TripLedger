package com.rkdevstudios.tripledger.features.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkdevstudios.tripledger.features.workspace.data.PaymentProofRepository
import com.rkdevstudios.tripledger.features.workspace.data.api.PaymentProofResponseDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

class PaymentProofViewModel(
    private val paymentProofRepository: PaymentProofRepository
) : ViewModel() {

    private val _payments = MutableStateFlow<List<PaymentProofResponseDto>>(emptyList())
    val payments: StateFlow<List<PaymentProofResponseDto>> = _payments.asStateFlow()

    private val _isRequestingSignature = MutableStateFlow(false)
    val isRequestingSignature: StateFlow<Boolean> = _isRequestingSignature.asStateFlow()

    private val _isUploadingToCloudinary = MutableStateFlow(false)
    val isUploadingToCloudinary: StateFlow<Boolean> = _isUploadingToCloudinary.asStateFlow()

    private val _isCompletingUpload = MutableStateFlow(false)
    val isCompletingUpload: StateFlow<Boolean> = _isCompletingUpload.asStateFlow()

    private val _isVerifyingPayment = MutableStateFlow(false)
    val isVerifyingPayment: StateFlow<Boolean> = _isVerifyingPayment.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadPayments(workspaceId: String, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            }
            paymentProofRepository.getPayments(workspaceId)
                .onSuccess { list ->
                    _payments.value = list
                    _errorMessage.value = null
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to load payment proofs"
                }
            _isRefreshing.value = false
        }
    }

    fun submitPayment(workspaceId: String, amount: BigDecimal, fileBytes: ByteArray, onSuccess: () -> Unit) {
        if (_isRequestingSignature.value || _isUploadingToCloudinary.value || _isCompletingUpload.value) return
        
        viewModelScope.launch {
            _errorMessage.value = null
            
            _isRequestingSignature.value = true
            val signatureRes = paymentProofRepository.getUploadSignature(workspaceId, amount)
            _isRequestingSignature.value = false

            signatureRes.onSuccess { sig ->
                _isUploadingToCloudinary.value = true
                val uploadRes = paymentProofRepository.uploadImageToCloudinary(
                    cloudName = sig.cloudName,
                    fileBytes = fileBytes,
                    apiKey = sig.apiKey,
                    timestamp = sig.timestamp,
                    signature = sig.signature,
                    publicId = sig.publicId
                )
                _isUploadingToCloudinary.value = false

                uploadRes.onSuccess { secureUrl ->
                    _isCompletingUpload.value = true
                    val completeRes = paymentProofRepository.completeUpload(
                        workspaceId = workspaceId,
                        paymentId = sig.paymentId,
                        publicId = sig.publicId
                    )
                    _isCompletingUpload.value = false

                    completeRes.onSuccess {
                        loadPayments(workspaceId)
                        onSuccess()
                    }.onFailure { error ->
                        _errorMessage.value = error.message ?: "Server complete verification failed"
                    }
                }.onFailure { error ->
                    _errorMessage.value = error.message ?: "Cloudinary image upload failed"
                }
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Signature generation failed"
            }
        }
    }

    fun approvePayment(workspaceId: String, paymentId: String) {
        if (_isVerifyingPayment.value) return
        viewModelScope.launch {
            _isVerifyingPayment.value = true
            _errorMessage.value = null
            paymentProofRepository.approvePayment(workspaceId, paymentId)
                .onSuccess {
                    loadPayments(workspaceId)
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to approve payment"
                }
            _isVerifyingPayment.value = false
        }
    }

    fun rejectPayment(workspaceId: String, paymentId: String, reason: String, onDialogDismiss: () -> Unit) {
        if (_isVerifyingPayment.value) return
        if (reason.isBlank()) {
            _errorMessage.value = "Rejection reason is required"
            return
        }
        viewModelScope.launch {
            _isVerifyingPayment.value = true
            _errorMessage.value = null
            paymentProofRepository.rejectPayment(workspaceId, paymentId, reason)
                .onSuccess {
                    loadPayments(workspaceId)
                    onDialogDismiss()
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "Failed to reject payment"
                }
            _isVerifyingPayment.value = false
        }
    }
}
