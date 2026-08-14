package com.rkdevstudios.tripledger.features.expense.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripTextField
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing
import com.rkdevstudios.tripledger.features.workspace.WorkspaceViewModel
import java.math.BigDecimal

@Composable
fun AddEditExpenseScreen(
    workspaceId: String,
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var payer by remember { mutableStateOf("Raj") }

    val isSaving by viewModel.isSavingExpense.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
    ) {
        Text(
            text = "Add Expense",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = TripSpacing.M)
        )

        TripTextField(
            value = description,
            onValueChange = { description = it },
            label = "Description",
            enabled = !isSaving
        )

        TripTextField(
            value = amount,
            onValueChange = { amount = it },
            label = "Amount",
            enabled = !isSaving
        )

        TripTextField(
            value = category,
            onValueChange = { category = it },
            label = "Category (e.g. Food, Transport)",
            enabled = !isSaving
        )

        TripTextField(
            value = payer,
            onValueChange = { payer = it },
            label = "Paid By",
            enabled = !isSaving
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
        ) {
            TripButton(
                text = "Cancel",
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            )
            TripButton(
                text = if (isSaving) "Saving..." else "Save",
                onClick = {
                    val amountVal = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    viewModel.addMockExpense(
                        workspaceId = workspaceId,
                        description = description,
                        amount = amountVal,
                        currency = "INR",
                        paidByName = payer,
                        categoryName = category,
                        categoryColor = "#FF9800",
                        categoryIcon = "restaurant"
                    )
                    onNavigateBack()
                },
                enabled = description.isNotBlank() && amount.isNotBlank() && !isSaving,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
