package com.rkdevstudios.tripledger.features.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripTextField
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun CreateWorkspaceScreen(
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var baseCurrency by remember { mutableStateOf("INR") }
    var budget by remember { mutableStateOf("") }
    var plannedMemberCount by remember { mutableStateOf("5") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isCreating by viewModel.isCreatingWorkspace.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
    ) {
        Text(
            text = "Create New Trip",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = TripSpacing.M)
        )

        TripTextField(
            value = name,
            onValueChange = { name = it },
            label = "Trip Name",
            enabled = !isCreating
        )

        TripTextField(
            value = description,
            onValueChange = { description = it },
            label = "Description (Optional)",
            enabled = !isCreating
        )

        TripTextField(
            value = baseCurrency,
            onValueChange = { baseCurrency = it },
            label = "Base Currency (ISO)",
            enabled = !isCreating
        )

        TripTextField(
            value = budget,
            onValueChange = { budget = it },
            label = "Total Budget",
            enabled = !isCreating
        )

        TripTextField(
            value = plannedMemberCount,
            onValueChange = { plannedMemberCount = it },
            label = "Expected Member Count",
            enabled = !isCreating
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = TripSpacing.S)
            )
        }

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
                text = if (isCreating) "Creating Trip..." else "Save",
                onClick = {
                    errorMessage = null
                    val budgetDecimal = budget.toBigDecimalOrNull()
                    val members = plannedMemberCount.toIntOrNull() ?: 1
                    viewModel.createWorkspace(
                        name = name,
                        description = description.takeIf { it.isNotBlank() },
                        startDate = LocalDate.now(),
                        endDate = LocalDate.now().plusDays(7),
                        baseCurrency = baseCurrency,
                        budget = budgetDecimal,
                        plannedMemberCount = members,
                        onSuccess = onNavigateBack,
                        onError = { errorMessage = it }
                    )
                },
                enabled = name.isNotBlank() && (plannedMemberCount.toIntOrNull() ?: 0) >= 1 && !isCreating,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
