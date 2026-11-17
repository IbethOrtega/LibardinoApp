package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GarmentEntity
import com.example.ui.StoreViewModel
import com.example.ui.components.BarcodeTagBadge
import com.example.ui.components.InfoPill
import com.example.ui.components.StockStatusPill
import com.example.ui.components.formatCurrency

@Composable
fun InventoryScreen(
    viewModel: StoreViewModel,
    onOpenScannerForInventory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allGarments by viewModel.garments.collectAsStateWithLifecycle()
    val filteredGarments by viewModel.filteredGarments.collectAsStateWithLifecycle()
    val lowStockAlerts by viewModel.lowStockAlerts.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val lowStockOnly by viewModel.lowStockOnly.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var editingGarment by remember { mutableStateOf<GarmentEntity?>(null) }
    var showCreateForm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingGarment = null
                    showCreateForm = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_garment_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Registrar nueva prenda")
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
                            text = "Inventario",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "${allGarments.size} referencias • ${allGarments.sumOf { it.stock }} unidades",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onOpenScannerForInventory,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("inventory_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Escanear prenda"
                        )
                    }
                }
            }

            // Prominent Low-Stock Alert Card
            if (lowStockAlerts.isNotEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { viewModel.setLowStockOnly(!lowStockOnly) }
                            .testTag("inventory_low_stock_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Alerta: ${lowStockAlerts.size} prenda(s) con stock bajo",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                val namesPreview = lowStockAlerts.take(3).joinToString(", ") {
                                    "${it.name} (${it.stock})"
                                }
                                Text(
                                    text = namesPreview,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    placeholder = { Text("Buscar por nombre, talla o código...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inventory_search_input")
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !lowStockOnly,
                        onClick = { viewModel.setLowStockOnly(false) },
                        label = { Text("Todas (${allGarments.size})") }
                    )
                    FilterChip(
                        selected = lowStockOnly,
                        onClick = { viewModel.setLowStockOnly(true) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text("Stock bajo (${lowStockAlerts.size})") }
                    )
                }
            }

            if (allGarments.isEmpty()) {
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
                                imageVector = Icons.Rounded.Checkroom,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Registra tu primera prenda",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Escanea la etiqueta con código de barras o agrégala manualmente con nombre, talla y precio.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = onOpenScannerForInventory,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.QrCodeScanner,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Escanear")
                                }
                                OutlinedButton(
                                    onClick = { showCreateForm = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Agregar")
                                }
                            }
                        }
                    }
                }
            } else if (filteredGarments.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Sin resultados",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "No hay prendas que coincidan con tu filtro.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredGarments, key = { it.id }) { garment ->
                    GarmentInventoryCard(
                        garment = garment,
                        isBusy = isBusy,
                        onCardClick = { editingGarment = garment },
                        onAdjustStock = { delta ->
                            viewModel.adjustStock(garment.id, delta)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showCreateForm || editingGarment != null) {
        GarmentFormSheet(
            existingGarment = editingGarment,
            prefilledBarcode = "",
            isBusy = isBusy,
            onDismiss = {
                showCreateForm = false
                editingGarment = null
            },
            onSave = { garmentId, barcode, name, size, price, stock, threshold ->
                viewModel.saveGarment(
                    garmentId = garmentId,
                    barcode = barcode,
                    name = name,
                    size = size,
                    price = price,
                    stock = stock,
                    lowStockThreshold = threshold,
                    onSuccess = {
                        showCreateForm = false
                        editingGarment = null
                    }
                )
            },
            onDelete = { garmentId ->
                viewModel.deleteGarment(
                    garmentId = garmentId,
                    onSuccess = {
                        showCreateForm = false
                        editingGarment = null
                    }
                )
            }
        )
    }
}

@Composable
private fun GarmentInventoryCard(
    garment: GarmentEntity,
    isBusy: Boolean,
    onCardClick: () -> Unit,
    onAdjustStock: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .testTag("garment_card_${garment.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = garment.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoPill(text = "Talla ${garment.size}")
                        if (garment.barcode.isNotBlank()) {
                            BarcodeTagBadge(barcode = garment.barcode)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatCurrency(garment.price),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StockStatusPill(garment = garment)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { onAdjustStock(-1) },
                        enabled = !isBusy && garment.stock > 0,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("decrement_stock_${garment.id}")
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Restar una unidad")
                    }

                    Text(
                        text = "${garment.stock}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    FilledTonalIconButton(
                        onClick = { onAdjustStock(1) },
                        enabled = !isBusy,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("increment_stock_${garment.id}")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Sumar una unidad")
                    }
                }
            }
        }
    }
}
