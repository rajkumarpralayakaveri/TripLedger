package com.rkdevstudios.tripledger.core.designsystem.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rkdevstudios.tripledger.core.designsystem.theme.TripElevation
import com.rkdevstudios.tripledger.core.designsystem.theme.TripLedgerTheme
import com.rkdevstudios.tripledger.core.designsystem.theme.TripSpacing

@Composable
fun TripCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = TripElevation.Small)
    ) {
        Column(
            modifier = Modifier.padding(TripSpacing.M),
            content = content
        )

    }
}

@Preview(name = "Light Mode")
@Composable
fun TripCardPreview() {
    TripLedgerTheme(darkTheme = false) {
        TripCard {
        }
    }
}
