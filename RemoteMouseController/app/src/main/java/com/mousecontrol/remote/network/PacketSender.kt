package com.mousecontrol.remote.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

/**
 * Shared send-loop used by every transport.
 *
 * MOVE packets are coalesced (only the latest matters for a continuous
 * gyro/trackpad gesture — dropping an intermediate frame just skips one
 * frame of motion). Every other packet type (clicks, drag down/up, volume,
 * brightness) goes through a small buffered queue so a fast double-tap or a
 * slider drag can never be silently dropped the way it would be on a single
 * conflated channel.
 */
abstract class PacketSender(private val scope: CoroutineScope) {

    private val moveChannel = Channel<String>(capacity = Channel.CONFLATED)
    private val eventChannel = Channel<String>(capacity = 64)
    private var job: Job? = null

    protected abstract suspend fun transmit(packet: String)

    protected fun start() {
        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                select<Unit> {
                    eventChannel.onReceive { transmit(it) }
                    moveChannel.onReceive { transmit(it) }
                }
            }
        }
    }

    fun enqueue(packet: String) {
        if (packet.startsWith("MOVE:")) moveChannel.trySend(packet) else eventChannel.trySend(packet)
    }

    protected fun stop() {
        job?.cancel()
    }
}
