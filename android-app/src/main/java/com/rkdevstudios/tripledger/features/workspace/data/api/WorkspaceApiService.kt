package com.rkdevstudios.tripledger.features.workspace.data.api

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
}

data class WorkspaceCreateRequestDto(
    val name: String,
    val description: String?,
    val startDate: String,
    val endDate: String,
    val baseCurrency: String,
    val budget: BigDecimal?,
    val plannedMemberCount: Int,
    val contributionStrategy: String = "EQUAL"
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
    val status: String,
    val contributionStrategy: String
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
    val planned: BigDecimal,
    val total: BigDecimal,
    val remaining: BigDecimal,
    val status: String
)
