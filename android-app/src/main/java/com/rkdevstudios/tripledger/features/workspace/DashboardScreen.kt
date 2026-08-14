package com.rkdevstudios.tripledger.features.workspace

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

@Composable
fun DashboardScreen(
    viewModel: WorkspaceViewModel,
    onCreateWorkspace: () -> Unit,
    onJoinWorkspace: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val workspaces by viewModel.workspaces.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M)
    ) {
        Text(
            text = "My Trips",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = TripSpacing.M)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
        ) {
            items(workspaces) { ws ->
                TripCard(
                    modifier = Modifier.clickable { onNavigateToDetails(ws.id) }
                ) {
                    Column {
                        Text(text = ws.name, style = MaterialTheme.typography.titleLarge)
                        ws.description?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = TripSpacing.XS)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = TripSpacing.S),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Status: ${ws.status}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "Members: ${ws.membersCount} Joined / ${ws.plannedMemberCount} Expected",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = TripSpacing.M),
            horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
        ) {
            TripButton(
                text = "Create Trip",
                onClick = onCreateWorkspace,
                modifier = Modifier.weight(1f)
            )
            TripButton(
                text = "Join Trip",
                onClick = onJoinWorkspace,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
