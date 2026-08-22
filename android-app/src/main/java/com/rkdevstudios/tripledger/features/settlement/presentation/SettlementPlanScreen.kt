package com.rkdevstudios.tripledger.features.settlement.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripCard
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing
import com.rkdevstudios.tripledger.core.utils.CurrencyFormatter
import com.rkdevstudios.tripledger.features.workspace.WorkspaceViewModel

@Composable
fun SettlementPlanScreen(
    workspaceId: String,
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit
) {
    viewModel.selectWorkspace(workspaceId)
    val plan by viewModel.currentPlan.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
    ) {
        Text(
            text = "Settlement Recommendations",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
        ) {
            val transfers = plan?.transfers ?: emptyList()
            if (transfers.isEmpty()) {
                item {
                    Text(
                        text = "All balances settled! No transfers needed.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(TripSpacing.S)
                    )
                }
            } else {
                items(transfers) { transfer ->
                    TripCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(TripSpacing.S),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${transfer.fromUserName} owes ${transfer.toUserName}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = CurrencyFormatter.formatMoney(transfer.amount, transfer.currency),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TripButton(
                                text = "Confirm",
                                onClick = {
                                    viewModel.confirmTransfer(workspaceId, transfer.id, plan?.sessionId ?: "")
                                }
                            )
                        }
                    }
                }
            }
        }

        TripButton(
            text = "Back",
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
