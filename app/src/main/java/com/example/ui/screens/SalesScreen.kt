package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SaleOrderWithItems
import com.example.data.SaleStatus
import com.example.ui.StoreViewModel
import com.example.ui.components.InfoPill
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatMillis

private enum class SaleHistoryFilter(val label: String) {
    ALL("Todas"),
    COMPLETED("Completadas"),
    CANCELLED("Canceladas")
}

@Composable
fun SalesScreen(
    viewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.salesOrders.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf(SaleHistoryFilter.ALL) }

    val filteredOrders = remember(orders, filter) {
        when (filter) {
            SaleHistoryFilter.ALL -> orders
            SaleHistoryFilter.COMPLETED -> orders.filter { it.order.statusEnum == SaleStatus.COMPLETADA }
            SaleHistoryFilter.CANCELLED -> orders.filter { it.order.statusEnum == SaleStatus.CANCELADA }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openNewSaleEditor,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("sales_new_sale_fab")
            ) {
                Icon(Icons.Rounded.AddShoppingCart, contentDescription = "Nueva venta")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Historial de Ventas",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Crea, completa, cancela o edita tus ventas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = viewModel::openNewSaleEditor,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sales_open_submenu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vender")
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SaleHistoryFilter.entries.forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            if (filteredOrders.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Aún no hay ventas registradas",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Toca 'Vender' para abrir el menú de ventas, agregar las prendas que lleva el cliente y ver el total.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = viewModel::openNewSaleEditor,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Abrir Submenú de Ventas")
                            }
                        }
                    }
                }
            } else {
                items(filteredOrders, key = { it.order.id }) { orderWithItems ->
                    SaleOrderCard(
                        orderWithItems = orderWithItems,
                        onEditOrder = { viewModel.openEditSaleEditor(orderWithItems) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun SaleOrderCard(
    orderWithItems: SaleOrderWithItems,
    onEditOrder: () -> Unit
) {
    val order = orderWithItems.order
    val isCompleted = order.statusEnum == SaleStatus.COMPLETADA

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sale_order_card_${order.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Venta ${order.formattedNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    InfoPill(
                        text = order.statusEnum.label,
                        containerColor = if (isCompleted) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        contentColor = if (isCompleted) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    InfoPill(text = order.paymentMethodEnum.label)
                }

                Text(
                    text = formatCurrency(order.totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                orderWithItems.items.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${line.quantity}x ${line.productName} (${line.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatCurrency(line.subtotal),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.totalUnits} producto(s) • ${formatMillis(order.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = onEditOrder,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("edit_sale_${order.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar")
                }
            }
        }
    }
}
