package com.mousecontrol.remote.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mousecontrol.remote.network.ConnectionStatus
import com.mousecontrol.remote.network.MouseButton
import com.mousecontrol.remote.network.MouseTransport
import com.mousecontrol.remote.network.PacketProtocol
import com.mousecontrol.remote.network.TransportTarget
import com.mousecontrol.remote.network.BluetoothTransport
import com.mousecontrol.remote.network.WifiTransport
import com.mousecontrol.remote.sensors.GyroSensorManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class ControlMode { GYROSCOPE, TRACKPAD }

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothAdapter: BluetoothAdapter? =
        application.getSystemService(BluetoothManager::class.java)?.adapter

    private var activeTransport: MouseTransport? = null
    private val gyro = GyroSensorManager(application)
    private var gyroJob: Job? = null
    private var statusCollectJob: Job? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _mode = MutableStateFlow(ControlMode.TRACKPAD)
    val mode: StateFlow<ControlMode> = _mode

    private val _volume = MutableStateFlow(60)
    val volume: StateFlow<Int> = _volume

    private val _brightness = MutableStateFlow(60)
    val brightness: StateFlow<Int> = _brightness

    val gyroAvailable get() = gyro.isAvailable
    fun bluetoothAdapterOrNull(): BluetoothAdapter? = bluetoothAdapter

    fun connectBluetooth(address: String) {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _connectionStatus.value = ConnectionStatus.FAILED
            return
        }
        connect(TransportTarget.Bluetooth(address)) { BluetoothTransport(adapter, viewModelScope) }
    }

    fun connectWifi(host: String, port: Int) {
        connect(TransportTarget.Wifi(host, port)) { WifiTransport(viewModelScope) }
    }

    private fun connect(target: TransportTarget, factory: () -> MouseTransport) {
        viewModelScope.launch {
            activeTransport?.disconnect()
            statusCollectJob?.cancel()

            val transport = factory()
            activeTransport = transport
            statusCollectJob = viewModelScope.launch {
                transport.status.collect { _connectionStatus.value = it }
            }
            transport.connect(target)
        }
    }

    fun setMode(newMode: ControlMode) {
        _mode.value = newMode
        if (newMode == ControlMode.GYROSCOPE) startGyro() else stopGyro()
    }

    private fun startGyro() {
        gyroJob?.cancel()
        gyroJob = viewModelScope.launch {
            gyro.deltaFlow().collect { delta ->
                activeTransport?.send(PacketProtocol.move(delta.dx, delta.dy))
            }
        }
    }

    private fun stopGyro() {
        gyroJob?.cancel()
        gyroJob = null
    }

    fun sendTrackpadDelta(dx: Float, dy: Float) {
        activeTransport?.send(PacketProtocol.move(dx, dy))
    }

    fun sendScroll(dy: Float) {
        activeTransport?.send(PacketProtocol.scroll(dy))
    }

    fun click(button: MouseButton) {
        activeTransport?.send(PacketProtocol.click(button))
    }

    fun doubleClick(button: MouseButton) {
        activeTransport?.send(PacketProtocol.doubleClick(button))
    }

    fun buttonDown(button: MouseButton) {
        activeTransport?.send(PacketProtocol.buttonDown(button))
    }

    fun buttonUp(button: MouseButton) {
        activeTransport?.send(PacketProtocol.buttonUp(button))
    }

    fun setVolume(level: Int) {
        _volume.value = level
        activeTransport?.send(PacketProtocol.volume(level))
    }

    fun setBrightness(level: Int) {
        _brightness.value = level
        activeTransport?.send(PacketProtocol.brightness(level))
    }

    fun disconnect() {
        stopGyro()
        activeTransport?.disconnect()
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }
}
