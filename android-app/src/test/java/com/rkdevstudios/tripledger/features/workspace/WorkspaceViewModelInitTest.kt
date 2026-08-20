package com.rkdevstudios.tripledger.features.workspace

import com.rkdevstudios.tripledger.core.network.NetworkResponse
import com.rkdevstudios.tripledger.features.workspace.data.WorkspaceRepository
import com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceApiService
import com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceDto
import com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceFinancialSnapshotDto
import com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceCreateRequestDto
import com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceMemberDto
import com.rkdevstudios.tripledger.features.workspace.data.api.InviteRequestDto
import com.rkdevstudios.tripledger.features.workspace.data.api.InviteTokenDto
import com.rkdevstudios.tripledger.features.workspace.data.api.JoinRequestDto
import com.rkdevstudios.tripledger.features.workspace.data.api.PaymentSignatureRequest
import com.rkdevstudios.tripledger.features.workspace.data.api.PaymentSignatureResponse
import com.rkdevstudios.tripledger.features.workspace.data.api.PaymentCompletionRequest
import com.rkdevstudios.tripledger.features.workspace.data.api.PaymentProofResponseDto
import com.rkdevstudios.tripledger.features.workspace.data.api.PaymentRejectionRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceViewModelInitTest {

    private class RecordingFakeWorkspaceApiService : WorkspaceApiService {
        var getWorkspacesCallCount = 0

        override suspend fun getWorkspaces(): NetworkResponse<List<WorkspaceDto>> {
            getWorkspacesCallCount++
            return NetworkResponse(success = true, data = emptyList(), error = null)
        }

        override suspend fun getFinancialSummary(workspaceId: String): NetworkResponse<WorkspaceFinancialSnapshotDto> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun createInviteToken(workspaceId: String, request: InviteRequestDto): NetworkResponse<InviteTokenDto> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun joinWorkspace(request: JoinRequestDto): NetworkResponse<WorkspaceMemberDto> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun createWorkspace(request: WorkspaceCreateRequestDto): NetworkResponse<WorkspaceDto> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun getUploadSignature(workspaceId: String, request: PaymentSignatureRequest): NetworkResponse<PaymentSignatureResponse> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun completeUpload(workspaceId: String, request: PaymentCompletionRequest): NetworkResponse<PaymentProofResponseDto> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun getPayments(workspaceId: String): NetworkResponse<List<PaymentProofResponseDto>> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun approvePayment(workspaceId: String, paymentId: String): NetworkResponse<PaymentProofResponseDto> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun rejectPayment(workspaceId: String, paymentId: String, request: PaymentRejectionRequest): NetworkResponse<PaymentProofResponseDto> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun removeMember(workspaceId: String, userId: String): NetworkResponse<Unit> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun updateMemberPlannedContribution(
            workspaceId: String,
            userId: String,
            request: com.rkdevstudios.tripledger.features.workspace.data.api.UpdatePlannedContributionRequestDto
        ): NetworkResponse<Unit> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun updateMemberRole(
            workspaceId: String,
            userId: String,
            request: com.rkdevstudios.tripledger.features.workspace.data.api.UpdateMemberRoleRequestDto
        ): NetworkResponse<WorkspaceMemberDto> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun leaveWorkspace(workspaceId: String): NetworkResponse<Unit> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun updateWorkspace(
            workspaceId: String,
            request: com.rkdevstudios.tripledger.features.workspace.data.api.WorkspaceUpdateRequestDto
        ): NetworkResponse<WorkspaceDto> {
            throw UnsupportedOperationException("Not needed for init test")
        }

        override suspend fun getExpenseTimeline(workspaceId: String): NetworkResponse<com.rkdevstudios.tripledger.features.workspace.data.api.ExpenseTimelineResponseDto> {
            return NetworkResponse(true, com.rkdevstudios.tripledger.features.workspace.data.api.ExpenseTimelineResponseDto(emptyList()), null)
        }

        override suspend fun createExpense(
            workspaceId: String,
            request: com.rkdevstudios.tripledger.features.workspace.data.api.CreateExpenseRequestDto
        ): NetworkResponse<com.rkdevstudios.tripledger.features.workspace.data.api.ExpenseDto> {
            return NetworkResponse(true, com.rkdevstudios.tripledger.features.workspace.data.api.ExpenseDto("e_1", workspaceId, request.paidByUserId, request.description, request.categoryId, request.expenseDate, request.expenseAt, request.receiptUrl, request.note), null)
        }
    }

    @Test
    fun instantiation_doesNotTriggerWorkspaceLoadOrNetworkRequests() {
        val fakeApiService = RecordingFakeWorkspaceApiService()
        val repository = WorkspaceRepository(fakeApiService)

        val viewModel = WorkspaceViewModel(repository)

        // Verify zero API calls are made upon instantiation
        assertEquals(0, fakeApiService.getWorkspacesCallCount)
        assertEquals(emptyList<MockWorkspace>(), viewModel.workspaces.value)
        assertFalse(viewModel.isLoadingWorkspaces.value)
        assertNull(viewModel.workspacesError.value)
    }
}
