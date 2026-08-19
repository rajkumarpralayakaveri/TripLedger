package com.rkdevstudios.tripledger.features.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripCard
import com.rkdevstudios.tripledger.core.designsystem.components.TripTextField
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentVerificationScreen(
    workspaceId: String,
    viewModel: PaymentProofViewModel,
    verifierId: String,
    onNavigateBack: () -> Unit
) {
    val payments by viewModel.payments.collectAsState()
    val isVerifying by viewModel.isVerifyingPayment.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var rejectTargetId by remember { mutableStateOf<String?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(workspaceId) {
        viewModel.loadPayments(workspaceId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.M)
    ) {
        Text(
            text = "Verification Queue",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        error?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
        ) {
            val pendingProofs = payments.filter { it.status == "PENDING" }
            if (pendingProofs.isEmpty()) {
                item {
                    Text(
                        text = "No pending payments in the queue.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                items(pendingProofs) { item ->
                    TripCard {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Payer: ${item.payerName}", style = MaterialTheme.typography.titleMedium)
                                    Text(text = "Amount: ₹${item.amount}", style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(
                                    text = item.status,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            item.viewUrl?.let { url: String ->
                                SubcomposeAsyncImage(
                                    model = url,
                                    contentDescription = "Receipt receipt image",
                                    loading = {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        }
                                    },
                                    error = {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Text(
                                                text = "Failed to load receipt image",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clickable { previewImageUrl = url }
                                )
                            }

                            if (item.userId == verifierId) {
                                Text(
                                    text = "Self-approval is forbidden. Another admin must approve.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
                                ) {
                                    TripButton(
                                        text = "Approve",
                                        onClick = { viewModel.approvePayment(workspaceId, item.id) },
                                        enabled = !isVerifying,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TripButton(
                                        text = "Reject",
                                        onClick = {
                                            rejectTargetId = item.id
                                            rejectReason = ""
                                        },
                                        enabled = !isVerifying,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rejection Dialog
        if (rejectTargetId != null) {
            AlertDialog(
                onDismissRequest = { rejectTargetId = null },
                title = { Text(text = "Reject Payment Proof") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(TripSpacing.S)) {
                        Text(text = "Please specify a reason for rejecting this payment proof.")
                        TripTextField(
                            value = rejectReason,
                            onValueChange = { rejectReason = it },
                            label = "Rejection Reason"
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val targetId = rejectTargetId
                            if (targetId != null && rejectReason.isNotBlank()) {
                                viewModel.rejectPayment(workspaceId, targetId, rejectReason) {
                                    rejectTargetId = null
                                }
                            }
                        },
                        enabled = rejectReason.isNotBlank()
                    ) {
                        Text("Confirm Rejection")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rejectTargetId = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Full Image Preview Dialog
        if (previewImageUrl != null) {
            AlertDialog(
                onDismissRequest = { previewImageUrl = null },
                title = { Text("Receipt Preview") },
                text = {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(400.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = previewImageUrl,
                            contentDescription = "Full receipt image",
                            loading = { CircularProgressIndicator() },
                            error = { Text("Failed to load image", color = MaterialTheme.colorScheme.error) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { previewImageUrl = null }) {
                        Text("Close")
                    }
                }
            )
        }

        TripButton(
            text = "Back",
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
