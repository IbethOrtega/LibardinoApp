package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GarmentEntity
import com.example.ui.ScanResolution
import com.example.ui.ScannerMode
import com.example.ui.StoreViewModel
import com.example.ui.components.BarcodeTagBadge
import com.example.ui.components.InfoPill
import com.example.ui.components.StockStatusPill
import com.example.ui.components.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val scannerMode by viewModel.scannerMode.collectAsStateWithLifecycle()
    val scanResolution by viewModel.scanResolution.collectAsStateWithLifecycle()
    val activeSale by viewModel.activeSale.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var showManualDialog by remember { mutableStateOf(false) }
    var newItemPrefillBarcode by remember { mutableStateOf<String?>(null) }
    var editingGarment by remember { mutableStateOf<GarmentEntity?>(null) }

    val isPaused = scanResolution != null ||
        showManualDialog ||
        newItemPrefillBarcode != null ||
        editingGarment != null ||
        activeSale.isOpen

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val isWide = maxWidth >= 680.dp

        if (isWide) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                ) {
                    ScannerHeaderAndMode(
                        scannerMode = scannerMode,
                        onSelectMode = viewModel::setScannerMode
                    )
                    BarcodeCameraView(
                        isScanningPaused = isPaused,
                        onBarcodeDetected = viewModel::resolveBarcode,
                        onManualEntryRequested = { showManualDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                ScannerSideSummaryCard(
                    scannerMode = scannerMode,
                    totalUnits = activeSale.totalUnits,
                    totalAmount = activeSale.totalAmount,
                    onOpenSaleMenu = viewModel::openNewSaleEditor,
                    onManualEntry = { showManualDialog = true },
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                ScannerHeaderAndMode(
                    scannerMode = scannerMode,
                    onSelectMode = viewModel::setScannerMode
                )
                BarcodeCameraView(
                    isScanningPaused = isPaused,
                    onBarcodeDetected = viewModel::resolveBarcode,
                    onManualEntryRequested = { showManualDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = if (activeSale.totalUnits > 0) {
                                        "Venta en curso (${activeSale.totalUnits} uds)"
                                    } else {
                                        "Apunta a la etiqueta"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (activeSale.totalUnits > 0) {
                                        "Total: ${formatCurrency(activeSale.totalAmount)}"
                                    } else if (scannerMode == ScannerMode.SELL) {
                                        "Cada código se suma a la venta"
                                    } else {
                                        "Registra o suma unidades al stock"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = viewModel::openNewSaleEditor,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("scanner_open_sale_submenu")
                        ) {
                            Text(if (activeSale.totalUnits > 0) "Ver Venta" else "Menú Venta")
                        }
                    }
                }
            }
        }
    }

    // Handle scanned barcode resolution
    when (val res = scanResolution) {
        is ScanResolution.MatchedGarment -> {
            MatchedGarmentSheet(
                garment = res.garment,
                scannerMode = scannerMode,
                isBusy = isBusy,
                onDismiss = viewModel::clearScanResolution,
                onAddToActiveSale = { qty, openEditor ->
                    viewModel.addInventoryGarmentToSale(res.garment, qty, openEditor = openEditor)
                    viewModel.clearScanResolution()
                },
                onRestock = { delta ->
                    viewModel.adjustStock(
                        garmentId = res.garment.id,
                        delta = delta,
                        onSuccess = { viewModel.clearScanResolution() }
                    )
                },
                onEditGarment = {
                    val target = res.garment
                    viewModel.clearScanResolution()
                    editingGarment = target
                }
            )
        }
        is ScanResolution.NewBarcode -> {
            AlertDialog(
                onDismissRequest = viewModel::clearScanResolution,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.QrCode2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text("Código no registrado") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Este código todavía no existe en Libardino:")
                        BarcodeTagBadge(barcode = res.barcode)
                        Text("¿Deseas registrarlo en el inventario ahora?")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val code = res.barcode
                            viewModel.clearScanResolution()
                            newItemPrefillBarcode = code
                        }
                    ) {
                        Text("Registrar prenda")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::clearScanResolution) {
                        Text("Cancelar")
                    }
                }
            )
        }
        null -> Unit
    }

    if (showManualDialog) {
        var manualInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showManualDialog = false },
            title = { Text("Ingresar código de barras") },
            text = {
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { if (it.length <= 64) manualInput = it },
                    label = { Text("Código o SKU") },
                    placeholder = { Text("Ej. 7501055312345") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_code_dialog_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = manualInput.trim()
                        showManualDialog = false
                        if (code.isNotEmpty()) {
                            viewModel.resolveBarcode(code)
                        }
                    },
                    enabled = manualInput.isNotBlank()
                ) {
                    Text("Buscar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (newItemPrefillBarcode != null || editingGarment != null) {
        GarmentFormSheet(
            existingGarment = editingGarment,
            prefilledBarcode = newItemPrefillBarcode ?: "",
            isBusy = isBusy,
            onDismiss = {
                newItemPrefillBarcode = null
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
                        newItemPrefillBarcode = null
                        editingGarment = null
                    }
                )
            },
            onDelete = if (editingGarment != null) {
                { garmentId ->
                    viewModel.deleteGarment(
                        garmentId = garmentId,
                        onSuccess = {
                            newItemPrefillBarcode = null
                            editingGarment = null
                        }
                    )
                }
            } else {
                null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScannerHeaderAndMode(
    scannerMode: ScannerMode,
    onSelectMode: (ScannerMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Escáner Libardino",
            style = MaterialTheme.typography.headlineSmall
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ScannerMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = scannerMode == mode,
                    onClick = { onSelectMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ScannerMode.entries.size
                    ),
                    icon = {
                        Icon(
                            imageVector = if (mode == ScannerMode.SELL) {
                                Icons.Rounded.PointOfSale
                            } else {
                                Icons.Rounded.Inventory
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.testTag("scanner_mode_${mode.name.lowercase()}")
                ) {
                    Text(mode.label)
                }
            }
        }
    }
}

@Composable
private fun ScannerSideSummaryCard(
    scannerMode: ScannerMode,
    totalUnits: Int,
    totalAmount: Double,
    onOpenSaleMenu: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (scannerMode == ScannerMode.SELL) Icons.Rounded.ShoppingBag else Icons.Rounded.Inventory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (scannerMode == ScannerMode.SELL) {
                    if (totalUnits > 0) "$totalUnits producto(s) • ${formatCurrency(totalAmount)}" else "Escanea para vender"
                } else {
                    "Escanea para inventario"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onOpenSaleMenu,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir Menú de Venta")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onManualEntry,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Código manual")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchedGarmentSheet(
    garment: GarmentEntity,
    scannerMode: ScannerMode,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onAddToActiveSale: (quantity: Int, openEditor: Boolean) -> Unit,
    onRestock: (Int) -> Unit,
    onEditGarment: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var quantity by remember(garment.id) { mutableIntStateOf(1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = garment.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    InfoPill(text = "Talla ${garment.size}")
                }
                IconButton(onClick = onEditGarment) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar prenda")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BarcodeTagBadge(barcode = garment.barcode)
                StockStatusPill(garment = garment)
            }

            Text(
                text = "Precio: ${formatCurrency(garment.price)}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider()

            if (scannerMode == ScannerMode.SELL) {
                if (garment.stock <= 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Esta prenda está agotada (Stock: 0). Suma unidades para venderla.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Button(
                        onClick = { onRestock(5) },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Sumar +5 unidades al stock")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cantidad", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { if (quantity > 1) quantity-- },
                                enabled = quantity > 1
                            ) {
                                Text("-")
                            }
                            Text(
                                text = "$quantity",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            OutlinedButton(
                                onClick = { if (quantity < garment.stock) quantity++ },
                                enabled = quantity < garment.stock
                            ) {
                                Text("+")
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { onAddToActiveSale(quantity, false) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Seguir escaneando")
                        }
                        Button(
                            onClick = { onAddToActiveSale(quantity, true) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(52.dp)
                        ) {
                            Text("Agregar e ir a Venta")
                        }
                    }
                }
            } else {
                Text("Sumar unidades al stock:", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(1, 5, 10, 20).forEach { delta ->
                        OutlinedButton(
                            onClick = { onRestock(delta) },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("+$delta")
                        }
                    }
                }
                Button(
                    onClick = onEditGarment,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Editar nombre, talla o precio")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
