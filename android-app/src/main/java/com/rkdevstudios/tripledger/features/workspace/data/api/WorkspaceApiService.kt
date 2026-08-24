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

    @retrofit2.http.PUT("api/v1/workspaces/{id}")
    suspend fun updateWorkspace(
        @Path("id") workspaceId: String,
        @retrofit2.http.Body request: WorkspaceUpdateRequestDto
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

    @GET("api/v1/workspaces/{id}/expenses")
    suspend fun getExpenseTimeline(
        @Path("id") workspaceId: String
    ): NetworkResponse<ExpenseTimelineResponseDto>

    @retrofit2.http.POST("api/v1/workspaces/{id}/expenses")
    suspend fun createExpense(
        @Path("id") workspaceId: String,
        @retrofit2.http.Body request: CreateExpenseRequestDto
    ): NetworkResponse<ExpenseDto>

    @retrofit2.http.PUT("api/v1/workspaces/{id}/expenses/{expenseId}")
    suspend fun updateExpense(
        @Path("id") workspaceId: String,
        @Path("expenseId") expenseId: String,
        @retrofit2.http.Body request: UpdateExpenseRequestDto
    ): NetworkResponse<ExpenseDto>

    @retrofit2.http.HTTP(method = "DELETE", path = "api/v1/workspaces/{id}/expenses/{expenseId}", hasBody = true)
    suspend fun deleteExpense(
        @Path("id") workspaceId: String,
        @Path("expenseId") expenseId: String,
        @retrofit2.http.Body request: DeleteExpenseRequestDto
    ): NetworkResponse<Unit>

    @GET("api/v1/workspaces/{id}/balances")
    suspend fun getBalances(
        @Path("id") workspaceId: String
    ): NetworkResponse<BalancesResponseDto>

    @GET("api/v1/workspaces/{id}/settlements/plan")
    suspend fun getSettlementPlan(
        @Path("id") workspaceId: String
    ): NetworkResponse<SettlementPlanResponseDto>

    @retrofit2.http.POST("api/v1/workspaces/{id}/settlements/{transferId}/confirm")
    suspend fun confirmTransfer(
        @Path("id") workspaceId: String,
        @Path("transferId") transferId: String,
        @retrofit2.http.Body request: ConfirmSettlementRequestDto
    ): NetworkResponse<Unit>

    @GET("api/v1/workspaces/{id}/settlements/history")
    suspend fun getSettlementHistory(
        @Path("id") workspaceId: String
    ): NetworkResponse<SettlementHistoryResponseDto>
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

data class WorkspaceUpdateRequestDto(
    val name: String? = null,
    val description: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val budget: BigDecimal? = null,
    val plannedMemberCount: Int? = null,
    val status: String? = null,
    val contributionMode: String? = null
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
    val contributionMode: String = "COMBINED",
    val createdBy: String? = null
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
    @SerializedName("cashContributed") val cashContributed: BigDecimal? = BigDecimal.ZERO,
    @SerializedName("directExpenseContribution") val directExpenseContribution: BigDecimal? = BigDecimal.ZERO,
    @SerializedName("adjustments") val adjustments: BigDecimal? = BigDecimal.ZERO,
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

data class CreateExpenseRequestDto(
    val paidByUserId: String,
    val amount: BigDecimal,
    val currency: String,
    val description: String,
    val categoryId: String,
    val expenseDate: String,
    val splitType: String = "EQUAL",
    val participantIds: List<String>,
    val splitValues: Map<String, BigDecimal>? = null,
    val expenseAt: String? = null,
    val receiptUrl: String? = null,
    val note: String? = null
)

data class UpdateExpenseRequestDto(
    val amount: BigDecimal,
    val currency: String,
    val description: String,
    val categoryId: String,
    val expenseDate: String,
    val splitType: String = "EQUAL",
    val participantIds: List<String>,
    val splitValues: Map<String, BigDecimal>? = null,
    val reason: String
)

data class DeleteExpenseRequestDto(
    val reason: String
)

data class ExpenseDto(
    val id: String,
    val workspaceId: String,
    val paidByUserId: String,
    val description: String,
    val categoryId: String,
    val expenseDate: String,
    val expenseAt: String?,
    val receiptUrl: String?,
    val note: String?,
    val splitType: String = "EQUAL",
    val splitValues: Map<String, BigDecimal>? = null
)

data class SplitAllocationDto(
    val userId: String,
    val name: String,
    val amount: BigDecimal,
    val currency: String,
    val rawValue: BigDecimal
)

data class ExpenseTimelineItemDto(
    val id: String,
    val description: String,
    val amount: BigDecimal,
    val currency: String,
    val paidByUserId: String,
    val paidByName: String,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val expenseDate: String,
    val expenseAt: String?,
    val receiptUrl: String?,
    val note: String?,
    val splitType: String = "EQUAL",
    val splitAllocations: List<SplitAllocationDto> = emptyList(),
    val createdByUserId: String? = null
)

data class ExpenseTimelineGroupDto(
    val date: String,
    val expenses: List<ExpenseTimelineItemDto>
)

data class ExpenseTimelineResponseDto(
    val timeline: List<ExpenseTimelineGroupDto>
)

// Settlement and Balance DTOs
data class MemberBalanceResponseDto(
    val userId: String,
    val userName: String,
    val paid: BigDecimal,
    val owed: BigDecimal,
    val balance: BigDecimal
)

data class BalancesResponseDto(
    val balances: List<MemberBalanceResponseDto>
)

data class SettlementTransferResponseDto(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: MoneyDto
)

data class SettlementPlanResponseDto(
    val sessionId: String,
    val workspaceId: String,
    val transfers: List<SettlementTransferResponseDto>,
    val stateHash: String,
    val planVersion: Int
)

data class ConfirmSettlementRequestDto(
    val sessionId: String
)

data class SettlementHistoryItemDto(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: com.rkdevstudios.tripledger.features.workspace.data.api.MoneyDto,
    val confirmedAt: String
)

data class MoneyDto(
    val amount: BigDecimal,
    val currency: String
)

data class SettlementHistoryGroupDto(
    val date: String,
    val transactions: List<SettlementHistoryItemDto>
)

data class SettlementHistoryResponseDto(
    val history: List<SettlementHistoryGroupDto>
)

