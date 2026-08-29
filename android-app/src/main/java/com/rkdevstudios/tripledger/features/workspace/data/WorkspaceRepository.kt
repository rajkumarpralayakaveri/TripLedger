package com.rkdevstudios.tripledger.features.workspace.data

import com.rkdevstudios.tripledger.features.expense.domain.ExpenseItem
import com.rkdevstudios.tripledger.features.expense.domain.ExpenseTimelineGroup
import com.rkdevstudios.tripledger.features.workspace.MockContributionSummary
import com.rkdevstudios.tripledger.features.workspace.MockFinancialSnapshot
import com.rkdevstudios.tripledger.features.workspace.MockWorkspace
import com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceApiService
import java.math.BigDecimal
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
                        membersCount = dto.memberCount,
                        contributionMode = dto.contributionMode,
                        createdBy = dto.createdBy
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
                        status = c.status,
                        fronted = c.directExpenseContribution ?: BigDecimal.ZERO
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
    suspend fun joinWorkspace(inviteToken: String): Result<String> {
        return try {
            val response = workspaceApiService.joinWorkspace(com.rkdevstudios.tripledger.features.workspace.data.api.JoinRequestDto(inviteToken))
            if (response.success && response.data != null) {
                Result.success(response.data.workspaceId)
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
        plannedMemberCount: Int,
        contributionMode: String = "COMBINED"
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
                    plannedMemberCount = plannedMemberCount,
                    contributionMode = contributionMode
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
                        membersCount = dto.memberCount,
                        contributionMode = dto.contributionMode,
                        createdBy = dto.createdBy
                    )
                )
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to create workspace"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMember(workspaceId: String, userId: String): Result<Unit> {
        return try {
            val response = workspaceApiService.removeMember(workspaceId, userId)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to remove member"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMemberPlannedContribution(workspaceId: String, userId: String, plannedAmount: java.math.BigDecimal): Result<Unit> {
        return try {
            val response = workspaceApiService.updateMemberPlannedContribution(
                workspaceId,
                userId,
                com.rkdevstudios.tripledger.features.workspace.data.api.UpdatePlannedContributionRequestDto(plannedAmount)
            )
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to update planned contribution"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMemberRole(workspaceId: String, userId: String, role: String): Result<Unit> {
        return try {
            val response = workspaceApiService.updateMemberRole(
                workspaceId,
                userId,
                com.rkdevstudios.tripledger.features.workspace.data.api.UpdateMemberRoleRequestDto(role)
            )
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to update member role"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveWorkspace(workspaceId: String): Result<Unit> {
        return try {
            val response = workspaceApiService.leaveWorkspace(workspaceId)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to leave workspace"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExpenseTimeline(workspaceId: String): Result<List<ExpenseTimelineGroup>> {
        return try {
            val response = workspaceApiService.getExpenseTimeline(workspaceId)
            if (response.success && response.data != null) {
                val groups = response.data.timeline.map { groupDto ->
                    ExpenseTimelineGroup(
                        date = try { LocalDate.parse(groupDto.date) } catch (e: Exception) { LocalDate.now() },
                        expenses = groupDto.expenses.map { itemDto ->
                            ExpenseItem(
                                id = itemDto.id,
                                description = itemDto.description,
                                amount = itemDto.amount,
                                currency = itemDto.currency,
                                paidByUserId = itemDto.paidByUserId,
                                paidByName = itemDto.paidByName,
                                categoryId = itemDto.categoryId,
                                categoryName = itemDto.categoryName,
                                categoryIcon = itemDto.categoryIcon,
                                categoryColor = itemDto.categoryColor,
                                date = try { LocalDate.parse(itemDto.expenseDate) } catch (e: Exception) { LocalDate.now() },
                                expenseAt = itemDto.expenseAt,
                                receiptUrl = itemDto.receiptUrl,
                                note = itemDto.note,
                                splitType = itemDto.splitType,
                                splitAllocations = itemDto.splitAllocations.map {
                                    com.rkdevstudios.tripledger.features.expense.domain.SplitAllocationItem(
                                        userId = it.userId,
                                        name = it.name,
                                        amount = it.amount,
                                        currency = it.currency,
                                        rawValue = it.rawValue
                                    )
                                },
                                createdByUserId = itemDto.createdByUserId ?: ""
                            )
                        }
                    )
                }
                Result.success(groups)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to load expense timeline"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createExpense(
        workspaceId: String,
        paidByUserId: String,
        amount: BigDecimal,
        currency: String,
        description: String,
        categoryId: String,
        expenseDate: LocalDate,
        participantIds: List<String>,
        expenseAt: String? = null,
        receiptUrl: String? = null,
        note: String? = null,
        splitType: String = "EQUAL",
        splitValues: Map<String, BigDecimal>? = null
    ): Result<Unit> {
        return try {
            val request = com.rkdevstudios.tripledger.features.workspace.data.api.CreateExpenseRequestDto(
                paidByUserId = paidByUserId,
                amount = amount,
                currency = currency,
                description = description,
                categoryId = categoryId,
                expenseDate = expenseDate.toString(),
                splitType = splitType,
                participantIds = participantIds,
                splitValues = splitValues,
                expenseAt = expenseAt,
                receiptUrl = receiptUrl,
                note = note
            )
            val response = workspaceApiService.createExpense(workspaceId, request)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to create expense"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateExpense(
        workspaceId: String,
        expenseId: String,
        amount: BigDecimal,
        currency: String,
        description: String,
        categoryId: String,
        expenseDate: LocalDate,
        splitType: String,
        participantIds: List<String>,
        splitValues: Map<String, BigDecimal>?,
        reason: String
    ): Result<Unit> {
        return try {
            val request = com.rkdevstudios.tripledger.features.workspace.data.api.UpdateExpenseRequestDto(
                amount = amount,
                currency = currency,
                description = description,
                categoryId = categoryId,
                expenseDate = expenseDate.toString(),
                splitType = splitType,
                participantIds = participantIds,
                splitValues = splitValues,
                reason = reason
            )
            val response = workspaceApiService.updateExpense(workspaceId, expenseId, request)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to update expense"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(
        workspaceId: String,
        expenseId: String,
        reason: String
    ): Result<Unit> {
        return try {
            val request = com.rkdevstudios.tripledger.features.workspace.data.api.DeleteExpenseRequestDto(
                reason = reason
            )
            val response = workspaceApiService.deleteExpense(workspaceId, expenseId, request)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to delete expense"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWorkspace(
        workspaceId: String,
        name: String? = null,
        description: String? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        budget: BigDecimal? = null,
        plannedMemberCount: Int? = null,
        status: String? = null,
        contributionMode: String? = null
    ): Result<MockWorkspace> {
        return try {
            val request = com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceUpdateRequestDto(
                name = name,
                description = description,
                startDate = startDate?.toString(),
                endDate = endDate?.toString(),
                budget = budget,
                plannedMemberCount = plannedMemberCount,
                status = status,
                contributionMode = contributionMode
            )
            val response = workspaceApiService.updateWorkspace(workspaceId, request)
            if (response.success && response.data != null) {
                val dto = response.data
                Result.success(
                    MockWorkspace(
                        id = dto.id,
                        name = dto.name,
                        description = dto.description ?: "",
                        startDate = LocalDate.parse(dto.startDate),
                        endDate = LocalDate.parse(dto.endDate),
                        baseCurrency = dto.baseCurrency,
                        budget = dto.budget ?: BigDecimal.ZERO,
                        plannedMemberCount = dto.plannedMemberCount,
                        status = dto.status,
                        membersCount = dto.memberCount,
                        contributionMode = dto.contributionMode,
                        createdBy = dto.createdBy
                    )
                )
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to update workspace"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveWorkspace(workspaceId: String): Result<Unit> {
        return try {
            val request = com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceUpdateRequestDto(
                status = "ARCHIVED"
            )
            val response = workspaceApiService.updateWorkspace(workspaceId, request)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to archive workspace"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBalances(workspaceId: String): Result<List<com.rkdevstudios.tripledger.features.settlement.domain.MemberBalanceItem>> {
        return try {
            val response = workspaceApiService.getBalances(workspaceId)
            if (response.success && response.data != null) {
                val mapped = response.data.balances.map { dto ->
                    com.rkdevstudios.tripledger.features.settlement.domain.MemberBalanceItem(
                        userId = dto.userId,
                        userName = dto.userName,
                        paid = dto.paid.amount,
                        owed = dto.owed.amount,
                        balance = dto.balance.amount
                    )
                }
                Result.success(mapped)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to fetch balances"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSettlementPlan(workspaceId: String, currency: String = "INR"): Result<com.rkdevstudios.tripledger.features.settlement.domain.SettlementPlanItem> {
        return try {
            System.out.println("[SETTLEMENT_DEBUG] WorkspaceRepository.getSettlementPlan called")
            val response = workspaceApiService.getSettlementPlan(workspaceId)
            System.out.println("[SETTLEMENT_DEBUG] WorkspaceRepository response.success=" + response.success + ", data=" + (response.data != null))
            if (response.success && response.data != null) {
                val data = response.data
                System.out.println("[SETTLEMENT_DEBUG] WorkspaceRepository transfers dto count: " + data.transfers.size)
                val transfers = data.transfers.map { dto ->
                    System.out.println("[SETTLEMENT_DEBUG] Map transfer DTO id=" + dto.id + " amount=" + dto.amount)
                    com.rkdevstudios.tripledger.features.settlement.domain.SettlementTransferItem(
                        id = dto.id,
                        fromUserId = dto.fromUserId,
                        fromUserName = dto.fromUserName,
                        toUserId = dto.toUserId,
                        toUserName = dto.toUserName,
                        amount = dto.amount.amount,
                        currency = dto.amount.currency
                    )
                }
                Result.success(
                    com.rkdevstudios.tripledger.features.settlement.domain.SettlementPlanItem(
                        sessionId = data.sessionId,
                        workspaceId = data.workspaceId,
                        transfers = transfers,
                        stateHash = data.stateHash,
                        planVersion = data.planVersion
                    )
                )
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to fetch settlement plan"))
            }
        } catch (e: Exception) {
            System.out.println("[SETTLEMENT_DEBUG] WorkspaceRepository exception: " + e.message)
            Result.failure(e)
        }
    }

    suspend fun confirmTransfer(workspaceId: String, transferId: String, sessionId: String): Result<Unit> {
        return try {
            val request = com.rkdevstudios.tripledger.features.workspace.data.api.ConfirmSettlementRequestDto(sessionId)
            val response = workspaceApiService.confirmTransfer(workspaceId, transferId, request)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to confirm settlement transfer"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSettlementHistory(workspaceId: String): Result<List<com.rkdevstudios.tripledger.features.settlement.domain.SettlementHistoryGroupItem>> {
        return try {
            val response = workspaceApiService.getSettlementHistory(workspaceId)
            if (response.success && response.data != null) {
                val mapped = response.data.history.map { groupDto ->
                    com.rkdevstudios.tripledger.features.settlement.domain.SettlementHistoryGroupItem(
                        date = LocalDate.parse(groupDto.date),
                        transactions = groupDto.transactions.map { itemDto ->
                            com.rkdevstudios.tripledger.features.settlement.domain.SettlementHistoryItem(
                                id = itemDto.id,
                                fromUserId = itemDto.fromUserId,
                                fromUserName = itemDto.fromUserName,
                                toUserId = itemDto.toUserId,
                                toUserName = itemDto.toUserName,
                                amount = itemDto.amount.amount,
                                currency = itemDto.amount.currency,
                                confirmedAt = itemDto.confirmedAt
                            )
                        }
                    )
                }
                Result.success(mapped)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to fetch settlement history"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
