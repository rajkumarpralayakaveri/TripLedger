package com.rkdevstudios.tripledger.features.expense.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripCard
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing
import com.rkdevstudios.tripledger.core.utils.CurrencyFormatter
import com.rkdevstudios.tripledger.features.expense.domain.ExpenseItem
import com.rkdevstudios.tripledger.features.workspace.WorkspaceViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    workspaceId: String,
    expenseId: String,
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit
) {
    val timeline by viewModel.currentTimeline.collectAsState()
    val snapshot by viewModel.currentFinancialSnapshot.collectAsState()
    val members = snapshot?.contributions ?: emptyList()

    val expenseItem: ExpenseItem? = remember(timeline, expenseId) {
        timeline.flatMap { it.expenses }.find { it.id == expenseId }
    }

    var isImageZoomed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
        ) {
            IconButton(onClick = onNavigateBack) {
                Text(
                    text = "←",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Expense Details",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (expenseItem == null) {
            TripCard {
                Text(
                    text = "Expense details not found.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            // Main Details Card
            TripCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
                ) {
                    Text(
                        text = expenseItem.description,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = CurrencyFormatter.formatMoney(expenseItem.amount, expenseItem.currency),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider()

                    Text(
                        text = "Paid by: ${expenseItem.paidByName}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    val formattedDate = try {
                        expenseItem.date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
                    } catch (e: Exception) { expenseItem.date.toString() }

                    val formattedTime = if (!expenseItem.expenseAt.isNullOrEmpty()) {
                        try {
                            java.time.Instant.parse(expenseItem.expenseAt)
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("h:mm a"))
                        } catch (e: Exception) {
                            try {
                                java.time.LocalDateTime.parse(expenseItem.expenseAt)
                                    .format(DateTimeFormatter.ofPattern("h:mm a"))
                            } catch (e2: Exception) { "" }
                        }
                    } else ""

                    val formattedDateTime = if (formattedTime.isNotBlank()) "$formattedDate · $formattedTime" else formattedDate

                    Text(
                        text = formattedDateTime,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    if (!expenseItem.note.isNullOrEmpty()) {
                        Text(
                            text = "Note: ${expenseItem.note}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Split Allocations Card
            TripCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Split Participants",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Type: ${expenseItem.splitType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (expenseItem.splitAllocations.isNotEmpty()) {
                        expenseItem.splitAllocations.forEach { alloc ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (expenseItem.splitType != "EQUAL" && expenseItem.splitType != "EXACT") {
                                        "${alloc.name} (${alloc.rawValue})"
                                    } else {
                                        alloc.name
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = CurrencyFormatter.formatMoney(alloc.amount, alloc.currency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    } else {
                        // Fallback to simple members division if allocations list is empty
                        val participantCount = if (members.isNotEmpty()) members.size else 1
                        val perPersonAmount = expenseItem.amount.divide(
                            BigDecimal.valueOf(participantCount.toLong()),
                            2,
                            RoundingMode.HALF_UP
                        )
                        if (members.isNotEmpty()) {
                            members.forEach { member ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = member.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = CurrencyFormatter.formatMoney(perPersonAmount, expenseItem.currency),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = expenseItem.paidByName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = CurrencyFormatter.formatMoney(expenseItem.amount, expenseItem.currency),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Receipt Display Card with Coil Image Loader
            TripCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
                ) {
                    Text(
                        text = "Receipt Proof",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!expenseItem.receiptUrl.isNullOrEmpty()) {
                        coil.compose.SubcomposeAsyncImage(
                            model = expenseItem.receiptUrl,
                            contentDescription = "Receipt Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clickable { isImageZoomed = true },
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            },
                            error = {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Failed to load image",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = expenseItem.receiptUrl ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                        TripButton(
                            text = "View Larger Receipt",
                            onClick = { isImageZoomed = true }
                        )
                    } else {
                        Text(
                            text = "No receipt attached",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (isImageZoomed && expenseItem?.receiptUrl != null) {
        Dialog(onDismissRequest = { isImageZoomed = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TripSpacing.M),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(TripSpacing.M),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
                ) {
                    Text(
                        text = "Receipt Image",
                        style = MaterialTheme.typography.titleLarge
                    )
                    coil.compose.AsyncImage(
                        model = expenseItem.receiptUrl,
                        contentDescription = "Full Screen Receipt Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                    )
                    TripButton(
                        text = "Close",
                        onClick = { isImageZoomed = false }
                    )
                }
            }
        }
    }
}
