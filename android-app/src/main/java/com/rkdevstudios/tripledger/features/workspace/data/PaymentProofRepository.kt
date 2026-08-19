package com.rkdevstudios.tripledger.features.workspace.data

import com.rkdevstudios.tripledger.features.workspace.data.api.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal

class PaymentProofRepository(
    private val workspaceApiService: WorkspaceApiService
) {
    private val cloudinaryApiService: CloudinaryApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.cloudinary.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(CloudinaryApiService::class.java)
    }

    suspend fun getUploadSignature(workspaceId: String, amount: BigDecimal): Result<PaymentSignatureResponse> {
        return try {
            val response = workspaceApiService.getUploadSignature(workspaceId, PaymentSignatureRequest(amount))
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to generate upload signature"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadImageToCloudinary(
        cloudName: String,
        fileBytes: ByteArray,
        apiKey: String,
        timestamp: Long,
        signature: String,
        publicId: String
    ): Result<String> {
        return try {
            val url = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
            
            val requestFile = fileBytes.toRequestBody("image/*".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData(
                "file",
                "receipt.png",
                requestFile
            )
            
            val apiKeyBody = apiKey.toRequestBody("text/plain".toMediaTypeOrNull())
            val timestampBody = timestamp.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val signatureBody = signature.toRequestBody("text/plain".toMediaTypeOrNull())
            val publicIdBody = publicId.toRequestBody("text/plain".toMediaTypeOrNull())
            val typeBody = "private".toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = cloudinaryApiService.uploadImage(
                url = url,
                file = filePart,
                apiKey = apiKeyBody,
                timestamp = timestampBody,
                signature = signatureBody,
                publicId = publicIdBody,
                type = typeBody
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.secure_url)
            } else {
                Result.failure(Exception("Cloudinary upload failed: ${response.errorBody()?.string() ?: response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeUpload(workspaceId: String, paymentId: String, publicId: String): Result<PaymentProofResponseDto> {
        return try {
            val response = workspaceApiService.completeUpload(workspaceId, PaymentCompletionRequest(paymentId, publicId))
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to complete upload"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPayments(workspaceId: String): Result<List<PaymentProofResponseDto>> {
        return try {
            val response = workspaceApiService.getPayments(workspaceId)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to list payments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approvePayment(workspaceId: String, paymentId: String): Result<PaymentProofResponseDto> {
        return try {
            val response = workspaceApiService.approvePayment(workspaceId, paymentId)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to approve payment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectPayment(workspaceId: String, paymentId: String, reason: String): Result<PaymentProofResponseDto> {
        return try {
            val response = workspaceApiService.rejectPayment(workspaceId, paymentId, PaymentRejectionRequest(reason))
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to reject payment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
