package com.rkdevstudios.tripledger.features.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripCard
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing
import com.rkdevstudios.tripledger.core.utils.CurrencyFormatter
import java.math.BigDecimal
import java.math.RoundingMode

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
    ) {
        workspace?.let { ws ->
            Text(
                text = ws.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

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
                                        // Budget Section
                                        Text(text = "Budget Ledger", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
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
                                TripCard {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = member.name, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    text = member.role,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            Text(
                                                text = member.status.replace("_", " "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (member.status == "FULLY_FUNDED" || member.status == "OVER_FUNDED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(TripSpacing.XS))
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
                            }
                        }
                    }
                }

                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            val currentUserRole = snapshot?.contributions?.find { it.userId == currentUserId }?.role ?: "MEMBER"

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
                if (currentUserRole == "OWNER" || currentUserRole == "ADMIN") {
                    TripButton(
                        text = "Verify Payments",
                        onClick = { onNavigateToVerification(ws.id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    TripButton(
                        text = "Submit Receipt / Proof",
                        onClick = { onNavigateToSubmission(ws.id) },
                        modifier = Modifier.fillMaxWidth()
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
}
