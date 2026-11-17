package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode

@Composable
fun BarcodeCameraView(
    isScanningPaused: Boolean,
    onBarcodeDetected: (String) -> Unit,
    onManualEntryRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var lastDetectedCode by remember { mutableStateOf<String?>(null) }
    var lastDetectedTimeMs by remember { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF141312))
    ) {
        if (hasCameraPermission) {
            val barcodeScanner = remember {
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
                BarcodeScanning.getClient(options)
            }

            val cameraController = remember(context) {
                LifecycleCameraController(context).apply {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                }
            }

            LaunchedEffect(cameraController, isScanningPaused) {
                val mainExecutor = ContextCompat.getMainExecutor(context)
                cameraController.setImageAnalysisAnalyzer(
                    mainExecutor,
                    MlKitAnalyzer(
                        listOf(barcodeScanner),
                        CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                        mainExecutor
                    ) { result ->
                        if (isScanningPaused) return@MlKitAnalyzer
                        val barcodes = result?.getValue(barcodeScanner)
                        val rawValue = barcodes?.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.trim()
                        if (!rawValue.isNullOrEmpty()) {
                            val now = System.currentTimeMillis()
                            if (rawValue != lastDetectedCode || (now - lastDetectedTimeMs) > 2000L) {
                                lastDetectedCode = rawValue
                                lastDetectedTimeMs = now
                                onBarcodeDetected(rawValue)
                            }
                        }
                    }
                )
            }

            DisposableEffect(lifecycleOwner, cameraController) {
                try {
                    cameraController.bindToLifecycle(lifecycleOwner)
                } catch (e: Exception) {
                    Log.e("BarcodeCameraView", "Failed to bind CameraX controller", e)
                }
                onDispose {
                    cameraController.clearImageAnalysisAnalyzer()
                    cameraController.unbind()
                    barcodeScanner.close()
                }
            }

            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        controller = cameraController
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            ScannerReticleOverlay(
                isPaused = isScanningPaused,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom camera controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onManualEntryRequested,
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.testTag("manual_barcode_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Keyboard,
                            contentDescription = "Escribir código manualmente",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Código manual",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = {
                        val next = !torchEnabled
                        torchEnabled = next
                        cameraController.enableTorch(next)
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (torchEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Black.copy(alpha = 0.65f)
                        },
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("torch_toggle_button")
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        contentDescription = if (torchEnabled) "Apagar linterna" else "Encender linterna"
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Permiso de cámara requerido",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Activa la cámara para leer las etiquetas con código de barras de tus prendas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.testTag("request_camera_permission_button")
                ) {
                    Text("Permitir cámara")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onManualEntryRequested,
                    modifier = Modifier.testTag("manual_barcode_fallback_button")
                ) {
                    Text("O escribir código manualmente", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ScannerReticleOverlay(
    isPaused: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_line")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )

    Canvas(modifier = modifier) {
        val boxWidth = size.width * 0.76f
        val boxHeight = size.height * 0.44f
        val left = (size.width - boxWidth) / 2f
        val top = (size.height - boxHeight) / 2f - size.height * 0.04f

        // Subtle frame border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.78f),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 4f)
        )

        if (!isPaused) {
            val lineY = top + 16f + (boxHeight - 32f) * laserProgress
            drawLine(
                color = accentColor,
                start = Offset(left + 20f, lineY),
                end = Offset(left + boxWidth - 20f, lineY),
                strokeWidth = 5f
            )
        }
    }
}
