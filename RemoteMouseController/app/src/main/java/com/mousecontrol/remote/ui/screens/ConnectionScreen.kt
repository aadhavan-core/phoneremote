package com.mousecontrol.remote.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mousecontrol.remote.network.BluetoothTransport
import com.mousecontrol.remote.network.ConnectionStatus
import com.mousecontrol.remote.viewmodel.AppViewModel

private enum class PairingTab { BLUETOOTH, WIFI }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ConnectionScreen(viewModel: AppViewModel, onScanQr: () -> Unit) {
    // Bluetooth is the default tab per spec — it needs no shared network.
    var tab by remember { mutableStateOf(PairingTab.BLUETOOTH) }
    val status by viewModel.connectionStatus.collectAsState()

    val bluetoothPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    val btPermissionState = rememberMultiplePermissionsState(bluetoothPermissions)

    Row(Modifier.fillMaxSize().padding(24.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Pair with your PC", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            TabRow(selectedTabIndex = tab.ordinal) {
                Tab(
                    selected = tab == PairingTab.BLUETOOTH,
                    onClick = { tab = PairingTab.BLUETOOTH },
                    text = { Text("Bluetooth") }
                )
                Tab(
                    selected = tab == PairingTab.WIFI,
                    onClick = { tab = PairingTab.WIFI },
                    text = { Text("Wi-Fi") }
                )
            }
            Spacer(Modifier.height(16.dp))

            when (tab) {
                PairingTab.BLUETOOTH -> BluetoothPairingPanel(
                    viewModel = viewModel,
                    hasPermission = btPermissionState.allPermissionsGranted,
                    onRequestPermission = { btPermissionState.launchMultiplePermissionRequest() }
                )
                PairingTab.WIFI -> WifiPairingPanel(viewModel)
            }

            Spacer(Modifier.height(12.dp))
            when (status) {
                ConnectionStatus.CONNECTING -> Text("Connecting…", color = MaterialTheme.colorScheme.secondary)
                ConnectionStatus.FAILED -> Text(
                    "Connection failed — make sure the host receiver is running.",
                    color = MaterialTheme.colorScheme.error
                )
                else -> {}
            }
        }

        Spacer(Modifier.width(24.dp))

        OutlinedButton(onClick = onScanQr, modifier = Modifier.align(Alignment.CenterVertically)) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Scan QR code")
        }
    }
}

@Composable
private fun BluetoothPairingPanel(
    viewModel: AppViewModel,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    if (!hasPermission) {
        Button(onClick = onRequestPermission) { Text("Grant Bluetooth permission") }
        return
    }
    val adapter = viewModel.bluetoothAdapterOrNull()
    if (adapter == null) {
        Text("Bluetooth is not available on this device — use the Wi-Fi tab instead.")
        return
    }
    if (!adapter.isEnabled) {
        Text("Turn on Bluetooth, then reopen this screen.")
        return
    }
    val devices = remember { BluetoothTransport.pairedDevices(adapter) }
    if (devices.isEmpty()) {
        Text("No paired devices yet. Pair with your PC in Android's Bluetooth settings first, then come back here.")
        return
    }
    LazyColumn(Modifier.heightIn(max = 240.dp)) {
        items(devices) { device ->
            ListItem(
                headlineContent = { Text(device.name ?: device.address) },
                supportingContent = { Text(device.address) },
                modifier = Modifier.clickable { viewModel.connectBluetooth(device.address) }
            )
        }
    }
}

@Composable
private fun WifiPairingPanel(viewModel: AppViewModel) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5005") }
    Column {
        OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host IP address") }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, singleLine = true)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { port.toIntOrNull()?.let { viewModel.connectWifi(host, it) } },
            enabled = host.isNotBlank()
        ) { Text("Connect") }
    }
}
