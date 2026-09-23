package com.mousecontrol.remote.network

import kotlinx.coroutines.flow.StateFlow

enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

/** Common surface both the Bluetooth and Wi-Fi transports implement. */
interface MouseTransport {
    val status: StateFlow<ConnectionStatus>
    suspend fun connect(target: TransportTarget): Boolean
    fun send(packet: String)
    fun disconnect()
}

sealed class TransportTarget {
    data class Wifi(val host: String, val port: Int) : TransportTarget()
    data class Bluetooth(val deviceAddress: String) : TransportTarget()
}
