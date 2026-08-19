package com.rkdevstudios.tripledger.features.expense.presentation

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
fun ExpenseTimelineScreen(
    workspaceId: String,
    viewModel: WorkspaceViewModel,
    onAddExpense: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToActivities: () -> Unit
) {
    viewModel.selectWorkspace(workspaceId)
    val timeline by viewModel.currentTimeline.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Trip Timeline",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            TripButton(
                text = "Activities",
                onClick = onNavigateToActivities
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
        ) {
            if (timeline.isEmpty()) {
                item {
                    Text(
                        text = "No expenses recorded yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(TripSpacing.S)
                    )
                }
            } else {
                items(timeline) { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(TripSpacing.S)) {
                        Text(
                            text = group.date.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        group.expenses.forEach { expense ->
                            TripCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = expense.description, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "Paid by: ${expense.paidByName} • ${expense.categoryName}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        text = CurrencyFormatter.formatMoney(expense.amount, expense.currency),
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
                text = "Add Expense",
                onClick = onAddExpense,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
