package com.rkdevstudios.tripledger.features.workspace

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripCard
import com.rkdevstudios.tripledger.core.designsystem.components.TripTextField
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing
import com.rkdevstudios.tripledger.core.utils.CurrencyFormatter
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSubmissionScreen(
    workspaceId: String,
    viewModel: PaymentProofViewModel,
    workspaceViewModel: WorkspaceViewModel,
    currentUserId: String,
    onNavigateBack: () -> Unit
) {
    workspaceViewModel.selectWorkspace(workspaceId)
    val currentWorkspace by workspaceViewModel.currentWorkspace.collectAsState()
    val currencyCode = currentWorkspace?.baseCurrency ?: "INR"

    val context = LocalContext.current
    val contentResolver = remember { context.contentResolver }

    var amountStr by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBytes by remember { mutableStateOf<ByteArray?>(null) }

    val payments by viewModel.payments.collectAsState()
    val isRequestingSignature by viewModel.isRequestingSignature.collectAsState()
    val isUploadingToCloudinary by viewModel.isUploadingToCloudinary.collectAsState()
    val isCompletingUpload by viewModel.isCompletingUpload.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val isSubmitting = isRequestingSignature || isUploadingToCloudinary || isCompletingUpload

    LaunchedEffect(workspaceId) {
        viewModel.loadPayments(workspaceId)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            try {
                selectedBytes = contentResolver.openInputStream(it)?.readBytes()
            } catch (e: Exception) {
                // ignore
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
            text = "Submit Payment Proof",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Submit Form Card
        TripCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
            ) {
                TripTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = "Amount",
                    enabled = !isSubmitting
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(TripSpacing.S)
                ) {
                    TripButton(
                        text = if (selectedUri != null) "Change Receipt" else "Select Receipt Image",
                        onClick = { launcher.launch("image/*") },
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f)
                    )
                }

                selectedUri?.let { uri ->
                    Text(
                        text = "Selected image: ${uri.lastPathSegment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                if (isSubmitting) {
                    val statusText = when {
                        isRequestingSignature -> "Requesting upload parameters..."
                        isUploadingToCloudinary -> "Uploading receipt image to Cloudinary..."
                        isCompletingUpload -> "Submitting payment for server verification..."
                        else -> "Processing..."
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = TripSpacing.S)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(TripSpacing.XS))
                        Text(text = statusText, style = MaterialTheme.typography.bodySmall)
                    }
                }

                error?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                TripButton(
                    text = "Submit Proof",
                    onClick = {
                        val amountVal = amountStr.toBigDecimalOrNull()
                        val bytes = selectedBytes
                        if (amountVal == null || amountVal.compareTo(BigDecimal.ZERO) <= 0) {
                            viewModel.clearError()
                            // Set custom validation error
                        } else if (bytes == null) {
                            viewModel.clearError()
                        } else {
                            viewModel.submitPayment(workspaceId, amountVal, bytes) {
                                amountStr = ""
                                selectedUri = null
                                selectedBytes = null
                            }
                        }
                    },
                    enabled = !isSubmitting && amountStr.isNotBlank() && selectedUri != null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // History Section
        Text(
            text = "Your Submissions",
            style = MaterialTheme.typography.titleMedium
        )

        val myPayments = payments.filter { it.userId == currentUserId }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TripSpacing.S)
        ) {
            if (myPayments.isEmpty()) {
                item {
                    Text(
                        text = "No submissions recorded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                items(myPayments) { item ->
                    TripCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Amount: ${CurrencyFormatter.formatMoney(item.amount, currencyCode)}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = item.status,
                                    color = when (item.status) {
                                        "APPROVED" -> MaterialTheme.colorScheme.primary
                                        "REJECTED" -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.secondary
                                    },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            Text(
                                text = "Created: ${item.createdAt.take(10)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            item.rejectionReason?.let { reason ->
                                Text(
                                    text = "Rejection Reason: $reason",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = TripSpacing.XS)
                                )
                            }
                        }
                    }
                }
            }
        }

        TripButton(
            text = "Back",
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
