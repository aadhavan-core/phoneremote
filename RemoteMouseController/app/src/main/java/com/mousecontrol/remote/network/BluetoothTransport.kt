package com.mousecontrol.remote.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

/** Standard Serial Port Profile UUID — any Bluetooth stack understands this. */
private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

/**
 * Classic Bluetooth (RFCOMM/SPP) transport. This is the default, preferred
 * transport: once the phone is paired with the host in Android's Bluetooth
 * settings, this opens a serial-style socket to it directly — no shared
 * Wi-Fi network required.
 *
 * Requires BLUETOOTH_CONNECT (API 31+) or ACCESS_FINE_LOCATION (API <=30)
 * to already be granted before calling connect() — the UI layer checks
 * this first, which is why the lint suppression below is safe.
 */
@SuppressLint("MissingPermission")
class BluetoothTransport(
    private val adapter: BluetoothAdapter,
    scope: CoroutineScope
) : PacketSender(scope), MouseTransport {

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val status: StateFlow<ConnectionStatus> = _status

    private var socket: BluetoothSocket? = null
    private var out: OutputStream? = null

    override suspend fun connect(target: TransportTarget): Boolean {
        val bt = target as? TransportTarget.Bluetooth ?: return false
        _status.value = ConnectionStatus.CONNECTING
        return withContext(Dispatchers.IO) {
            try {
                val device: BluetoothDevice = adapter.getRemoteDevice(bt.deviceAddress)
                adapter.cancelDiscovery()
                val s = device.createRfcommSocketToServiceRecord(SPP_UUID)
                s.connect()
                socket = s
                out = s.outputStream
                transmit(PacketProtocol.hello())
                start()
                _status.value = ConnectionStatus.CONNECTED
                true
            } catch (e: Exception) {
                _status.value = ConnectionStatus.FAILED
                false
            }
        }
    }

    override suspend fun transmit(packet: String) {
        try {
            out?.write(packet.toByteArray())
            out?.flush()
        } catch (e: Exception) {
            _status.value = ConnectionStatus.FAILED
        }
    }

    override fun send(packet: String) = enqueue(packet)

    override fun disconnect() {
        stop()
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
        out = null
        _status.value = ConnectionStatus.DISCONNECTED
    }

    companion object {
        @SuppressLint("MissingPermission")
        fun pairedDevices(adapter: BluetoothAdapter): List<BluetoothDevice> =
            adapter.bondedDevices?.toList() ?: emptyList()
    }
}
