package com.rkdevstudios.tripledger.features.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripCard
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing
import com.rkdevstudios.tripledger.core.utils.CurrencyFormatter
import com.rkdevstudios.tripledger.features.workspace.data.api.ContributionSummaryDto
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceDetailsScreen(
    workspaceId: String,
    viewModel: WorkspaceViewModel,
    currentUserId: String,
    onNavigateBack: () -> Unit,
    onInviteMembers: (String) -> Unit,
    onNavigateToExpenses: (String) -> Unit,
    onNavigateToSettlements: (String) -> Unit,
    onNavigateToSubmission: (String) -> Unit,
    onNavigateToVerification: (String) -> Unit
) {
    LaunchedEffect(workspaceId) {
        viewModel.selectWorkspace(workspaceId)
    }

    val workspace by viewModel.currentWorkspace.collectAsState()
    val snapshot by viewModel.currentFinancialSnapshot.collectAsState()
    val isLoading by viewModel.isLoadingSummary.collectAsState()
    val error by viewModel.summaryError.collectAsState()
    val isRefreshing by viewModel.isRefreshingSummary.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showEditTripDialog by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var selectedMemberForManagement by remember { mutableStateOf<MockContributionSummary?>(null) }
    var errorMessageDialog by remember { mutableStateOf<String?>(null) }

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshFinancialSummary(workspaceId)
        }
    }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            pullToRefreshState.startRefresh()
        } else {
            pullToRefreshState.endRefresh()
        }
    }

    val currentUserRole = snapshot?.contributions?.find { it.userId == currentUserId }?.role ?: "MEMBER"
    val isCallerAdmin = currentUserRole == "ADMIN"
    val isCallerPrimaryAdmin = workspace?.createdBy != null && workspace?.createdBy == currentUserId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
    ) {
        workspace?.let { ws ->
            // Header Row with Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ws.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(pullToRefreshState.nestedScrollConnection)
            ) {
                if (isLoading && snapshot == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (error != null && snapshot == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error ?: "Failed to load details",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = TripSpacing.M)
                        )
                        TripButton(
                            text = "Retry",
                            onClick = { viewModel.selectWorkspace(workspaceId) }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
                    ) {
                        if (error != null) {
                            item {
                                TripCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(TripSpacing.XS),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Couldn't refresh details.",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Retry",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.clickable { viewModel.refreshFinancialSummary(workspaceId) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            snapshot?.let { snap ->
                                TripCard {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
                                    ) {
                                        // Budget Section Title Row with Overflow Menu
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "Budget Ledger", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                                            if (isCallerAdmin) {
                                                Box {
                                                    IconButton(onClick = { showMenu = true }) {
                                                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Trip Menu")
                                                    }
                                                    DropdownMenu(
                                                        expanded = showMenu,
                                                        onDismissRequest = { showMenu = false }
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("Edit Trip") },
                                                            onClick = {
                                                                showMenu = false
                                                                showEditTripDialog = true
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("Archive Trip") },
                                                            onClick = {
                                                                showMenu = false
                                                                showArchiveDialog = true
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Planned: ${CurrencyFormatter.formatMoney(snap.budget, ws.baseCurrency)}", style = MaterialTheme.typography.bodyMedium)
                                            Text(text = "Spent: ${CurrencyFormatter.formatMoney(snap.spent, ws.baseCurrency)}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        val budgetProgress = if (snap.budget.compareTo(BigDecimal.ZERO) > 0) {
                                            snap.spent.divide(snap.budget, 2, RoundingMode.HALF_UP).toFloat()
                                        } else 0f
                                        LinearProgressIndicator(
                                            progress = { budgetProgress.coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.error,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Text(
                                            text = "Remaining Budget: ${CurrencyFormatter.formatMoney(snap.budget.subtract(snap.spent), ws.baseCurrency)}",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = TripSpacing.XS))

                                        // Fund Section
                                        Text(text = "Trip Fund", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Funded: ${CurrencyFormatter.formatMoney(snap.currentFund, ws.baseCurrency)}", style = MaterialTheme.typography.bodyMedium)
                                            Text(text = "Gap: ${CurrencyFormatter.formatMoney(snap.fundingGap, ws.baseCurrency)}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        val fundProgress = if (snap.budget.compareTo(BigDecimal.ZERO) > 0) {
                                            snap.currentFund.divide(snap.budget, 2, RoundingMode.HALF_UP).toFloat()
                                        } else 0f
                                        LinearProgressIndicator(
                                            progress = { fundProgress.coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = TripSpacing.XS))

                                        // Member Summary
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Members: ${snap.memberCount} Joined / ${ws.plannedMemberCount} Expected", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Member Contributions",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(top = TripSpacing.S)
                            )
                        }

                        snapshot?.let { snap ->
                            items(snap.contributions) { member ->
                                val isSelf = member.userId == currentUserId
                                val isAdminMember = member.role == "ADMIN"

                                TripCard {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(text = member.name, style = MaterialTheme.typography.titleMedium)
                                                    if (isSelf) {
                                                        Spacer(modifier = Modifier.width(TripSpacing.XS))
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "You",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(TripSpacing.XS))
                                                Surface(
                                                    color = if (isAdminMember) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (isAdminMember) "ADMIN" else "MEMBER",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isAdminMember) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = member.status.replace("_", " "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (member.status == "FULLY_FUNDED" || member.status == "OVER_FUNDED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(TripSpacing.XS))
                                        Column {
                                            if (ws.contributionMode == "INDIVIDUAL") {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(text = "Planned: ${CurrencyFormatter.formatMoney(member.planned, ws.baseCurrency)}", style = MaterialTheme.typography.bodySmall)
                                                    Text(text = "Consumed: ${CurrencyFormatter.formatMoney(member.total, ws.baseCurrency)}", style = MaterialTheme.typography.bodySmall)
                                                }
                                                val remainingBudget = member.planned.subtract(member.total).max(BigDecimal.ZERO)
                                                val settlementAmount = member.fronted.subtract(member.total)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = TripSpacing.XS),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(text = "Fronted: ${CurrencyFormatter.formatMoney(member.fronted, ws.baseCurrency)}", style = MaterialTheme.typography.bodySmall)
                                                    Text(text = "Remaining: ${CurrencyFormatter.formatMoney(remainingBudget, ws.baseCurrency)}", style = MaterialTheme.typography.bodySmall)
                                                }
                                                val settlementText = when {
                                                    settlementAmount.compareTo(BigDecimal.ZERO) > 0 -> "Settlement: Receives ${CurrencyFormatter.formatMoney(settlementAmount, ws.baseCurrency)}"
                                                    settlementAmount.compareTo(BigDecimal.ZERO) < 0 -> "Settlement: Pays ${CurrencyFormatter.formatMoney(settlementAmount.abs(), ws.baseCurrency)}"
                                                    else -> "Settlement: Settled (₹0)"
                                                }
                                                Text(
                                                    text = settlementText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (settlementAmount.compareTo(BigDecimal.ZERO) >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(top = TripSpacing.XS)
                                                )
                                            } else {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(text = "Contributed: ${CurrencyFormatter.formatMoney(member.total, ws.baseCurrency)}", style = MaterialTheme.typography.bodySmall)
                                                    Text(text = "Planned: ${CurrencyFormatter.formatMoney(member.planned, ws.baseCurrency)}", style = MaterialTheme.typography.bodySmall)
                                                }
                                                if (member.remaining.compareTo(BigDecimal.ZERO) > 0) {
                                                    Text(
                                                        text = "Remaining: ${CurrencyFormatter.formatMoney(member.remaining, ws.baseCurrency)}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.padding(top = TripSpacing.XS)
                                                    )
                                                }
                                            }
                                        }

                                        // Administrative actions for Admin on other members
                                        val canManageTarget = isCallerAdmin && !isSelf && (member.role == "MEMBER" || isCallerPrimaryAdmin)
                                        if (canManageTarget) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = TripSpacing.XS))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(onClick = { selectedMemberForManagement = member }) {
                                                    Text("Manage")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            if (pullToRefreshState.isRefreshing || pullToRefreshState.progress > 0f) {
                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

            // Bottom Navigation & Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
                ) {
                    TripButton(
                        text = "Back",
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    )
                    TripButton(
                        text = "Invite",
                        onClick = { onInviteMembers(ws.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
                ) {
                    TripButton(
                        text = "Timeline",
                        onClick = { onNavigateToExpenses(ws.id) },
                        modifier = Modifier.weight(1f)
                    )
                    TripButton(
                        text = "Settle",
                        onClick = { onNavigateToSettlements(ws.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
                ) {
                    if (isCallerAdmin) {
                        TripButton(
                            text = "Verify Payments",
                            onClick = { onNavigateToVerification(ws.id) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        TripButton(
                            text = "Submit Receipt",
                            onClick = { onNavigateToSubmission(ws.id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    TripButton(
                        text = "Leave Trip",
                        onClick = { showLeaveDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    // Modal Dialogs

    // 1. Manage Member Dialog
    selectedMemberForManagement?.let { target ->
        AlertDialog(
            onDismissRequest = { selectedMemberForManagement = null },
            title = { Text(text = "Manage ${target.name}") },
            text = { Text(text = "Role: ${target.role}") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(TripSpacing.XS)
                ) {
                    if (target.role == "MEMBER") {
                        Button(
                            onClick = {
                                viewModel.updateMemberRole(
                                    workspaceId = workspaceId,
                                    userId = target.userId,
                                    role = "ADMIN",
                                    onSuccess = { selectedMemberForManagement = null },
                                    onError = { msg -> errorMessageDialog = msg }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Promote to Admin")
                        }
                        Button(
                            onClick = {
                                viewModel.removeMember(
                                    workspaceId = workspaceId,
                                    userId = target.userId,
                                    onSuccess = { selectedMemberForManagement = null },
                                    onError = { msg -> errorMessageDialog = msg }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove from Trip")
                        }
                    } else if (target.role == "ADMIN" && isCallerPrimaryAdmin) {
                        Button(
                            onClick = {
                                viewModel.updateMemberRole(
                                    workspaceId = workspaceId,
                                    userId = target.userId,
                                    role = "MEMBER",
                                    onSuccess = { selectedMemberForManagement = null },
                                    onError = { msg -> errorMessageDialog = msg }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Demote to Member")
                        }
                    } else {
                        Text(
                            text = "Admin users cannot be demoted or removed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMemberForManagement = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Edit Trip Dialog (Name, description, start date, end date, budget, planned member count, contribution mode)
    if (showEditTripDialog && workspace != null) {
        val ws = workspace!!
        var name by remember { mutableStateOf(ws.name) }
        var description by remember { mutableStateOf(ws.description ?: "") }
        var startDateText by remember { mutableStateOf(ws.startDate.toString()) }
        var endDateText by remember { mutableStateOf(ws.endDate.toString()) }
        var budgetText by remember { mutableStateOf(ws.budget?.toPlainString() ?: "0") }
        var plannedMemberCountText by remember { mutableStateOf(ws.plannedMemberCount.toString()) }
        var selectedMode by remember { mutableStateOf(ws.contributionMode) }

        val hasFinancialActivity = (snapshot?.spent?.compareTo(BigDecimal.ZERO) ?: 0) > 0 || (snapshot?.currentFund?.compareTo(BigDecimal.ZERO) ?: 0) > 0

        AlertDialog(
            onDismissRequest = { showEditTripDialog = false },
            title = { Text("Edit Trip Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(TripSpacing.S)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Trip Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
                    ) {
                        OutlinedTextField(
                            value = startDateText,
                            onValueChange = { startDateText = it },
                            label = { Text("Start Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endDateText,
                            onValueChange = { endDateText = it },
                            label = { Text("End Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it },
                        label = { Text("Total Budget") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = plannedMemberCountText,
                        onValueChange = { plannedMemberCountText = it },
                        label = { Text("Expected Member Count") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(TripSpacing.XS))
                    Text("Contribution Mode", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedMode == "COMBINED",
                            onClick = { if (!hasFinancialActivity) selectedMode = "COMBINED" },
                            enabled = !hasFinancialActivity
                        )
                        Text("Combined Pool", modifier = Modifier.clickable { if (!hasFinancialActivity) selectedMode = "COMBINED" })
                        Spacer(modifier = Modifier.width(TripSpacing.M))
                        RadioButton(
                            selected = selectedMode == "INDIVIDUAL",
                            onClick = { if (!hasFinancialActivity) selectedMode = "INDIVIDUAL" },
                            enabled = !hasFinancialActivity
                        )
                        Text("Individual Spending", modifier = Modifier.clickable { if (!hasFinancialActivity) selectedMode = "INDIVIDUAL" })
                    }
                    if (hasFinancialActivity) {
                        Text(
                            text = "Contribution mode is locked because financial activity has started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedBudget = budgetText.toBigDecimalOrNull()
                        val parsedMembers = plannedMemberCountText.toIntOrNull()
                        val parsedStart = try { LocalDate.parse(startDateText) } catch (e: Exception) { null }
                        val parsedEnd = try { LocalDate.parse(endDateText) } catch (e: Exception) { null }

                        if (parsedStart == null || parsedEnd == null) {
                            errorMessageDialog = "Please enter valid dates in YYYY-MM-DD format"
                            return@Button
                        }
                        if (parsedEnd.isBefore(parsedStart)) {
                            errorMessageDialog = "End date cannot be before start date"
                            return@Button
                        }

                        viewModel.updateWorkspace(
                            workspaceId = workspaceId,
                            name = name,
                            description = description,
                            startDate = parsedStart,
                            endDate = parsedEnd,
                            budget = parsedBudget,
                            plannedMemberCount = parsedMembers,
                            contributionMode = if (!hasFinancialActivity) selectedMode else null,
                            onSuccess = { showEditTripDialog = false },
                            onError = { msg -> errorMessageDialog = msg }
                        )
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTripDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Archive Trip Confirmation Dialog
    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("Archive Trip?") },
            text = { Text("This will remove the trip from your active trips. Financial history will be preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.archiveWorkspace(
                            workspaceId = workspaceId,
                            onSuccess = {
                                showArchiveDialog = false
                                onNavigateBack()
                            },
                            onError = { msg -> errorMessageDialog = msg }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Leave Trip Confirmation Dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Trip?") },
            text = { Text("Are you sure you want to leave this trip?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.leaveWorkspace(
                            workspaceId = workspaceId,
                            onSuccess = {
                                showLeaveDialog = false
                                onNavigateBack()
                            },
                            onError = { msg ->
                                showLeaveDialog = false
                                errorMessageDialog = msg
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 6. Generic Error Notification Dialog
    errorMessageDialog?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMessageDialog = null },
            title = { Text("Notice") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { errorMessageDialog = null }) {
                    Text("OK")
                }
            }
        )
    }
}
