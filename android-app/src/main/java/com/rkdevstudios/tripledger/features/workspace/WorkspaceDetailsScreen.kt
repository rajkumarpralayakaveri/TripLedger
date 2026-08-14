package com.rkdevstudios.tripledger.features.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripCard
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun WorkspaceDetailsScreen(
    workspaceId: String,
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit,
    onInviteMembers: (String) -> Unit,
    onNavigateToExpenses: (String) -> Unit,
    onNavigateToSettlements: (String) -> Unit
) {
    LaunchedEffect(workspaceId) {
        viewModel.selectWorkspace(workspaceId)
    }

    val workspace by viewModel.currentWorkspace.collectAsState()
    val snapshot by viewModel.currentFinancialSnapshot.collectAsState()
    val isLoading by viewModel.isLoadingSummary.collectAsState()
    val error by viewModel.summaryError.collectAsState()

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

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
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
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
                ) {
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
                                        Text(text = "Planned: ${ws.baseCurrency} ${snap.budget}", style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "Spent: ${ws.baseCurrency} ${snap.spent}", style = MaterialTheme.typography.bodyMedium)
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
                                        text = "Remaining Budget: ${ws.baseCurrency} ${snap.budget.subtract(snap.spent)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = TripSpacing.XS))

                                    // Fund Section
                                    Text(text = "Trip Fund", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Funded: ${ws.baseCurrency} ${snap.currentFund}", style = MaterialTheme.typography.bodyMedium)
                                        Text(text = "Gap: ${ws.baseCurrency} ${snap.fundingGap}", style = MaterialTheme.typography.bodyMedium)
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
                                        Text(text = "Contributed: ${ws.baseCurrency} ${member.total}", style = MaterialTheme.typography.bodySmall)
                                        Text(text = "Planned: ${ws.baseCurrency} ${member.planned}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (member.remaining.compareTo(BigDecimal.ZERO) > 0) {
                                        Text(
                                            text = "Remaining: ${ws.baseCurrency} ${member.remaining}",
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
