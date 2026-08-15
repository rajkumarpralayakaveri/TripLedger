package com.rkdevstudios.tripledger.features.workspace.data

import com.rkdevstudios.tripledger.features.workspace.MockContributionSummary
import com.rkdevstudios.tripledger.features.workspace.MockFinancialSnapshot
import com.rkdevstudios.tripledger.features.workspace.MockWorkspace
import com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceApiService
import java.time.LocalDate

class WorkspaceRepository(private val workspaceApiService: WorkspaceApiService) {

    suspend fun getWorkspaces(): Result<List<MockWorkspace>> {
        return try {
            val response = workspaceApiService.getWorkspaces()
            if (response.success && response.data != null) {
                val list = response.data.map { dto ->
                    MockWorkspace(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        startDate = LocalDate.parse(dto.startDate),
                        endDate = LocalDate.parse(dto.endDate),
                        baseCurrency = dto.baseCurrency,
                        budget = dto.budget,
                        plannedMemberCount = dto.plannedMemberCount,
                        status = dto.status,
                        membersCount = dto.memberCount
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to load workspaces"))
            }
        } catch (e: Exception) {
            android.util.Log.e("WorkspaceRepository", "getWorkspaces request failed", e)
            Result.failure(e)
        }
    }

    suspend fun getFinancialSummary(workspaceId: String): Result<MockFinancialSnapshot> {
        return try {
            val response = workspaceApiService.getFinancialSummary(workspaceId)
            if (response.success && response.data != null) {
                val dto = response.data
                val contributions = dto.memberContributions.map { c ->
                    MockContributionSummary(
                        userId = c.userId,
                        name = c.name,
                        role = c.role,
                        planned = c.planned,
                        total = c.total,
                        remaining = c.remaining,
                        status = c.status
                    )
                }
                val snapshot = MockFinancialSnapshot(
                    budget = dto.totalBudget,
                    spent = dto.totalSpent,
                    currentFund = dto.currentFund,
                    fundingGap = dto.fundingGap,
                    memberCount = dto.memberCount,
                    fundedMembers = dto.fundedMembers,
                    pendingMembers = dto.pendingMembers,
                    contributions = contributions
                )
                Result.success(snapshot)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to load financial summary"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun joinWorkspace(inviteToken: String): Result<Unit> {
        return try {
            val response = workspaceApiService.joinWorkspace(com.rkdevstudios.tripledger.features.workspace.data.api.JoinRequestDto(inviteToken))
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to join workspace"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createInviteToken(workspaceId: String): Result<String> {
        return try {
            val response = workspaceApiService.createInviteToken(
                workspaceId = workspaceId,
                request = com.rkdevstudios.tripledger.features.workspace.data.api.InviteRequestDto()
            )
            if (response.success && response.data != null) {
                Result.success(response.data.token)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to create invite token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createWorkspace(
        name: String,
        description: String?,
        startDate: LocalDate,
        endDate: LocalDate,
        baseCurrency: String,
        budget: java.math.BigDecimal?,
        plannedMemberCount: Int
    ): Result<MockWorkspace> {
        return try {
            val response = workspaceApiService.createWorkspace(
                com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceCreateRequestDto(
                    name = name,
                    description = description,
                    startDate = startDate.toString(),
                    endDate = endDate.toString(),
                    baseCurrency = baseCurrency,
                    budget = budget,
                    plannedMemberCount = plannedMemberCount
                )
            )
            if (response.success && response.data != null) {
                val dto = response.data
                Result.success(
                    MockWorkspace(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description,
                        startDate = LocalDate.parse(dto.startDate),
                        endDate = LocalDate.parse(dto.endDate),
                        baseCurrency = dto.baseCurrency,
                        budget = dto.budget,
                        plannedMemberCount = dto.plannedMemberCount,
                        status = dto.status,
                        membersCount = dto.memberCount
                    )
                )
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to create workspace"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
