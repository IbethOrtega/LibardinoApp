package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DraftSaleItem
import com.example.data.GarmentEntity
import com.example.data.PaymentMethod
import com.example.data.SaleStatus
import com.example.ui.ActiveSaleEditorState
import com.example.ui.components.InfoPill
import com.example.ui.components.formatCurrency

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SaleEditorSheet(
    saleState: ActiveSaleEditorState,
    inventoryGarments: List<GarmentEntity>,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onAddInventoryGarment: (GarmentEntity) -> Unit,
    onAddCustomPriceItem: (name: String, size: String, price: Double, qty: Int) -> Unit,
    onUpdateItemQuantity: (key: String, newQty: Int) -> Unit,
    onRemoveItem: (key: String) -> Unit,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    onCompleteSale: () -> Unit,
    onCancelSale: () -> Unit,
    onDiscardDraft: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showInventoryPicker by remember { mutableStateOf(false) }
    var showCustomPriceDialog by remember { mutableStateOf(false) }
    var showBarcodeScannerDialog by remember { mutableStateOf(false) }
    var scannerMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (saleState.isEditing) {
                            "Editando Venta ${saleState.editingOrderNumber ?: ""}"
                        } else {
                            "Nueva Venta"
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Agrega prendas o productos por precio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar menú de venta")
                }
            }

            // 3 Quick Add Product Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        scannerMessage = null
                        showBarcodeScannerDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sale_add_by_scan_button")
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Escanear", maxLines = 1)
                }

                OutlinedButton(
                    onClick = { showInventoryPicker = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sale_add_from_inventory_button")
                ) {
                    Icon(Icons.Rounded.Checkroom, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inventario", maxLines = 1)
                }

                OutlinedButton(
                    onClick = { showCustomPriceDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sale_add_by_price_button")
                ) {
                    Icon(Icons.Rounded.AttachMoney, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Por precio", maxLines = 1)
                }
            }

            if (scannerMessage != null) {
                Text(
                    text = scannerMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider()

            // Line items list
            if (saleState.items.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No hay productos en esta venta. Usa Escanear, Inventario o Por precio para agregar lo que lleva el cliente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(saleState.items, key = { it.key }) { item ->
                        SaleDraftLineRow(
                            item = item,
                            onIncrement = { onUpdateItemQuantity(item.key, item.quantity + 1) },
                            onDecrement = { onUpdateItemQuantity(item.key, item.quantity - 1) },
                            onRemove = { onRemoveItem(item.key) }
                        )
                    }
                }
            }

            // Payment method
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = saleState.paymentMethod == method,
                        onClick = { onSelectPaymentMethod(method) },
                        label = { Text(method.label) }
                    )
                }
            }

            // Live Total Summary Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total a pagar",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${saleState.totalUnits} producto(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatCurrency(saleState.totalAmount),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Complete / Cancel buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        if (!saleState.isEditing && saleState.items.isEmpty()) {
                            onDiscardDraft()
                        } else {
                            onCancelSale()
                        }
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(52.dp)
                        .testTag("cancel_sale_button")
                ) {
                    Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancelar")
                }

                Button(
                    onClick = onCompleteSale,
                    enabled = !isBusy && saleState.items.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(52.dp)
                        .testTag("complete_sale_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (saleState.isEditing && saleState.originalStatus == SaleStatus.COMPLETADA) {
                                "Guardar cambios"
                            } else {
                                "Completar Venta"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    if (showInventoryPicker) {
        InventoryGarmentPickerDialog(
            garments = inventoryGarments,
            onDismiss = { showInventoryPicker = false },
            onSelect = { garment ->
                onAddInventoryGarment(garment)
                showInventoryPicker = false
            }
        )
    }

    if (showCustomPriceDialog) {
        CustomPriceProductDialog(
            onDismiss = { showCustomPriceDialog = false },
            onAdd = { name, size, price, qty ->
                onAddCustomPriceItem(name, size, price, qty)
                showCustomPriceDialog = false
            }
        )
    }

    if (showBarcodeScannerDialog) {
        Dialog(
            onDismissRequest = { showBarcodeScannerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Escanear producto",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { showBarcodeScannerDialog = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar escáner")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    BarcodeCameraView(
                        isScanningPaused = false,
                        onBarcodeDetected = { code ->
                            val match = inventoryGarments.firstOrNull {
                                it.barcode.equals(code, ignoreCase = true)
                            }
                            if (match != null) {
                                onAddInventoryGarment(match)
                                scannerMessage = null
                                showBarcodeScannerDialog = false
                            } else {
                                scannerMessage = "Código '$code' no encontrado en inventario."
                                showBarcodeScannerDialog = false
                            }
                        },
                        onManualEntryRequested = { showBarcodeScannerDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SaleDraftLineRow(
    item: DraftSaleItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Talla ${item.size} • ${formatCurrency(item.unitPrice)} c/u",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Subtotal: ${formatCurrency(item.subtotal)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Disminuir cantidad")
                }
                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(
                    onClick = onIncrement,
                    enabled = item.maxInventoryStock == null || item.quantity < item.maxInventoryStock,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Aumentar cantidad")
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Quitar producto",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryGarmentPickerDialog(
    garments: List<GarmentEntity>,
    onDismiss: () -> Unit,
    onSelect: (GarmentEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(garments, query) {
        val q = query.trim().lowercase()
        garments.filter { g ->
            q.isEmpty() ||
                g.name.lowercase().contains(q) ||
                g.size.lowercase().contains(q) ||
                g.barcode.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar del inventario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar prenda o talla...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (filtered.isEmpty()) {
                    Text(
                        text = "No hay prendas registradas que coincidan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered, key = { it.id }) { g ->
                            Surface(
                                onClick = { onSelect(g) },
                                enabled = g.stock > 0,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = g.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            InfoPill(text = "Talla ${g.size}")
                                            InfoPill(
                                                text = if (g.stock > 0) "Disp: ${g.stock}" else "Agotado",
                                                containerColor = if (g.stock > 0) {
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.errorContainer
                                                }
                                            )
                                        }
                                    }
                                    Text(
                                        text = formatCurrency(g.price),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomPriceProductDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, size: String, price: Double, qty: Int) -> Unit
) {
    var productName by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("M") }
    var priceText by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar por precio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = productName,
                    onValueChange = { if (it.length <= 80) productName = it },
                    label = { Text("Nombre del producto *") },
                    placeholder = { Text("Ej. Camiseta básica") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_item_name_input")
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("S", "M", "L", "XL", "Única").forEach { quick ->
                        FilterChip(
                            selected = size.equals(quick, ignoreCase = true),
                            onClick = { size = quick },
                            label = { Text(quick) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = size,
                        onValueChange = { if (it.length <= 16) size = it },
                        label = { Text("Talla") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Precio *") },
                        prefix = { Text("$") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("custom_item_price_input")
                    )
                }

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it.filter { c -> c.isDigit() } },
                    label = { Text("Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.replace(",", ".").toDoubleOrNull()
                    val qty = qtyText.toIntOrNull()
                    when {
                        productName.isBlank() -> error = "Ingresa el nombre del producto."
                        price == null || price < 0.0 -> error = "Ingresa un precio válido."
                        qty == null || qty <= 0 -> error = "Ingresa una cantidad válida."
                        else -> onAdd(productName.trim(), size.trim().ifEmpty { "Única" }, price, qty)
                    }
                },
                modifier = Modifier.testTag("confirm_custom_price_item_button")
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
