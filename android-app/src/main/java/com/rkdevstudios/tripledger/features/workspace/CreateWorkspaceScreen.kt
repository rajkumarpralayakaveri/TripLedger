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
    var contributionMode by remember { mutableStateOf("COMBINED") }
    var startDateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDateText by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
        ) {
            TripTextField(
                value = startDateText,
                onValueChange = { startDateText = it },
                label = "Start Date (YYYY-MM-DD)",
                enabled = !isCreating,
                modifier = Modifier.weight(1f)
            )
            TripTextField(
                value = endDateText,
                onValueChange = { endDateText = it },
                label = "End Date (YYYY-MM-DD)",
                enabled = !isCreating,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Base Currency",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = TripSpacing.S)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
        ) {
            listOf("INR", "USD", "EUR", "GBP").forEach { curr ->
                val isSelected = baseCurrency == curr
                androidx.compose.material3.OutlinedButton(
                    onClick = { baseCurrency = curr },
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
                    ),
                    enabled = !isCreating,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = curr,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        TripTextField(
            value = budget,
            onValueChange = { budget = it },
            label = "Total Budget",
            enabled = !isCreating
        )

        Text(
            text = "Contribution Mode",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = TripSpacing.S)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
        ) {
            val modes = listOf("COMBINED" to "Combined Pool", "INDIVIDUAL" to "Individual Spending")
            modes.forEach { (modeKey, modeLabel) ->
                val isSelected = contributionMode == modeKey
                androidx.compose.material3.OutlinedButton(
                    onClick = { contributionMode = modeKey },
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
                    ),
                    enabled = !isCreating,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = modeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

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
                    val parsedStart = try { LocalDate.parse(startDateText) } catch (e: Exception) { null }
                    val parsedEnd = try { LocalDate.parse(endDateText) } catch (e: Exception) { null }

                    if (parsedStart == null || parsedEnd == null) {
                        errorMessage = "Please enter valid dates in YYYY-MM-DD format"
                        return@TripButton
                    }
                    if (parsedEnd.isBefore(parsedStart)) {
                        errorMessage = "End date cannot be before start date"
                        return@TripButton
                    }

                    val budgetDecimal = budget.toBigDecimalOrNull()
                    val members = plannedMemberCount.toIntOrNull() ?: 1
                    viewModel.createWorkspace(
                        name = name,
                        description = description.takeIf { it.isNotBlank() },
                        startDate = parsedStart,
                        endDate = parsedEnd,
                        baseCurrency = baseCurrency,
                        budget = budgetDecimal,
                        plannedMemberCount = members,
                        contributionMode = contributionMode,
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
