package com.rkdevstudios.tripledger.features.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripCard
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: WorkspaceViewModel,
    onCreateWorkspace: () -> Unit,
    onJoinWorkspace: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val workspaces by viewModel.workspaces.collectAsState()
    val isLoading by viewModel.isLoadingWorkspaces.collectAsState()
    val error by viewModel.workspacesError.collectAsState()
    val isRefreshing by viewModel.isRefreshingWorkspaces.collectAsState()
    val serverStatus by com.rkdevstudios.tripledger.core.network.ServerState.status.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshWorkspaces()
        }
    }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            pullToRefreshState.startRefresh()
        } else {
            pullToRefreshState.endRefresh()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadWorkspaces()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M)
    ) {
        if (serverStatus is com.rkdevstudios.tripledger.core.network.ServerStatus.WakingUp) {
            val msg = (serverStatus as com.rkdevstudios.tripledger.core.network.ServerStatus.WakingUp).message
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = TripSpacing.M)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(TripSpacing.M),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
                ) {
                    com.rkdevstudios.tripledger.core.designsystem.components.TripLoadingIndicator()
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Text(
            text = "My Trips",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = TripSpacing.M)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            if (isLoading && workspaces.isEmpty()) {
                // Skeleton loading state
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
                ) {
                    items(3) {
                        TripCard {
                            Column(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(20.dp)
                                )
                                Spacer(modifier = Modifier.height(TripSpacing.S))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.3f)
                                        .height(14.dp)
                                )
                            }
                        }
                    }
                }
            } else if (error != null && workspaces.isEmpty()) {
                // Initial load error state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error ?: "Unable to load trips",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = TripSpacing.M)
                    )
                    TripButton(
                        text = "Retry",
                        onClick = { viewModel.loadWorkspaces() }
                    )
                }
            } else if (workspaces.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No trips planned yet",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = TripSpacing.S)
                    )
                    Text(
                        text = "Create a new trip or join an existing one to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = TripSpacing.L)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
            } else {
                // Success state with content
                Column(modifier = Modifier.fillMaxSize()) {
                    if (error != null) {
                        TripCard(
                            modifier = Modifier.padding(bottom = TripSpacing.S)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(TripSpacing.XS),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Couldn't refresh trips.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Retry",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.clickable { viewModel.refreshWorkspaces() }
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                }
            }

            if (pullToRefreshState.isRefreshing || pullToRefreshState.progress > 0f) {
                PullToRefreshContainer(
                    state = pullToRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }

        if (workspaces.isNotEmpty()) {
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
}
