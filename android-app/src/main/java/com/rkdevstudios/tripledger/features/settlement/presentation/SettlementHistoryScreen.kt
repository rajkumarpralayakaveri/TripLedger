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
import com.rkdevstudios.tripledger.features.workspace.WorkspaceViewModel

@Composable
fun SettlementHistoryScreen(
    workspaceId: String,
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit
) {
    viewModel.selectWorkspace(workspaceId)
    val history by viewModel.currentHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
    ) {
        Text(
            text = "Settlement History",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
        ) {
            if (history.isEmpty()) {
                item {
                    Text(
                        text = "No settlement history recorded.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(TripSpacing.S)
                    )
                }
            } else {
                items(history) { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(TripSpacing.S)) {
                        Text(
                            text = group.date.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        group.transactions.forEach { tx ->
                            TripCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(TripSpacing.S),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "${tx.fromUserName} Paid ${tx.toUserName}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = tx.confirmedAt,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        text = "${tx.currency} ${tx.amount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
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
