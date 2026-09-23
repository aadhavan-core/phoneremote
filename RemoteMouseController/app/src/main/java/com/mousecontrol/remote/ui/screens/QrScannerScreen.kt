package com.mousecontrol.remote.ui.screens

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.json.JSONObject

/**
 * Expects a QR payload of the form:
 *   {"host":"192.168.1.42","port":5005}   -> Wi-Fi pairing
 *   {"mac":"AA:BB:CC:DD:EE:FF"}           -> Bluetooth pairing
 *
 * host-receiver/show_pairing_qr.py on the PC prints exactly this.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(
    onResult: (host: String?, port: Int?, macAddress: String?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var handled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    Box(Modifier.fillMaxSize()) {
        if (cameraPermission.status.isGranted) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        val scanner = BarcodeScanning.getClient()
                        analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !handled) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val raw = barcodes.firstOrNull()?.rawValue
                                        if (raw != null && !handled) {
                                            parsePayload(raw)?.let { (host, port, mac) ->
                                                handled = true
                                                onResult(host, port, mac)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
        } else {
            Column(Modifier.align(Alignment.Center).padding(24.dp)) {
                Text("Camera permission is needed to scan the pairing QR code.")
            }
        }

        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Text("Cancel")
        }
    }
}

private fun parsePayload(raw: String): Triple<String?, Int?, String?>? = try {
    val json = JSONObject(raw)
    val host = json.optString("host").takeIf { it.isNotBlank() }
    val port = if (json.has("port")) json.optInt("port") else null
    val mac = json.optString("mac").takeIf { it.isNotBlank() }
    if (host == null && mac == null) null else Triple(host, port, mac)
} catch (e: Exception) {
    null
}
