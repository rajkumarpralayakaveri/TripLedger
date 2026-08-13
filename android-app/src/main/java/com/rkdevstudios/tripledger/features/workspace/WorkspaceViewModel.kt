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
    val status: String,
    val membersCount: Int
)

data class MockContributionSummary(
    val userId: String,
    val name: String,
    val planned: BigDecimal,
    val total: BigDecimal,
    val remaining: BigDecimal,
    val status: String
)

data class MockFinancialSnapshot(
    val budget: BigDecimal,
    val spent: BigDecimal,
    val currentFund: BigDecimal,
    val fundingGap: BigDecimal,
    val fundedMembers: Int,
    val pendingMembers: Int,
    val contributions: List<MockContributionSummary>
)

class WorkspaceViewModel(
    private val workspaceRepository: WorkspaceRepository
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

    init {
        loadWorkspaces()
    }

    fun loadWorkspaces() {
        viewModelScope.launch {
            _isLoadingWorkspaces.value = true
            _workspacesError.value = null
            workspaceRepository.getWorkspaces().fold(
                onSuccess = { list ->
                    _workspaces.value = list
                },
                onFailure = { error ->
                    _workspacesError.value = error.message ?: "Failed to load workspaces"
                }
            )
            _isLoadingWorkspaces.value = false
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
                    _summaryError.value = error.message ?: "Failed to load financial summary"
                }
            )
            _isLoadingSummary.value = false
        }
        
        // Mock loaders for secondary modules in this phase
        _currentTimeline.value = _timelines.value[id] ?: emptyList()
        _currentActivities.value = _activities.value[id] ?: emptyList()
        _currentBalances.value = _balances.value[id] ?: emptyList()
        _currentPlan.value = _plans.value[id]
        _currentHistory.value = _history.value[id] ?: emptyList()
    }

    fun confirmMockTransfer(workspaceId: String, transferId: String) {
        val plan = _plans.value[workspaceId] ?: return
        val transfer = plan.transfers.find { it.id == transferId } ?: return

        val updatedTransfers = plan.transfers.filter { it.id != transferId }
        val updatedPlan = plan.copy(transfers = updatedTransfers, planVersion = plan.planVersion + 1)
        _plans.value = _plans.value + (workspaceId to updatedPlan)

        val currentBalancesList = _balances.value[workspaceId] ?: emptyList()
        val updatedBalancesList = currentBalancesList.map { mb ->
            when (mb.userId) {
                transfer.fromUserId -> mb.copy(
                    paid = mb.paid + transfer.amount,
                    balance = mb.balance + transfer.amount
                )
                transfer.toUserId -> mb.copy(
                    paid = mb.paid - transfer.amount,
                    balance = mb.balance - transfer.amount
                )
                else -> mb
            }
        }
        _balances.value = _balances.value + (workspaceId to updatedBalancesList)

        val newHistoryItem = SettlementHistoryItem(
            id = "hist_${System.currentTimeMillis()}",
            fromUserId = transfer.fromUserId,
            fromUserName = transfer.fromUserName,
            toUserId = transfer.toUserId,
            toUserName = transfer.toUserName,
            amount = transfer.amount,
            currency = transfer.currency,
            confirmedAt = "Just now"
        )
        val currentHistGroups = _history.value[workspaceId] ?: emptyList()
        val todayGroup = currentHistGroups.find { it.date == LocalDate.now() }
        val updatedHistGroups = if (todayGroup != null) {
            currentHistGroups.map {
                if (it.date == LocalDate.now()) it.copy(transactions = listOf(newHistoryItem) + it.transactions) else it
            }
        } else {
            listOf(SettlementHistoryGroupItem(LocalDate.now(), listOf(newHistoryItem))) + currentHistGroups
        }
        _history.value = _history.value + (workspaceId to updatedHistGroups)

        val newActivity = ActivityFeedItem(
            id = "act_${System.currentTimeMillis()}",
            message = "${transfer.fromUserName} paid ${transfer.toUserName} ${transfer.currency} ${transfer.amount}",
            timestamp = "Just now"
        )
        _activities.value = _activities.value + (workspaceId to (listOf(newActivity) + (_activities.value[workspaceId] ?: emptyList())))

        selectWorkspace(workspaceId)
    }

    fun createWorkspace(
        name: String,
        description: String?,
        startDate: LocalDate,
        endDate: LocalDate,
        baseCurrency: String,
        budget: BigDecimal?
    ) {
        val workspaceId = "ws_${System.currentTimeMillis()}"
        val newWs = MockWorkspace(
            id = workspaceId,
            name = name,
            description = description,
            startDate = startDate,
            endDate = endDate,
            baseCurrency = baseCurrency,
            budget = budget,
            status = "PLANNING",
            membersCount = 1
        )
        _workspaces.value = _workspaces.value + newWs
    }

    fun joinWorkspace(inviteToken: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            workspaceRepository.joinWorkspace(inviteToken).fold(
                onSuccess = {
                    loadWorkspaces()
                    onSuccess()
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to join workspace")
                }
            )
        }
    }

    fun createInviteToken(
        workspaceId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            workspaceRepository.createInviteToken(workspaceId).fold(
                onSuccess = { token ->
                    onSuccess(token)
                },
                onFailure = { error ->
                    onError(error.message ?: "Failed to generate invite token")
                }
            )
        }
    }

    fun addMockExpense(
        workspaceId: String,
        description: String,
        amount: BigDecimal,
        currency: String,
        paidByName: String,
        categoryName: String,
        categoryColor: String,
        categoryIcon: String
    ) {
        val newItem = ExpenseItem(
            id = "e_${System.currentTimeMillis()}",
            description = description,
            amount = amount,
            currency = currency,
            paidByName = paidByName,
            date = LocalDate.now(),
            categoryName = categoryName,
            categoryColor = categoryColor,
            categoryIcon = categoryIcon
        )

        val currentGroups = _timelines.value[workspaceId] ?: emptyList()
        val todayGroup = currentGroups.find { it.date == LocalDate.now() }

        val updatedGroups = if (todayGroup != null) {
            currentGroups.map {
                if (it.date == LocalDate.now()) it.copy(expenses = it.expenses + newItem) else it
            }
        } else {
            listOf(ExpenseTimelineGroup(LocalDate.now(), listOf(newItem))) + currentGroups
        }

        _timelines.value = _timelines.value + (workspaceId to updatedGroups)

        val newActivity = ActivityFeedItem(
            id = "a_${System.currentTimeMillis()}",
            message = "$paidByName added $description",
            timestamp = "Just now"
        )
        _activities.value = _activities.value + (workspaceId to (listOf(newActivity) + (_activities.value[workspaceId] ?: emptyList())))

        selectWorkspace(workspaceId)
    }
}
