package com.mousecontrol.remote.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

/**
 * UDP transport: lowest latency for the continuous stream of MOVE packets,
 * used as the Wi-Fi fallback when Bluetooth isn't available or preferred.
 * An occasional dropped UDP packet only skips one frame of cursor motion,
 * which is an acceptable trade for not blocking on retransmission/ack like
 * TCP would.
 */
class WifiTransport(scope: CoroutineScope) : PacketSender(scope), MouseTransport {

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override val status: StateFlow<ConnectionStatus> = _status

    private var socket: DatagramSocket? = null
    private var address: InetSocketAddress? = null

    override suspend fun connect(target: TransportTarget): Boolean {
        val wifi = target as? TransportTarget.Wifi ?: return false
        _status.value = ConnectionStatus.CONNECTING
        return withContext(Dispatchers.IO) {
            try {
                val s = DatagramSocket()
                socket = s
                address = InetSocketAddress(wifi.host, wifi.port)
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
        val addr = address ?: return
        try {
            val bytes = packet.toByteArray()
            socket?.send(DatagramPacket(bytes, bytes.size, addr))
        } catch (e: Exception) {
            _status.value = ConnectionStatus.FAILED
        }
    }

    override fun send(packet: String) = enqueue(packet)

    override fun disconnect() {
        stop()
        socket?.close()
        socket = null
        _status.value = ConnectionStatus.DISCONNECTED
    }
}
