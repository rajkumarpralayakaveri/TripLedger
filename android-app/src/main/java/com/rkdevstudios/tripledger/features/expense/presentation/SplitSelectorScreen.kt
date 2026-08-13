package com.rkdevstudios.tripledger.features.expense.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripCard
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing
import com.rkdevstudios.tripledger.features.workspace.WorkspaceViewModel

@Composable
fun SplitSelectorScreen(
    workspaceId: String,
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit
) {
    viewModel.selectWorkspace(workspaceId)
    val snapshot by viewModel.currentFinancialSnapshot.collectAsState()

    // Map of member name to checked status
    val memberCheckStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(snapshot) {
        snapshot?.contributions?.forEach { member ->
            if (!memberCheckStates.containsKey(member.name)) {
                memberCheckStates[member.name] = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
    ) {
        Text(
            text = "Select Splits",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
        ) {
            snapshot?.contributions?.let { contributions ->
                items(contributions) { member ->
                    val isChecked = memberCheckStates[member.name] ?: true
                    TripCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = member.name, style = MaterialTheme.typography.titleMedium)
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { memberCheckStates[member.name] = it }
                            )
                        }
                    }
                }
            }
        }

        TripButton(
            text = "Done",
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
