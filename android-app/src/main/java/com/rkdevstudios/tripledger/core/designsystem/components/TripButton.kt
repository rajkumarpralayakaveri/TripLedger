package com.rkdevstudios.tripledger.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rkdevstudios.tripledger.core.designsystem.theme.TripLedgerTheme

@Composable
fun TripButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

@Preview(name = "Light Mode")
@Composable
fun TripButtonPreviewLight() {
    TripLedgerTheme(darkTheme = false) {
        TripButton(text = "Add Expense", onClick = {})
    }
}

@Preview(name = "Dark Mode")
@Composable
fun TripButtonPreviewDark() {
    TripLedgerTheme(darkTheme = true) {
        TripButton(text = "Add Expense", onClick = {})
    }
}
