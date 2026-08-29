package com.rkdevstudios.tripledger.features.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripCard
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing

@Composable
fun InviteMembersScreen(
    workspaceId: String,
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit
) {
    var generatedToken by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
    ) {
        Text(
            text = "Invite Members",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        TripCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "Generate Invite Link", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(TripSpacing.S))

                if (generatedToken.isEmpty()) {
                    TripButton(
                        text = "Generate Token",
                        onClick = {
                            viewModel.createInviteToken(
                                workspaceId = workspaceId,
                                onSuccess = { token ->
                                    generatedToken = token
                                },
                                onError = { error ->
                                    generatedToken = "Error: $error"
                                }
                            )
                        }
                    )
                } else {
                    Text(
                        text = "Invite link: https://tripledger.app/join/$generatedToken",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        TripButton(
            text = "Back",
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
