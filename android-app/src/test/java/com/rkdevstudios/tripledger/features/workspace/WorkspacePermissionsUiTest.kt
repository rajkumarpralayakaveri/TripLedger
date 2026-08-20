package com.rkdevstudios.tripledger.features.workspace

import com.rkdevstudios.tripledger.core.network.NetworkResponse
import com.rkdevstudios.tripledger.features.workspace.data.WorkspaceRepository
import com.rkdevstudios.tripledger.features.workspace.data.api.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class WorkspacePermissionsUiTest {

    private class PermissionsFakeApiService : WorkspaceApiService {
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

        override suspend fun joinWorkspace(request: JoinRequestDto): NetworkResponse<WorkspaceMemberDto> =
            throw UnsupportedOperationException()

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
                    ExpenseTimelineItemDto("e_1", "Hotel", BigDecimal.valueOf(10000), "INR", "usr_raj", "Raj", "cat_hotel", "Hotel", "hotel", "#000000", "2026-08-20", "2026-08-20T10:00:00Z", "https://cloudinary.com/receipt.jpg", "Paid via UPI")
                ))
            )), null)

        override suspend fun createExpense(workspaceId: String, request: CreateExpenseRequestDto): NetworkResponse<ExpenseDto> =
            NetworkResponse(true, ExpenseDto("e_1", workspaceId, request.paidByUserId, request.description, request.categoryId, request.expenseDate, request.expenseAt, request.receiptUrl, request.note), null)

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
}
