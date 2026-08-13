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
            label = "Trip Name"
        )

        TripTextField(
            value = description,
            onValueChange = { description = it },
            label = "Description (Optional)"
        )

        TripTextField(
            value = baseCurrency,
            onValueChange = { baseCurrency = it },
            label = "Base Currency (ISO)"
        )

        TripTextField(
            value = budget,
            onValueChange = { budget = it },
            label = "Total Budget"
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
                text = "Save",
                onClick = {
                    val budgetDecimal = budget.toBigDecimalOrNull()
                    viewModel.createWorkspace(
                        name = name,
                        description = description.takeIf { it.isNotBlank() },
                        startDate = LocalDate.now(),
                        endDate = LocalDate.now().plusDays(7),
                        baseCurrency = baseCurrency,
                        budget = budgetDecimal
                    )
                    onNavigateBack()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
