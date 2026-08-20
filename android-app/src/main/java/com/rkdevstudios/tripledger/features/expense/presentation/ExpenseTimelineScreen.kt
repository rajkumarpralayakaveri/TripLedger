package com.rkdevstudios.tripledger.features.expense.presentation

import androidx.compose.foundation.clickable
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
    onExpenseClick: (String) -> Unit = {},
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
                    val groupFormattedDate = try {
                        group.date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
                    } catch (e: Exception) { group.date.toString() }

                    Column(verticalArrangement = Arrangement.spacedBy(TripSpacing.S)) {
                        Text(
                            text = groupFormattedDate,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        group.expenses.forEach { expense ->
                            TripCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onExpenseClick(expense.id) }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(TripSpacing.XS)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = expense.description, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = "Paid by: ${expense.paidByName}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Text(
                                            text = CurrencyFormatter.formatMoney(expense.amount, expense.currency),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    val formattedDateTime = try {
                                        val dateStr = expense.date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
                                        val timeStr = if (!expense.expenseAt.isNullOrEmpty()) {
                                            try {
                                                java.time.Instant.parse(expense.expenseAt)
                                                    .atZone(java.time.ZoneId.systemDefault())
                                                    .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
                                            } catch (e: Exception) {
                                                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
                                            }
                                        } else {
                                            java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
                                        }
                                        "$dateStr · $timeStr"
                                    } catch (e: Exception) {
                                        expense.date.toString()
                                    }

                                    Text(
                                        text = formattedDateTime,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )

                                    if (!expense.note.isNullOrEmpty()) {
                                        Text(
                                            text = "Note: ${expense.note}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (!expense.receiptUrl.isNullOrEmpty()) {
                                        Text(
                                            text = "📄 Receipt attached",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
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
