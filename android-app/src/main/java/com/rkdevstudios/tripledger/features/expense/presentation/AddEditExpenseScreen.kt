package com.rkdevstudios.tripledger.features.expense.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    // Phase 4 Split Selection State variables
    val splitTypes = listOf("EQUAL", "EXACT", "PERCENTAGE", "SHARES")
    var selectedSplitType by remember { mutableStateOf("EQUAL") }
    var isSplitTypeMenuExpanded by remember { mutableStateOf(false) }

    // Participant IDs selection (defaults to all)
    var selectedParticipantIds by remember(members) {
        mutableStateOf(members.map { it.userId }.toSet())
    }

    // Split raw coefficient values mapping userId to raw String representation
    var splitInputs by remember(members) {
        mutableStateOf(members.associate { it.userId to "" })
    }

    // UI Validations
    val amountVal = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val errorText = remember(amount, selectedSplitType, selectedParticipantIds, splitInputs) {
        if (amountVal <= BigDecimal.ZERO) {
            "Amount must be positive"
        } else if (selectedParticipantIds.isEmpty()) {
            "Select at least one participant"
        } else {
            when (selectedSplitType) {
                "EXACT" -> {
                    val sumExact = selectedParticipantIds.sumOf { splitInputs[it]?.toBigDecimalOrNull() ?: BigDecimal.ZERO }
                    if (sumExact.compareTo(amountVal) != 0) {
                        "Sum of exact amounts ($sumExact) must equal total expense ($amountVal)"
                    } else null
                }
                "PERCENTAGE" -> {
                    val sumPercent = selectedParticipantIds.sumOf { splitInputs[it]?.toBigDecimalOrNull() ?: BigDecimal.ZERO }
                    if (sumPercent.compareTo(BigDecimal.valueOf(100)) != 0) {
                        "Percentages must sum to exactly 100% (currently $sumPercent%)"
                    } else null
                }
                "SHARES" -> {
                    val sumShares = selectedParticipantIds.sumOf { splitInputs[it]?.toBigDecimalOrNull() ?: BigDecimal.ZERO }
                    if (sumShares <= BigDecimal.ZERO) {
                        "Sum of shares must be greater than zero"
                    } else if (selectedParticipantIds.any { (splitInputs[it]?.toBigDecimalOrNull() ?: BigDecimal.ZERO) < BigDecimal.ZERO }) {
                        "Individual shares cannot be negative"
                    } else null
                }
                else -> null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(TripSpacing.M)
            .verticalScroll(rememberScrollState()),
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

        // Paid By Member Dropdown Selector
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

        // Split Type Selector
        ExposedDropdownMenuBox(
            expanded = isSplitTypeMenuExpanded,
            onExpandedChange = { if (!isSaving) isSplitTypeMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedSplitType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Split Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSplitTypeMenuExpanded) },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = isSplitTypeMenuExpanded,
                onDismissRequest = { isSplitTypeMenuExpanded = false }
            ) {
                splitTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            selectedSplitType = type
                            isSplitTypeMenuExpanded = false
                        }
                    )
                }
            }
        }

        // Dynamic Splits Editor per member
        Text(
            text = "Split Participants",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = TripSpacing.S)
        )

        members.forEach { member ->
            val isChecked = selectedParticipantIds.contains(member.userId)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            if (!isSaving) {
                                selectedParticipantIds = if (checked) {
                                    selectedParticipantIds + member.userId
                                } else {
                                    selectedParticipantIds - member.userId
                                }
                            }
                        },
                        enabled = !isSaving
                    )
                    Text(text = member.name, style = MaterialTheme.typography.bodyMedium)
                }

                if (isChecked && selectedSplitType != "EQUAL") {
                    OutlinedTextField(
                        value = splitInputs[member.userId] ?: "",
                        onValueChange = { input ->
                            if (!isSaving) {
                                splitInputs = splitInputs + (member.userId to input)
                            }
                        },
                        label = {
                            Text(
                                when (selectedSplitType) {
                                    "EXACT" -> "Amount (₹)"
                                    "PERCENTAGE" -> "Percent (%)"
                                    "SHARES" -> "Shares"
                                    else -> ""
                                }
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier.width(140.dp)
                    )
                } else if (isChecked && selectedSplitType == "EQUAL" && amountVal > BigDecimal.ZERO) {
                    val eqPartCount = if (selectedParticipantIds.isNotEmpty()) selectedParticipantIds.size else 1
                    val share = amountVal.divide(BigDecimal.valueOf(eqPartCount.toLong()), 2, java.math.RoundingMode.HALF_UP)
                    Text(
                        text = "₹$share",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Show computed percentage values beside input text boxes if PERCENTAGE is selected
        if (selectedSplitType == "PERCENTAGE" && amountVal > BigDecimal.ZERO) {
            Text(
                text = "Computed Splits:",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            selectedParticipantIds.forEach { userId ->
                val name = members.find { it.userId == userId }?.name.orEmpty()
                val pct = splitInputs[userId]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val computed = amountVal.multiply(pct).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP)
                Text(text = "$name: ₹$computed ($pct%)", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Show computed shares values beside input text boxes if SHARES is selected
        if (selectedSplitType == "SHARES" && amountVal > BigDecimal.ZERO) {
            val totalShares = selectedParticipantIds.sumOf { splitInputs[it]?.toBigDecimalOrNull() ?: BigDecimal.ZERO }
            if (totalShares > BigDecimal.ZERO) {
                Text(
                    text = "Computed Splits:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                selectedParticipantIds.forEach { userId ->
                    val name = members.find { it.userId == userId }?.name.orEmpty()
                    val sh = splitInputs[userId]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val computed = amountVal.multiply(sh).divide(totalShares, 2, java.math.RoundingMode.HALF_UP)
                    Text(text = "$name: ₹$computed ($sh shares)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Error message visual feedback
        errorText?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = TripSpacing.S)
            )
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

        Spacer(modifier = Modifier.height(TripSpacing.L))

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
                    val valuesMap = selectedParticipantIds.associateWith {
                        splitInputs[it]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    }
                    viewModel.createExpense(
                        workspaceId = workspaceId,
                        paidByUserId = selectedPayerUserId,
                        amount = amountVal,
                        currency = "INR",
                        description = description,
                        categoryId = categoryId,
                        expenseDate = java.time.LocalDate.now(),
                        participantIds = selectedParticipantIds.toList(),
                        splitType = selectedSplitType,
                        splitValues = if (selectedSplitType != "EQUAL") valuesMap else null,
                        expenseAt = java.time.Instant.now().toString(),
                        receiptUrl = receiptUrl,
                        note = note.ifBlank { null },
                        onSuccess = { onNavigateBack() }
                    )
                },
                enabled = description.isNotBlank() && amount.isNotBlank() && selectedPayerUserId.isNotBlank() && errorText == null && !isSaving,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
