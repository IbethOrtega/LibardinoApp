package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GarmentEntity

private val QUICK_SIZES = listOf("XS", "S", "M", "L", "XL", "XXL", "28", "30", "32", "34", "Única")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GarmentFormSheet(
    existingGarment: GarmentEntity?,
    prefilledBarcode: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        garmentId: Long?,
        barcode: String,
        name: String,
        size: String,
        price: Double,
        stock: Int,
        lowStockThreshold: Int
    ) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var barcode by remember(existingGarment, prefilledBarcode) {
        mutableStateOf(existingGarment?.barcode ?: prefilledBarcode)
    }
    var name by remember(existingGarment) { mutableStateOf(existingGarment?.name ?: "") }
    var size by remember(existingGarment) { mutableStateOf(existingGarment?.size ?: "M") }
    var priceText by remember(existingGarment) {
        mutableStateOf(
            if (existingGarment != null && existingGarment.price > 0.0) {
                if (existingGarment.price % 1.0 == 0.0) {
                    existingGarment.price.toLong().toString()
                } else {
                    existingGarment.price.toString()
                }
            } else {
                ""
            }
        )
    }
    var stockText by remember(existingGarment) {
        mutableStateOf(existingGarment?.stock?.toString() ?: "1")
    }
    var lowThresholdText by remember(existingGarment) {
        mutableStateOf(existingGarment?.lowStockThreshold?.toString() ?: "3")
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var showEmbeddedScanner by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingGarment == null) "Registrar Prenda" else "Editar Prenda",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar formulario")
                }
            }

            // Barcode row with live camera scanner button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { if (it.length <= 64) barcode = it },
                    label = { Text("Código de barras (opcional)") },
                    placeholder = { Text("Escanea o escribe el código") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("garment_barcode_input")
                )
                FilledTonalIconButton(
                    onClick = { showEmbeddedScanner = true },
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("garment_scan_barcode_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = "Escanear código con cámara"
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 80) name = it },
                label = { Text("Nombre de la prenda *") },
                placeholder = { Text("Ej. Camisa Polo, Jean Clásico...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("garment_name_input")
            )

            Text(
                text = "Talla *",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QUICK_SIZES.forEach { quick ->
                    FilterChip(
                        selected = size.equals(quick, ignoreCase = true),
                        onClick = { size = quick },
                        label = { Text(quick) }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = size,
                    onValueChange = { if (it.length <= 16) size = it },
                    label = { Text("Talla *") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("garment_size_input")
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Precio *") },
                    prefix = { Text("$") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("garment_price_input")
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Stock actual *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("garment_stock_input")
                )
                OutlinedTextField(
                    value = lowThresholdText,
                    onValueChange = { lowThresholdText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Alerta bajo stock") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("garment_threshold_input")
                )
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val cleanBarcode = barcode.trim()
                    val cleanName = name.trim()
                    val cleanSize = size.trim()
                    val price = priceText.replace(",", ".").toDoubleOrNull()
                    val stock = stockText.toIntOrNull()
                    val threshold = lowThresholdText.toIntOrNull() ?: 3

                    when {
                        cleanName.isEmpty() -> validationError = "Ingresa el nombre de la prenda."
                        cleanSize.isEmpty() -> validationError = "Ingresa la talla."
                        price == null || price < 0.0 -> validationError = "Ingresa un precio válido."
                        stock == null || stock < 0 -> validationError = "Ingresa una cantidad de stock válida."
                        else -> {
                            validationError = null
                            onSave(
                                existingGarment?.id,
                                cleanBarcode,
                                cleanName,
                                cleanSize,
                                price,
                                stock,
                                threshold.coerceAtLeast(0)
                            )
                        }
                    }
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_garment_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (existingGarment == null) "Guardar Prenda" else "Guardar Cambios",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (existingGarment != null && onDelete != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    enabled = !isBusy,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("delete_garment_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Eliminar Prenda")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showEmbeddedScanner) {
        Dialog(
            onDismissRequest = { showEmbeddedScanner = false },
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
                            text = "Escanear etiqueta",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = { showEmbeddedScanner = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Cerrar escáner")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    BarcodeCameraView(
                        isScanningPaused = false,
                        onBarcodeDetected = { scanned ->
                            barcode = scanned
                            showEmbeddedScanner = false
                        },
                        onManualEntryRequested = { showEmbeddedScanner = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm && existingGarment != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar prenda?") },
            text = { Text("Se eliminará '${existingGarment.name}' (Talla ${existingGarment.size}) del inventario.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(existingGarment.id)
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
