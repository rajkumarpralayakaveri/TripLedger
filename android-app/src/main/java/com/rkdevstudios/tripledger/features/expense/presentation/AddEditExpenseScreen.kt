package com.rkdevstudios.tripledger.features.expense.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rkdevstudios.tripledger.core.designsystem.components.TripButton
import com.rkdevstudios.tripledger.core.designsystem.components.TripTextField
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing
import com.rkdevstudios.tripledger.features.workspace.WorkspaceViewModel
import java.math.BigDecimal

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    workspaceId: String,
    viewModel: WorkspaceViewModel,
    onNavigateBack: () -> Unit
) {
    val snapshot by viewModel.currentFinancialSnapshot.collectAsState()
    val members = snapshot?.contributions ?: emptyList()

    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val currentSessionUserId = viewModel.sessionManager?.let {
        try {
            com.rkdevstudios.tripledger.core.auth.SharedPreferencesSessionStore(appContext).getSession()?.userId.orEmpty()
        } catch (e: Exception) { "" }
    } ?: ""
    val currentUserMember = members.find { it.userId == currentSessionUserId } ?: members.firstOrNull()

    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf("cat_general") }
    var note by remember { mutableStateOf("") }
    var receiptUrl by remember { mutableStateOf<String?>(null) }

    var selectedPayerUserId by remember(members) {
        mutableStateOf(currentUserMember?.userId ?: "")
    }

    val isSaving by viewModel.isSavingExpense.collectAsState()
    var isPayerMenuExpanded by remember { mutableStateOf(false) }

    val selectedPayerName = members.find { it.userId == selectedPayerUserId }?.name ?: "Select Payer"

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

        // Dynamic Paid By Member Dropdown Selector with Trailing Chevron Icon
        ExposedDropdownMenuBox(
            expanded = isPayerMenuExpanded,
            onExpandedChange = { if (!isSaving) isPayerMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedPayerName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Paid By") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPayerMenuExpanded) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = isPayerMenuExpanded,
                onDismissRequest = { isPayerMenuExpanded = false }
            ) {
                members.forEach { member ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (member.userId == currentSessionUserId) "${member.name} (You)" else member.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            selectedPayerUserId = member.userId
                            isPayerMenuExpanded = false
                        }
                    )
                }
            }
        }



    val context = androidx.compose.ui.platform.LocalContext.current
    val contentResolver = remember { context.contentResolver }
    var isUploadingReceipt by remember { mutableStateOf(false) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                val bytes = contentResolver.openInputStream(it)?.readBytes()
                if (bytes != null) {
                    selectedFileName = "Selected receipt (${bytes.size / 1024} KB)"
                    isUploadingReceipt = true
                    viewModel.uploadExpenseReceipt(
                        workspaceId = workspaceId,
                        fileBytes = bytes,
                        onSuccess = { url ->
                            receiptUrl = url
                            isUploadingReceipt = false
                        },
                        onError = { _ ->
                            isUploadingReceipt = false
                        }
                    )
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Optional Upload Receipt Control
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TripSpacing.XS)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isUploadingReceipt -> "Uploading receipt..."
                    receiptUrl != null -> "Receipt attached"
                    selectedFileName != null -> selectedFileName!!
                    else -> "No receipt attached"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (receiptUrl != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            TripButton(
                text = if (receiptUrl != null) "Change Receipt" else "Add Receipt",
                onClick = { launcher.launch("image/*") },
                enabled = !isSaving && !isUploadingReceipt
            )
        }
    }

        TripTextField(
            value = note,
            onValueChange = { note = it },
            label = "Note (Optional)",
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
                    val allParticipantIds = members.map { it.userId }
                    viewModel.createExpense(
                        workspaceId = workspaceId,
                        paidByUserId = selectedPayerUserId,
                        amount = amountVal,
                        currency = "INR",
                        description = description,
                        categoryId = categoryId,
                        expenseDate = java.time.LocalDate.now(),
                        participantIds = if (allParticipantIds.isNotEmpty()) allParticipantIds else listOf(selectedPayerUserId),
                        expenseAt = java.time.Instant.now().toString(),
                        receiptUrl = receiptUrl,
                        note = note.ifBlank { null },
                        onSuccess = { onNavigateBack() }
                    )
                },
                enabled = description.isNotBlank() && amount.isNotBlank() && selectedPayerUserId.isNotBlank() && !isSaving,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
