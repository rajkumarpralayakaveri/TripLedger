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

import com.rkdevstudios.tripledger.core.utils.CurrencyFormatter

@Composable
fun BalancesScreen(
    workspaceId: String,
    viewModel: WorkspaceViewModel,
    onNavigateToPlan: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateBack: () -> Unit
) {
    viewModel.selectWorkspace(workspaceId)
    val balances by viewModel.currentBalances.collectAsState()
    val currentWorkspace by viewModel.currentWorkspace.collectAsState()
    val currencyCode = currentWorkspace?.baseCurrency ?: "INR"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
    ) {
        Text(
            text = "Member Balances",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
        ) {
            items(balances) { mb ->
                TripCard {
                    Column(modifier = Modifier.fillMaxWidth().padding(TripSpacing.S)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = mb.userName, style = MaterialTheme.typography.titleMedium)
                            val balanceColor = if (mb.balance >= java.math.BigDecimal.ZERO) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                            val balanceAbs = mb.balance.abs()
                            val formattedBalance = CurrencyFormatter.formatMoney(balanceAbs, currencyCode)
                            val prefix = if (mb.balance > java.math.BigDecimal.ZERO) "+" else if (mb.balance < java.math.BigDecimal.ZERO) "-" else ""
                            Text(
                                text = "$prefix$formattedBalance",
                                style = MaterialTheme.typography.titleMedium,
                                color = balanceColor
                            )
                        }
                        Spacer(modifier = Modifier.height(TripSpacing.XS))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Paid: ${CurrencyFormatter.formatMoney(mb.paid, currencyCode)}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "Share: ${CurrencyFormatter.formatMoney(mb.owed, currencyCode)}", style = MaterialTheme.typography.bodySmall)
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
                text = "History",
                onClick = onNavigateToHistory,
                modifier = Modifier.weight(1f)
            )
            TripButton(
                text = "Get Plan",
                onClick = onNavigateToPlan,
                modifier = Modifier.weight(1f)
            )
        }

        TripButton(
            text = "Back",
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
