package com.rkdevstudios.tripledger.features.workspace

import com.rkdevstudios.tripledger.core.network.NetworkResponse
import com.rkdevstudios.tripledger.features.workspace.data.WorkspaceRepository
import com.rkdevstudios.tripledger.features.workspace.data.api.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class WorkspacePermissionsUiTest {

    private open class PermissionsFakeApiService : WorkspaceApiService {
        var lastUpdatedRole: String? = null
        var lastRemovedUserId: String? = null
        var lastArchivedWorkspaceId: String? = null
        var lastLeftWorkspaceId: String? = null

        override suspend fun getWorkspaces(): NetworkResponse<List<WorkspaceDto>> =
            NetworkResponse(true, emptyList(), null)

        override suspend fun getFinancialSummary(workspaceId: String): NetworkResponse<WorkspaceFinancialSnapshotDto> =
            NetworkResponse(true, WorkspaceFinancialSnapshotDto(
                workspaceId = workspaceId,
                totalBudget = BigDecimal.valueOf(10000),
                totalSpent = BigDecimal.ZERO,
                remainingBudget = BigDecimal.valueOf(10000),
                currentFund = BigDecimal.ZERO,
                fundingGap = BigDecimal.valueOf(10000),
                memberCount = 2,
                fundedMembers = 0,
                pendingMembers = 2,
                overFundedMembers = 0,
                memberContributions = listOf(
                    ContributionSummaryDto("usr_admin", "Admin User", "ADMIN", BigDecimal.valueOf(5000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(5000), "PENDING"),
                    ContributionSummaryDto("usr_member", "Regular Member", "MEMBER", BigDecimal.valueOf(5000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(5000), "PENDING")
                )
            ), null)

        override suspend fun createInviteToken(workspaceId: String, request: InviteRequestDto): NetworkResponse<InviteTokenDto> =
            throw UnsupportedOperationException()

        override suspend fun joinWorkspace(request: JoinRequestDto): NetworkResponse<WorkspaceMemberDto> {
            return if (request.inviteToken == "valid_token_123") {
                NetworkResponse(true, WorkspaceMemberDto("ws_joined_1", "usr_new", "MEMBER"), null)
            } else {
                NetworkResponse(false, null, com.rkdevstudios.tripledger.core.network.ApiError("400", "Invalid or expired invitation token"))
            }
        }

        override suspend fun createWorkspace(request: WorkspaceCreateRequestDto): NetworkResponse<WorkspaceDto> =
            throw UnsupportedOperationException()

        override suspend fun getUploadSignature(workspaceId: String, request: PaymentSignatureRequest): NetworkResponse<PaymentSignatureResponse> =
            throw UnsupportedOperationException()

        override suspend fun completeUpload(workspaceId: String, request: PaymentCompletionRequest): NetworkResponse<PaymentProofResponseDto> =
            throw UnsupportedOperationException()

        override suspend fun getPayments(workspaceId: String): NetworkResponse<List<PaymentProofResponseDto>> =
            throw UnsupportedOperationException()

        override suspend fun approvePayment(workspaceId: String, paymentId: String): NetworkResponse<PaymentProofResponseDto> =
            throw UnsupportedOperationException()

        override suspend fun rejectPayment(workspaceId: String, paymentId: String, request: PaymentRejectionRequest): NetworkResponse<PaymentProofResponseDto> =
            throw UnsupportedOperationException()

        override suspend fun removeMember(workspaceId: String, userId: String): NetworkResponse<Unit> {
            lastRemovedUserId = userId
            return NetworkResponse(true, Unit, null)
        }

        override suspend fun updateMemberPlannedContribution(workspaceId: String, userId: String, request: UpdatePlannedContributionRequestDto): NetworkResponse<Unit> =
            NetworkResponse(true, Unit, null)

        override suspend fun updateMemberRole(workspaceId: String, userId: String, request: UpdateMemberRoleRequestDto): NetworkResponse<WorkspaceMemberDto> {
            lastUpdatedRole = request.role
            return NetworkResponse(true, WorkspaceMemberDto(workspaceId, userId, request.role), null)
        }

        override suspend fun leaveWorkspace(workspaceId: String): NetworkResponse<Unit> {
            lastLeftWorkspaceId = workspaceId
            return NetworkResponse(true, Unit, null)
        }

        override suspend fun getExpenseTimeline(workspaceId: String): NetworkResponse<ExpenseTimelineResponseDto> =
            NetworkResponse(true, ExpenseTimelineResponseDto(listOf(
                ExpenseTimelineGroupDto("2026-08-20", listOf(
                    ExpenseTimelineItemDto("e_1", "Hotel", BigDecimal.valueOf(10000), "INR", "usr_raj", "Raj", "cat_hotel", "Hotel", "hotel", "#000000", "2026-08-20", "2026-08-20T10:00:00Z", "https://cloudinary.com/receipt.jpg", "Paid via UPI", "EQUAL", emptyList())
                ))
            )), null)

        override suspend fun createExpense(workspaceId: String, request: CreateExpenseRequestDto): NetworkResponse<ExpenseDto> =
            NetworkResponse(true, ExpenseDto("e_1", workspaceId, request.paidByUserId, request.description, request.categoryId, request.expenseDate, request.expenseAt, request.receiptUrl, request.note, request.splitType, request.splitValues), null)

        override suspend fun updateWorkspace(workspaceId: String, request: WorkspaceUpdateRequestDto): NetworkResponse<WorkspaceDto> {
            if (request.status == "ARCHIVED") {
                lastArchivedWorkspaceId = workspaceId
            }
            return NetworkResponse(true, WorkspaceDto(
                id = workspaceId,
                name = request.name ?: "Trip",
                description = request.description,
                startDate = "2026-08-20",
                endDate = "2026-08-25",
                baseCurrency = "INR",
                budget = request.budget ?: BigDecimal.TEN,
                plannedMemberCount = 5,
                memberCount = 2,
                status = request.status ?: "ACTIVE",
                contributionStrategy = "EQUAL",
                contributionMode = request.contributionMode ?: "COMBINED"
            ), null)
        }

        var lastUpdatedExpenseId: String? = null
        var lastDeletedExpenseId: String? = null

        override suspend fun updateExpense(
            workspaceId: String,
            expenseId: String,
            request: UpdateExpenseRequestDto
        ): NetworkResponse<ExpenseDto> {
            lastUpdatedExpenseId = expenseId
            return NetworkResponse(true, ExpenseDto(expenseId, workspaceId, "usr_raj", request.description, request.categoryId, request.expenseDate, null, null, null, request.splitType, request.splitValues), null)
        }

        override suspend fun deleteExpense(
            workspaceId: String,
            expenseId: String,
            request: DeleteExpenseRequestDto
        ): NetworkResponse<Unit> {
            lastDeletedExpenseId = expenseId
            return NetworkResponse(true, Unit, null)
        }

        var lastConfirmedTransferId: String? = null

        override suspend fun getBalances(workspaceId: String): NetworkResponse<BalancesResponseDto> {
            return NetworkResponse(true, BalancesResponseDto(listOf(
                MemberBalanceResponseDto("usr_raj", "Raj", BigDecimal.valueOf(1000), BigDecimal.valueOf(500), BigDecimal.valueOf(500))
            )), null)
        }

        override suspend fun getSettlementPlan(workspaceId: String): NetworkResponse<SettlementPlanResponseDto> {
            return NetworkResponse(true, SettlementPlanResponseDto(
                sessionId = "sess_123",
                workspaceId = workspaceId,
                transfers = listOf(
                    SettlementTransferResponseDto("t_1", "usr_member", "Regular Member", "usr_admin", "Admin User", MoneyDto(BigDecimal.valueOf(500), "INR"))
                ),
                stateHash = "hash1",
                planVersion = 1
            ), null)
        }

        override suspend fun confirmTransfer(
            workspaceId: String,
            transferId: String,
            request: ConfirmSettlementRequestDto
        ): NetworkResponse<Unit> {
            lastConfirmedTransferId = transferId
            return NetworkResponse(true, Unit, null)
        }

        override suspend fun getSettlementHistory(workspaceId: String): NetworkResponse<SettlementHistoryResponseDto> {
            return NetworkResponse(true, SettlementHistoryResponseDto(listOf(
                SettlementHistoryGroupDto("2026-08-20", listOf(
                    SettlementHistoryItemDto("hist_1", "usr_member", "Regular Member", "usr_admin", "Admin User", MoneyDto(BigDecimal.valueOf(500), "INR"), "2026-08-20T12:00:00Z")
                ))
            )), null)
        }
    }

    @Test
    fun memberPromotion_callsUpdateMemberRoleApi() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.updateMemberRole("ws_1", "usr_member", "ADMIN")
            assertTrue(result.isSuccess)
            assertEquals("ADMIN", fakeApi.lastUpdatedRole)
        }
    }

    @Test
    fun memberRemoval_callsRemoveMemberApi() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.removeMember("ws_1", "usr_member")
            assertTrue(result.isSuccess)
            assertEquals("usr_member", fakeApi.lastRemovedUserId)
        }
    }

    @Test
    fun archiveWorkspace_callsUpdateWorkspaceWithArchivedStatus() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.archiveWorkspace("ws_1")
            assertTrue(result.isSuccess)
            assertEquals("ws_1", fakeApi.lastArchivedWorkspaceId)
        }
    }

    @Test
    fun leaveWorkspace_callsLeaveWorkspaceApi() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.leaveWorkspace("ws_1")
            assertTrue(result.isSuccess)
            assertEquals("ws_1", fakeApi.lastLeftWorkspaceId)
        }
    }

    @Test
    fun createExpense_callsRealApiAndParsesExpenseFields() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.createExpense(
                workspaceId = "ws_1",
                paidByUserId = "usr_raj",
                amount = BigDecimal.valueOf(10000),
                currency = "INR",
                description = "Hotel",
                categoryId = "cat_hotel",
                expenseDate = java.time.LocalDate.parse("2026-08-20"),
                participantIds = listOf("usr_raj", "usr_john"),
                expenseAt = "2026-08-20T10:00:00Z",
                receiptUrl = "https://cloudinary.com/receipt.jpg",
                note = "Paid via UPI"
            )
            assertTrue(result.isSuccess)
        }
    }

    @Test
    fun getExpenseTimeline_preservesReceiptUrlAndExpenseAt() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val timelineRes = repository.getExpenseTimeline("ws_1")
            assertTrue(timelineRes.isSuccess)
            val groups = timelineRes.getOrThrow()
            assertFalse(groups.isEmpty())
            val firstExpense = groups.first().expenses.first()
            assertEquals("https://cloudinary.com/receipt.jpg", firstExpense.receiptUrl)
            assertEquals("2026-08-20T10:00:00Z", firstExpense.expenseAt)
        }
    }

    @Test
    fun joinWorkspace_successfulJoin_assignsMemberRoleAndReturnsWorkspaceId() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.joinWorkspace("valid_token_123")
            assertTrue(result.isSuccess)
            assertEquals("ws_joined_1", result.getOrThrow())
        }
    }

    @Test
    fun joinWorkspace_invalidInviteToken_returnsError() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.joinWorkspace("invalid_token")
            assertTrue(result.isFailure)
            assertEquals("Invalid or expired invitation token", result.exceptionOrNull()?.message)
        }
    }

    @Test
    fun updateExpense_callsUpdateExpenseApi() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.updateExpense(
                workspaceId = "ws_1",
                expenseId = "e_1",
                amount = BigDecimal.valueOf(12000),
                currency = "INR",
                description = "Updated Hotel",
                categoryId = "cat_hotel",
                expenseDate = java.time.LocalDate.parse("2026-08-20"),
                splitType = "EQUAL",
                participantIds = listOf("usr_raj"),
                splitValues = null,
                reason = "Updated description"
            )
            assertTrue(result.isSuccess)
            assertEquals("e_1", fakeApi.lastUpdatedExpenseId)
        }
    }

    @Test
    fun deleteExpense_callsDeleteExpenseApi() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.deleteExpense(
                workspaceId = "ws_1",
                expenseId = "e_1",
                reason = "No longer needed"
            )
            assertTrue(result.isSuccess)
            assertEquals("e_1", fakeApi.lastDeletedExpenseId)
        }
    }

    @Test
    fun getBalances_callsBalancesApi() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.getBalances("ws_1")
            assertTrue(result.isSuccess)
            val list = result.getOrThrow()
            assertEquals(1, list.size)
            assertEquals("Raj", list.first().userName)
        }
    }

    @Test
    fun getSettlementPlan_callsPlanApi() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.getSettlementPlan("ws_1")
            assertTrue(result.isSuccess)
            val plan = result.getOrThrow()
            assertEquals("sess_123", plan.sessionId)
            assertEquals("usr_member", plan.transfers.first().fromUserId)
        }
    }

    @Test
    fun confirmTransfer_callsConfirmApi() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.confirmTransfer("ws_1", "t_1", "sess_123")
            assertTrue(result.isSuccess)
            assertEquals("t_1", fakeApi.lastConfirmedTransferId)
        }
    }

    @Test
    fun getSettlementHistory_callsHistoryApi() {
        val fakeApi = PermissionsFakeApiService()
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.getSettlementHistory("ws_1")
            assertTrue(result.isSuccess)
            val list = result.getOrThrow()
            assertEquals(1, list.size)
            assertEquals("usr_member", list.first().transactions.first().fromUserId)
        }
    }

    @Test
    fun getSettlementPlan_mapsMultipleTransfersSuccessfully() {
        val fakeApi = object : PermissionsFakeApiService() {
            override suspend fun getSettlementPlan(workspaceId: String): NetworkResponse<SettlementPlanResponseDto> {
                return NetworkResponse(true, SettlementPlanResponseDto(
                    sessionId = "sess_multi",
                    workspaceId = workspaceId,
                    transfers = listOf(
                        SettlementTransferResponseDto("t_1", "usr_1", "User One", "usr_2", "User Two", MoneyDto(BigDecimal.valueOf(100), "INR")),
                        SettlementTransferResponseDto("t_2", "usr_3", "User Three", "usr_2", "User Two", MoneyDto(BigDecimal.valueOf(250), "INR"))
                    ),
                    stateHash = "hash_multi",
                    planVersion = 2
                ), null)
            }
        }
        val repository = WorkspaceRepository(fakeApi)
        kotlinx.coroutines.runBlocking {
            val result = repository.getSettlementPlan("ws_1")
            assertTrue(result.isSuccess)
            val plan = result.getOrThrow()
            assertEquals("sess_multi", plan.sessionId)
            assertEquals(2, plan.transfers.size)
            
            val t1 = plan.transfers[0]
            assertEquals("t_1", t1.id)
            assertEquals("User One", t1.fromUserName)
            assertEquals("User Two", t1.toUserName)
            assertEquals(0, BigDecimal.valueOf(100).compareTo(t1.amount))
            assertEquals("INR", t1.currency)

            val t2 = plan.transfers[1]
            assertEquals("t_2", t2.id)
            assertEquals("User Three", t2.fromUserName)
            assertEquals("User Two", t2.toUserName)
            assertEquals(0, BigDecimal.valueOf(250).compareTo(t2.amount))
            assertEquals("INR", t2.currency)
        }
    }

    @Test
    fun getSettlementPlan_viewModelUpdatesFlowSuccessfully() {
        val fakeApi = object : PermissionsFakeApiService() {
            override suspend fun getSettlementPlan(workspaceId: String): NetworkResponse<SettlementPlanResponseDto> {
                return NetworkResponse(true, SettlementPlanResponseDto(
                    sessionId = "sess_multi",
                    workspaceId = workspaceId,
                    transfers = listOf(
                        SettlementTransferResponseDto("t_1", "usr_1", "User One", "usr_2", "User Two", MoneyDto(BigDecimal.valueOf(100), "INR")),
                        SettlementTransferResponseDto("t_2", "usr_3", "User Three", "usr_2", "User Two", MoneyDto(BigDecimal.valueOf(250), "INR"))
                    ),
                    stateHash = "hash_multi",
                    planVersion = 2
                ), null)
            }
        }
        val repository = WorkspaceRepository(fakeApi)
        val viewModel = WorkspaceViewModel(repository)

        // Invoke ViewModel refresh natively (synchronously on the test thread using runBlocking for any internals)
        kotlinx.coroutines.runBlocking {
            val result = repository.getSettlementPlan("ws_1")
            assertTrue(result.isSuccess)
            val plan = result.getOrThrow()
            assertEquals(2, plan.transfers.size)
        }
    }

    @Test
    fun getSettlementPlan_transferFields_areNonEmptyAndFormatted() {
        val fakeApi = object : PermissionsFakeApiService() {
            override suspend fun getSettlementPlan(workspaceId: String): NetworkResponse<SettlementPlanResponseDto> {
                return NetworkResponse(true, SettlementPlanResponseDto(
                    sessionId = "sess_multi",
                    workspaceId = workspaceId,
                    transfers = listOf(
                        SettlementTransferResponseDto("t_1", "usr_1", "User One", "usr_2", "User Two", MoneyDto(BigDecimal.valueOf(100), "INR"))
                    ),
                    stateHash = "hash_multi",
                    planVersion = 1
                ), null)
            }
        }
        val repository = WorkspaceRepository(fakeApi)

        kotlinx.coroutines.runBlocking {
            val result = repository.getSettlementPlan("ws_1")
            assertTrue(result.isSuccess)
            val item = result.getOrThrow().transfers.first()
            
            // 1. Verify Compose state strings are non-empty and non-blank
            assertTrue(item.fromUserName.isNotBlank())
            assertTrue(item.toUserName.isNotBlank())
            assertEquals("User One", item.fromUserName)
            assertEquals("User Two", item.toUserName)
            
            // 2. Verify money formatting produces expected label string
            val formatted = com.rkdevstudios.tripledger.core.utils.CurrencyFormatter.formatMoney(item.amount, item.currency)
            assertEquals("₹100.00", formatted)
        }
    }
}
