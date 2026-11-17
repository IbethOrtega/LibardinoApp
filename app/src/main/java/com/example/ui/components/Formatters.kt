package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.GarmentEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val currencyFormat: NumberFormat by lazy {
    NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }
}

private val dateFormat: SimpleDateFormat by lazy {
    SimpleDateFormat("dd MMM, HH:mm", Locale("es", "CO"))
}

fun formatCurrency(amount: Double): String {
    return currencyFormat.format(amount)
}

fun formatMillis(millis: Long): String {
    return dateFormat.format(Date(millis))
}

@Composable
fun StockStatusPill(
    garment: GarmentEntity,
    modifier: Modifier = Modifier
) {
    val (bg, fg, label) = when {
        garment.isOutOfStock -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Agotado (0)"
        )
        garment.isLowStock -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Stock bajo: ${garment.stock}"
        )
        else -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Stock: ${garment.stock}"
        )
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun BarcodeTagBadge(
    barcode: String,
    modifier: Modifier = Modifier
) {
    if (barcode.isBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.QrCode2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = barcode,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InfoPill(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
        color = contentColor,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
