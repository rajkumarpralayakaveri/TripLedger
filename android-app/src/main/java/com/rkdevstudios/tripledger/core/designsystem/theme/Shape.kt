package com.rkdevstudios.tripledger.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object TripRadius {
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
}

val AppShapes = Shapes(
    small = RoundedCornerShape(TripRadius.Small),
    medium = RoundedCornerShape(TripRadius.Medium),
    large = RoundedCornerShape(TripRadius.Large),
    extraLarge = RoundedCornerShape(TripRadius.Large)
)
