package com.rkdevstudios.tripledger.features.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkdevstudios.tripledger.features.expense.domain.ActivityFeedItem
import com.rkdevstudios.tripledger.features.expense.domain.ExpenseItem
import com.rkdevstudios.tripledger.features.expense.domain.ExpenseTimelineGroup
import com.rkdevstudios.tripledger.features.settlement.domain.MemberBalanceItem
import com.rkdevstudios.tripledger.features.settlement.domain.SettlementHistoryGroupItem
import com.rkdevstudios.tripledger.features.settlement.domain.SettlementHistoryItem
import com.rkdevstudios.tripledger.features.settlement.domain.SettlementPlanItem
import com.rkdevstudios.tripledger.features.settlement.domain.SettlementTransferItem
import com.rkdevstudios.tripledger.features.workspace.data.WorkspaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

data class MockWorkspace(
    val id: String,
    val name: String,
    val description: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val baseCurrency: String,
    val budget: BigDecimal?,
    val plannedMemberCount: Int,
    val status: String,
    val membersCount: Int,
    val contributionMode: String = "COMBINED",
    val createdBy: String? = null
)

data class MockContributionSummary(
    val userId: String,
    val name: String,
    val role: String,
    val planned: BigDecimal,
    val total: BigDecimal,
    val remaining: BigDecimal,
    val status: String,
    val fronted: BigDecimal = BigDecimal.ZERO
)

data class MockFinancialSnapshot(
    val budget: BigDecimal,
    val spent: BigDecimal,
    val currentFund: BigDecimal,
    val fundingGap: BigDecimal,
    val memberCount: Int,
    val fundedMembers: Int,
    val pendingMembers: Int,
    val contributions: List<MockContributionSummary>
)

