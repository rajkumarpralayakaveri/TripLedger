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
                        status = dto.status,
                        membersCount = 1 // Simplified placeholder, actual members fetched in detail snapshot
                    )
                }
                Result.success(list)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to load workspaces"))
            }
        } catch (e: Exception) {
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
}
