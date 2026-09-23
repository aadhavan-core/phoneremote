package com.mousecontrol.remote.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mousecontrol.remote.network.ConnectionStatus
import com.mousecontrol.remote.ui.screens.ConnectionScreen
import com.mousecontrol.remote.ui.screens.ControlScreen
import com.mousecontrol.remote.ui.screens.QrScannerScreen
import com.mousecontrol.remote.viewmodel.AppViewModel

private enum class Screen { CONNECT, QR_SCAN, CONTROL }

@Composable
fun RemoteMouseNavHost(viewModel: AppViewModel) {
    var screen by remember { mutableStateOf(Screen.CONNECT) }
    val status by viewModel.connectionStatus.collectAsState()

    LaunchedEffect(status) {
        if (status == ConnectionStatus.CONNECTED) screen = Screen.CONTROL
    }

    when (screen) {
        Screen.CONNECT -> ConnectionScreen(
            viewModel = viewModel,
            onScanQr = { screen = Screen.QR_SCAN }
        )

        Screen.QR_SCAN -> QrScannerScreen(
            onResult = { host, port, macAddress ->
                when {
                    macAddress != null -> viewModel.connectBluetooth(macAddress)
                    host != null && port != null -> viewModel.connectWifi(host, port)
                }
                screen = Screen.CONNECT
            },
            onCancel = { screen = Screen.CONNECT }
        )

        Screen.CONTROL -> ControlScreen(
            viewModel = viewModel,
            onDisconnected = { screen = Screen.CONNECT }
        )
    }
}
