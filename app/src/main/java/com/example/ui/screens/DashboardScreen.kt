package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.ChartPoint
import com.example.ui.StoreViewModel
import com.example.ui.components.formatCurrency

private enum class ChartRange(val label: String) {
    TODAY("Hoy por hora"),
    WEEK("Últimos 7 días")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: StoreViewModel,
    onStartNewSale: () -> Unit,
    onOpenScanner: () -> Unit,
    onViewLowStockAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    var chartRange by remember { mutableStateOf(ChartRange.TODAY) }

    val chartPoints = if (chartRange == ChartRange.TODAY) {
        stats.hourlyPointsToday
    } else {
        stats.last7DaysPoints
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Branded Libardino Black & Crimson Header
        item {
            Surface(
                color = Color(0xFF0A0A0A),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_libardino_logo),
                            contentDescription = "Logo Libardino",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LIBARDINO",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Resumen de ventas e inventario",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onStartNewSale,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dashboard_new_sale_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nueva Venta")
                        }
                        OutlinedButton(
                            onClick = onOpenScanner,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dashboard_scan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Escanear")
                        }
                    }
                }
            }
        }

        // Low stock alert banner
        if (stats.lowStockCount > 0) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onViewLowStockAlerts)
                        .testTag("dashboard_low_stock_alert")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = "Alerta de bajo stock",
                                    tint = Color.White
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "¡Alerta de inventario bajo!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Hay ${stats.lowStockCount} prenda(s) con pocas unidades o agotadas. Toca para revisar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Today KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DashboardKpiCard(
                        title = "Vendido hoy",
                        value = formatCurrency(stats.todayRevenue),
                        subtitle = "${stats.todayCompletedCount} venta(s) completadas",
                        icon = Icons.Rounded.PointOfSale,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1.15f)
                    )
                    DashboardKpiCard(
                        title = "Productos hoy",
                        value = "${stats.todayUnits}",
                        subtitle = if (stats.todayCancelledCount > 0) {
                            "${stats.todayCancelledCount} cancelada(s)"
                        } else {
                            "prendas vendidas"
                        },
                        icon = Icons.Rounded.TrendingUp,
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(0.95f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DashboardKpiCard(
                        title = "Stock total",
                        value = "${stats.totalInventoryUnits}",
                        subtitle = "${stats.totalSkus} prendas registradas",
                        icon = Icons.Rounded.Checkroom,
                        accent = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardKpiCard(
                        title = "Alertas stock",
                        value = "${stats.lowStockCount}",
                        subtitle = if (stats.lowStockCount == 0) "Inventario sano" else "Requieren surtido",
                        icon = Icons.Rounded.WarningAmber,
                        accent = if (stats.lowStockCount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onViewLowStockAlerts)
                    )
                }
            }
        }

        // Sales Bar Chart Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Gráfica de Ventas",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ChartRange.entries.forEachIndexed { idx, range ->
                            SegmentedButton(
                                selected = chartRange == range,
                                onClick = { chartRange = range },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = idx,
                                    count = ChartRange.entries.size
                                )
                            ) {
                                Text(range.label)
                            }
                        }
                    }

                    SalesBarChart(
                        points = chartPoints,
                        barColor = MaterialTheme.colorScheme.primary,
                        gridColor = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }

        // Top Products Today
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Prendas más vendidas hoy",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    if (stats.topProductsToday.isEmpty()) {
                        Text(
                            text = "Aún no hay ventas completadas hoy. Cuando completes una venta aparecerán aquí las prendas más vendidas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val maxUnits = stats.topProductsToday.maxOf { it.units }.coerceAtLeast(1)
                        stats.topProductsToday.forEach { product ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${product.productName} (${product.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${product.units} uds • ${formatCurrency(product.revenue)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { product.units.toFloat() / maxUnits.toFloat() },
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun DashboardKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SalesBarChart(
    points: List<ChartPoint>,
    barColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val maxRevenue = points.maxOf { it.revenue }.coerceAtLeast(1.0)
    val hasSales = points.any { it.revenue > 0.0 }

    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val topPad = 12f
                val bottomPad = 8f
                val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                // Horizontal reference grid lines
                for (i in 0..2) {
                    val y = topPad + (chartHeight / 2f) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f
                    )
                }

                val slotWidth = size.width / points.size.toFloat()
                val barWidth = (slotWidth * 0.52f).coerceAtLeast(12f)

                points.forEachIndexed { index, point ->
                    val ratio = if (hasSales) (point.revenue / maxRevenue).toFloat() else 0f
                    val barHeight = (chartHeight * ratio).coerceAtLeast(if (point.revenue > 0) 10f else 4f)
                    val x = slotWidth * index + (slotWidth - barWidth) / 2f
                    val y = topPad + (chartHeight - barHeight)

                    drawRoundRect(
                        color = if (point.revenue > 0.0) barColor else gridColor.copy(alpha = 0.5f),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }
            }

            if (!hasSales) {
                Text(
                    text = "Sin ventas en este período",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            points.forEach { point ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (point.units > 0) {
                        Text(
                            text = "${point.units}u",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = barColor
                        )
                    }
                }
            }
        }
    }
}
