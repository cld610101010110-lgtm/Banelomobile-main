package com.project.dba_delatorre_dometita_ramirez_tan

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================================
// Colors - Dashboard Theme (shared across report screens)
// ============================================================================
val LatteCream = Color(0xFFF3E5AB)
val LightCoffee = Color(0xFFFAF1E6)
val Mocha = Color(0xFF837060)
val Cappuccino = Color(0xFFDDBEA9)
val CoffeeBrown = Color(0xFF6F4E37)
val EspressoDark = Color(0xFF4B3621)
val BackgroundCoffee = Color(0xFFFFF8F0)
val Latte = Color(0xFFF5E6DA)
val WasteRed = Color(0xFFD32F2F)
val WarningOrange = Color(0xFFF57C00)

// ============================================================================
// Filter Button Component (shared across report screens)
// ============================================================================
@Composable
fun ReportFilterButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) CoffeeBrown else LightCoffee,
            contentColor = if (isSelected) Color.White else EspressoDark
        )
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