class WorkspaceViewModel(
    private val workspaceRepository: WorkspaceRepository,
    val sessionManager: com.rkdevstudios.tripledger.core.auth.SessionManager? = null,
    val paymentProofRepository: com.rkdevstudios.tripledger.features.workspace.data.PaymentProofRepository? = null
) : ViewModel() {

    private val _workspaces = MutableStateFlow<List<MockWorkspace>>(emptyList())
    val workspaces: StateFlow<List<MockWorkspace>> = _workspaces.asStateFlow()

    private val _isLoadingWorkspaces = MutableStateFlow(false)
    val isLoadingWorkspaces: StateFlow<Boolean> = _isLoadingWorkspaces.asStateFlow()

    private val _workspacesError = MutableStateFlow<String?>(null)
    val workspacesError: StateFlow<String?> = _workspacesError.asStateFlow()

    private val _isLoadingSummary = MutableStateFlow(false)
    val isLoadingSummary: StateFlow<Boolean> = _isLoadingSummary.asStateFlow()

    private val _summaryError = MutableStateFlow<String?>(null)
    val summaryError: StateFlow<String?> = _summaryError.asStateFlow()

    private val _currentWorkspace = MutableStateFlow<MockWorkspace?>(null)
    val currentWorkspace: StateFlow<MockWorkspace?> = _currentWorkspace.asStateFlow()

    private val _currentFinancialSnapshot = MutableStateFlow<MockFinancialSnapshot?>(null)
    val currentFinancialSnapshot: StateFlow<MockFinancialSnapshot?> = _currentFinancialSnapshot.asStateFlow()

    // ViewModel-owned in-flight guards
    private val _isCreatingWorkspace = MutableStateFlow(false)
    val isCreatingWorkspace: StateFlow<Boolean> = _isCreatingWorkspace.asStateFlow()

    private val _isJoiningWorkspace = MutableStateFlow(false)
    val isJoiningWorkspace: StateFlow<Boolean> = _isJoiningWorkspace.asStateFlow()

    private val _isSavingExpense = MutableStateFlow(false)
    val isSavingExpense: StateFlow<Boolean> = _isSavingExpense.asStateFlow()

    private val _isGeneratingInvite = MutableStateFlow(false)
    val isGeneratingInvite: StateFlow<Boolean> = _isGeneratingInvite.asStateFlow()

    // Refresh loaders
    private val _isRefreshingWorkspaces = MutableStateFlow(false)
    val isRefreshingWorkspaces: StateFlow<Boolean> = _isRefreshingWorkspaces.asStateFlow()

    private val _isRefreshingSummary = MutableStateFlow(false)
    val isRefreshingSummary: StateFlow<Boolean> = _isRefreshingSummary.asStateFlow()

    // SETTLEMENT & EXPENSE TIMELINE MOCKS (To keep UI functional)
    private val _financialSnapshots = MutableStateFlow<Map<String, MockFinancialSnapshot>>(emptyMap())
    private val _timelines = MutableStateFlow<Map<String, List<ExpenseTimelineGroup>>>(emptyMap())
    private val _currentTimeline = MutableStateFlow<List<ExpenseTimelineGroup>>(emptyList())
    val currentTimeline: StateFlow<List<ExpenseTimelineGroup>> = _currentTimeline.asStateFlow()

    private val _activities = MutableStateFlow<Map<String, List<ActivityFeedItem>>>(emptyMap())
    private val _currentActivities = MutableStateFlow<List<ActivityFeedItem>>(emptyList())
    val currentActivities: StateFlow<List<ActivityFeedItem>> = _currentActivities.asStateFlow()

    private val _balances = MutableStateFlow<Map<String, List<MemberBalanceItem>>>(emptyMap())
    private val _currentBalances = MutableStateFlow<List<MemberBalanceItem>>(emptyList())
    val currentBalances: StateFlow<List<MemberBalanceItem>> = _currentBalances.asStateFlow()

    private val _plans = MutableStateFlow<Map<String, SettlementPlanItem>>(emptyMap())
    private val _currentPlan = MutableStateFlow<SettlementPlanItem?>(null)
    val currentPlan: StateFlow<SettlementPlanItem?> = _currentPlan.asStateFlow()

    private val _history = MutableStateFlow<Map<String, List<SettlementHistoryGroupItem>>>(emptyMap())
    private val _currentHistory = MutableStateFlow<List<SettlementHistoryGroupItem>>(emptyList())
    val currentHistory: StateFlow<List<SettlementHistoryGroupItem>> = _currentHistory.asStateFlow()

    fun loadWorkspaces() {
        viewModelScope.launch {
            _isLoadingWorkspaces.value = true
            _workspacesError.value = null
            workspaceRepository.getWorkspaces().fold(
                onSuccess = { list ->
                    _workspaces.value = list
                },
                onFailure = { error ->
                    val friendlyMsg = when (error) {
                        is java.net.UnknownHostException -> "No internet connection. Please try again."
                        is java.net.SocketTimeoutException -> "Connection timed out. Please try again."
                        else -> error.message ?: "Failed to load workspaces"
                    }
                    _workspacesError.value = friendlyMsg
                }
            )
            _isLoadingWorkspaces.value = false
        }
    }

    fun refreshWorkspaces() {
        if (_isRefreshingWorkspaces.value) return
        _isRefreshingWorkspaces.value = true
        viewModelScope.launch {
            workspaceRepository.getWorkspaces().fold(
                onSuccess = { list ->
                    _workspaces.value = list
                    _workspacesError.value = null
                },
                onFailure = { error ->
                    val friendlyMsg = when (error) {
                        is java.net.UnknownHostException -> "No internet connection. Please check your network and try again."
                        is java.net.SocketTimeoutException -> "Connection timed out. Please try again."
                        else -> error.message ?: "Failed to refresh workspaces"
                    }
                    _workspacesError.value = friendlyMsg
                }
            )
            _isRefreshingWorkspaces.value = false
        }
    }

    fun selectWorkspace(id: String) {
        _currentWorkspace.value = _workspaces.value.find { it.id == id }
        viewModelScope.launch {
            _isLoadingSummary.value = true
            _summaryError.value = null
            workspaceRepository.getFinancialSummary(id).fold(
                onSuccess = { snapshot ->
                    _currentFinancialSnapshot.value = snapshot
                },
                onFailure = { error ->
                    val friendlyMsg = when (error) {
                        is java.net.UnknownHostException -> "No internet connection. Please check your network and try again."
                        is java.net.SocketTimeoutException -> "Connection timed out. Please try again."
                        else -> error.message ?: "Failed to load financial summary"
                    }
                    _summaryError.value = friendlyMsg
                }
            )
            _isLoadingSummary.value = false
        }
        
        refreshExpenseTimeline(id)
        refreshBalances(id)
        refreshSettlementPlan(id)
        refreshSettlementHistory(id)

        _currentActivities.value = _activities.value[id] ?: emptyList()
    }

    fun refreshExpenseTimeline(id: String) {
        viewModelScope.launch {
            workspaceRepository.getExpenseTimeline(id).fold(
                onSuccess = { groups ->
                    _currentTimeline.value = groups
                },
                onFailure = { _ -> }
            )
        }
    }

    fun refreshFinancialSummary(id: String) {
        if (_isRefreshingSummary.value) return
        _isRefreshingSummary.value = true
        viewModelScope.launch {
            workspaceRepository.getFinancialSummary(id).fold(
                onSuccess = { snapshot ->
                    _currentFinancialSnapshot.value = snapshot
                    _summaryError.value = null
                },
                onFailure = { error ->
                    val friendlyMsg = when (error) {
                        is java.net.UnknownHostException -> "No internet connection. Please check your network and try again."
                        is java.net.SocketTimeoutException -> "Connection timed out. Please try again."
                        else -> error.message ?: "Failed to refresh details"
                    }
                    _summaryError.value = friendlyMsg
                }
            )
            _isRefreshingSummary.value = false
        }
    }

    fun refreshBalances(workspaceId: String) {
        viewModelScope.launch {
            workspaceRepository.getBalances(workspaceId).fold(
                onSuccess = { balancesList ->
                    _currentBalances.value = balancesList
                },
                onFailure = { _ -> }
            )
        }
    }

    fun refreshSettlementPlan(workspaceId: String) {
        viewModelScope.launch {
            workspaceRepository.getSettlementPlan(workspaceId).fold(
                onSuccess = { plan ->
                    _currentPlan.value = plan
                },
                onFailure = { _ -> }
            )
        }
    }

    fun refreshSettlementHistory(workspaceId: String) {
        viewModelScope.launch {
            workspaceRepository.getSettlementHistory(workspaceId).fold(
                onSuccess = { historyList ->
                    _currentHistory.value = historyList
                },
                onFailure = { _ -> }
            )
        }
    }

    fun confirmTransfer(workspaceId: String, transferId: String, sessionId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            workspaceRepository.confirmTransfer(workspaceId, transferId, sessionId).fold(
                onSuccess = {
                    refreshBalances(workspaceId)
                    refreshSettlementPlan(workspaceId)
                    refreshSettlementHistory(workspaceId)
                    refreshFinancialSummary(workspaceId)
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to confirm settlement transfer")
                }
            )
        }
    }

    fun createWorkspace(
        name: String,
        description: String?,
        startDate: LocalDate,
        endDate: LocalDate,
        baseCurrency: String,
        budget: BigDecimal?,
        plannedMemberCount: Int,
        contributionMode: String = "COMBINED",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isCreatingWorkspace.value = true
            workspaceRepository.createWorkspace(
                name = name,
                description = description,
                startDate = startDate,
                endDate = endDate,
                baseCurrency = baseCurrency,
                budget = budget,
                plannedMemberCount = plannedMemberCount,
                contributionMode = contributionMode
            ).fold(
                onSuccess = {
                    loadWorkspaces()
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to create workspace")
                }
            )
            _isCreatingWorkspace.value = false
        }
    }

    fun joinWorkspace(inviteToken: String, onSuccess: (String) -> Unit = {}, onError: (String) -> Unit = {}) {
        if (_isJoiningWorkspace.value) return
        _isJoiningWorkspace.value = true
        viewModelScope.launch {
            workspaceRepository.joinWorkspace(inviteToken).fold(
                onSuccess = { workspaceId ->
                    loadWorkspaces()
                    onSuccess(workspaceId)
                },
                onFailure = { error ->
                    val friendlyMsg = when (error) {
                        is java.net.UnknownHostException -> "No internet connection. Please try again."
                        is java.net.SocketTimeoutException -> "Request timed out. Please try again."
                        else -> error.message ?: "Failed to join workspace"
                    }
                    onError(friendlyMsg)
                }
            )
            _isJoiningWorkspace.value = false
        }
    }

    fun createInviteToken(
        workspaceId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isGeneratingInvite.value) return
        _isGeneratingInvite.value = true
        viewModelScope.launch {
            workspaceRepository.createInviteToken(workspaceId).fold(
                onSuccess = { token ->
                    onSuccess(token)
                },
                onFailure = { error ->
                    val friendlyMsg = when (error) {
                        is java.net.UnknownHostException -> "No internet connection. Please try again."
                        is java.net.SocketTimeoutException -> "Request timed out. Please try again."
                        else -> error.message ?: "Failed to generate invite token"
                    }
                    onError(friendlyMsg)
                }
            )
            _isGeneratingInvite.value = false
        }
    }

    fun createExpense(
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
        splitValues: Map<String, BigDecimal>? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (_isSavingExpense.value) return
        _isSavingExpense.value = true
        viewModelScope.launch {
            workspaceRepository.createExpense(
                workspaceId = workspaceId,
                paidByUserId = paidByUserId,
                amount = amount,
                currency = currency,
                description = description,
                categoryId = categoryId,
                expenseDate = expenseDate,
                participantIds = participantIds,
                expenseAt = expenseAt,
                receiptUrl = receiptUrl,
                note = note,
                splitType = splitType,
                splitValues = splitValues
            ).fold(
                onSuccess = {
                    refreshExpenseTimeline(workspaceId)
                    refreshFinancialSummary(workspaceId)
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to save expense")
                }
            )
            _isSavingExpense.value = false
        }
    }

    fun updateExpense(
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
        reason: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (_isSavingExpense.value) return
        _isSavingExpense.value = true
        viewModelScope.launch {
            workspaceRepository.updateExpense(
                workspaceId = workspaceId,
                expenseId = expenseId,
                amount = amount,
                currency = currency,
                description = description,
                categoryId = categoryId,
                expenseDate = expenseDate,
                splitType = splitType,
                participantIds = participantIds,
                splitValues = splitValues,
                reason = reason
            ).fold(
                onSuccess = {
                    refreshExpenseTimeline(workspaceId)
                    refreshFinancialSummary(workspaceId)
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to update expense")
                }
            )
            _isSavingExpense.value = false
        }
    }

    fun deleteExpense(
        workspaceId: String,
        expenseId: String,
        reason: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            workspaceRepository.deleteExpense(
                workspaceId = workspaceId,
                expenseId = expenseId,
                reason = reason
            ).fold(
                onSuccess = {
                    refreshExpenseTimeline(workspaceId)
                    refreshFinancialSummary(workspaceId)
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to delete expense")
                }
            )
        }
    }

    fun uploadExpenseReceipt(
        workspaceId: String,
        fileBytes: ByteArray,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            paymentProofRepository?.uploadReceiptToCloudinary(workspaceId, fileBytes)?.fold(
                onSuccess = { url -> onSuccess(url) },
                onFailure = { err -> onError(err.message ?: "Failed to upload receipt") }
            ) ?: onError("Upload repository unavailable")
        }
    }

    fun updateMemberPlannedContribution(workspaceId: String, userId: String, plannedAmount: BigDecimal, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            workspaceRepository.updateMemberPlannedContribution(workspaceId, userId, plannedAmount).fold(
                onSuccess = {
                    refreshFinancialSummary(workspaceId)
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to update planned contribution")
                }
            )
        }
    }

    fun updateMemberRole(workspaceId: String, userId: String, role: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            workspaceRepository.updateMemberRole(workspaceId, userId, role).fold(
                onSuccess = {
                    refreshFinancialSummary(workspaceId)
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to update member role")
                }
            )
        }
    }

    fun leaveWorkspace(workspaceId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            workspaceRepository.leaveWorkspace(workspaceId).fold(
                onSuccess = {
                    loadWorkspaces()
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to leave workspace")
                }
            )
        }
    }

    fun removeMember(workspaceId: String, userId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            workspaceRepository.removeMember(workspaceId, userId).fold(
                onSuccess = {
                    refreshFinancialSummary(workspaceId)
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to remove member")
                }
            )
        }
    }

    fun updateWorkspace(
        workspaceId: String,
        name: String? = null,
        description: String? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        budget: BigDecimal? = null,
        plannedMemberCount: Int? = null,
        contributionMode: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            workspaceRepository.updateWorkspace(
                workspaceId = workspaceId,
                name = name,
                description = description,
                startDate = startDate,
                endDate = endDate,
                budget = budget,
                plannedMemberCount = plannedMemberCount,
                contributionMode = contributionMode
            ).fold(
                onSuccess = { updated ->
                    _currentWorkspace.value = updated
                    refreshFinancialSummary(workspaceId)
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to update workspace")
                }
            )
        }
    }

    fun archiveWorkspace(workspaceId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            workspaceRepository.archiveWorkspace(workspaceId).fold(
                onSuccess = {
                    loadWorkspaces()
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to archive workspace")
                }
            )
        }
    }
}
