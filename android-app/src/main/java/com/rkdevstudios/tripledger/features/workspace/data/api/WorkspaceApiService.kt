package com.rkdevstudios.tripledger.features.workspace.data.api

import com.google.gson.annotations.SerializedName
import com.rkdevstudios.tripledger.core.network.NetworkResponse
import retrofit2.http.GET
import retrofit2.http.Path
import java.math.BigDecimal

interface WorkspaceApiService {

    @GET("api/v1/workspaces")
    suspend fun getWorkspaces(): NetworkResponse<List<WorkspaceDto>>

    @GET("api/v1/workspaces/{id}/financial-summary")
    suspend fun getFinancialSummary(
        @Path("id") workspaceId: String
    ): NetworkResponse<WorkspaceFinancialSnapshotDto>

    @retrofit2.http.POST("api/v1/workspaces/{id}/invite")
    suspend fun createInviteToken(
        @Path("id") workspaceId: String,
        @retrofit2.http.Body request: InviteRequestDto
    ): NetworkResponse<InviteTokenDto>

    @retrofit2.http.POST("api/v1/workspaces/join")
    suspend fun joinWorkspace(
        @retrofit2.http.Body request: JoinRequestDto
    ): NetworkResponse<WorkspaceMemberDto>

    @retrofit2.http.POST("api/v1/workspaces")
    suspend fun createWorkspace(
        @retrofit2.http.Body request: WorkspaceCreateRequestDto
    ): NetworkResponse<WorkspaceDto>

    @retrofit2.http.POST("api/v1/workspaces/{id}/payments/signature")
    suspend fun getUploadSignature(
        @Path("id") workspaceId: String,
        @retrofit2.http.Body request: PaymentSignatureRequest
    ): NetworkResponse<PaymentSignatureResponse>

    @retrofit2.http.POST("api/v1/workspaces/{id}/payments/complete")
    suspend fun completeUpload(
        @Path("id") workspaceId: String,
        @retrofit2.http.Body request: PaymentCompletionRequest
    ): NetworkResponse<PaymentProofResponseDto>

    @GET("api/v1/workspaces/{id}/payments")
    suspend fun getPayments(
        @Path("id") workspaceId: String
    ): NetworkResponse<List<PaymentProofResponseDto>>

    @retrofit2.http.POST("api/v1/workspaces/{id}/payments/{paymentId}/approve")
    suspend fun approvePayment(
        @Path("id") workspaceId: String,
        @Path("paymentId") paymentId: String
    ): NetworkResponse<PaymentProofResponseDto>

    @retrofit2.http.POST("api/v1/workspaces/{id}/payments/{paymentId}/reject")
    suspend fun rejectPayment(
        @Path("id") workspaceId: String,
        @Path("paymentId") paymentId: String,
        @retrofit2.http.Body request: PaymentRejectionRequest
    ): NetworkResponse<PaymentProofResponseDto>

    @retrofit2.http.DELETE("api/v1/workspaces/{id}/members/{userId}")
    suspend fun removeMember(
        @Path("id") workspaceId: String,
        @Path("userId") userId: String
    ): NetworkResponse<Unit>

    @retrofit2.http.PUT("api/v1/workspaces/{id}/contributions/planned/{userId}")
    suspend fun updateMemberPlannedContribution(
        @Path("id") workspaceId: String,
        @Path("userId") userId: String,
        @retrofit2.http.Body request: UpdatePlannedContributionRequestDto
    ): NetworkResponse<Unit>

    @retrofit2.http.PUT("api/v1/workspaces/{id}/members/{userId}/role")
    suspend fun updateMemberRole(
        @Path("id") workspaceId: String,
        @Path("userId") userId: String,
        @retrofit2.http.Body request: UpdateMemberRoleRequestDto
    ): NetworkResponse<WorkspaceMemberDto>

    @retrofit2.http.POST("api/v1/workspaces/{id}/leave")
    suspend fun leaveWorkspace(
        @Path("id") workspaceId: String
    ): NetworkResponse<Unit>
}

data class UpdateMemberRoleRequestDto(
    val role: String
)

data class WorkspaceCreateRequestDto(
    val name: String,
    val description: String?,
    val startDate: String,
    val endDate: String,
    val baseCurrency: String,
    val budget: BigDecimal?,
    val plannedMemberCount: Int,
    val contributionStrategy: String = "EQUAL",
    val contributionMode: String = "COMBINED"
)

data class UpdatePlannedContributionRequestDto(
    val plannedAmount: BigDecimal
)

data class JoinRequestDto(
    val inviteToken: String
)

data class InviteRequestDto(
    val maxUses: Int = 5,
    val expirationSeconds: Long = 86400L
)

data class InviteTokenDto(
    val token: String,
    val workspaceId: String,
    val expiresAt: String?,
    val active: Boolean
)

data class WorkspaceMemberDto(
    val workspaceId: String,
    val userId: String,
    val role: String
)

data class WorkspaceDto(
    val id: String,
    val name: String,
    val description: String?,
    val startDate: String,
    val endDate: String,
    val baseCurrency: String,
    val budget: BigDecimal?,
    val plannedMemberCount: Int,
    val memberCount: Int,
    val status: String,
    val contributionStrategy: String,
    val contributionMode: String = "COMBINED"
)

data class WorkspaceFinancialSnapshotDto(
    val workspaceId: String,
    val totalBudget: BigDecimal,
    val totalSpent: BigDecimal,
    val remainingBudget: BigDecimal,
    val currentFund: BigDecimal,
    val fundingGap: BigDecimal,
    val memberCount: Int,
    val fundedMembers: Int,
    val pendingMembers: Int,
    val overFundedMembers: Int,
    val memberContributions: List<ContributionSummaryDto>
)

data class ContributionSummaryDto(
    val userId: String,
    val name: String,
    val role: String,
    @SerializedName("plannedContribution") val planned: BigDecimal,
    @SerializedName("totalContribution") val total: BigDecimal,
    @SerializedName("remainingContribution") val remaining: BigDecimal,
    val status: String
)

data class PaymentSignatureRequest(
    val amount: BigDecimal
)

data class PaymentSignatureResponse(
    val paymentId: String,
    val publicId: String,
    val signature: String,
    val timestamp: Long,
    val apiKey: String,
    val cloudName: String
)

data class PaymentCompletionRequest(
    val paymentId: String,
    val publicId: String
)

data class PaymentRejectionRequest(
    val reason: String
)

data class PaymentProofResponseDto(
    val id: String,
    val workspaceId: String,
    val userId: String,
    val payerName: String,
    val amount: BigDecimal,
    val status: String,
    val createdAt: String,
    val submittedAt: String?,
    val verifiedAt: String?,
    val verifiedBy: String?,
    val rejectionReason: String?,
    val viewUrl: String?
)

interface CloudinaryApiService {
    @retrofit2.http.Multipart
    @retrofit2.http.POST
    suspend fun uploadImage(
        @retrofit2.http.Url url: String,
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part,
        @retrofit2.http.Part("api_key") apiKey: okhttp3.RequestBody,
        @retrofit2.http.Part("timestamp") timestamp: okhttp3.RequestBody,
        @retrofit2.http.Part("signature") signature: okhttp3.RequestBody,
        @retrofit2.http.Part("public_id") publicId: okhttp3.RequestBody,
        @retrofit2.http.Part("type") type: okhttp3.RequestBody
    ): retrofit2.Response<CloudinaryUploadResponseDto>
}

data class CloudinaryUploadResponseDto(
    val public_id: String,
    val secure_url: String
)
