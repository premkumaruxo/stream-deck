package com.streamdeck.control.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

data class DiscoveredPc(
    val name: String,
    val host: String,
    val port: Int,
    val companionId: String = "",
    val protocolVersion: Int = 1,
    val lastSeenAt: Long = System.currentTimeMillis(),
)

class PcDiscovery(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val running = AtomicBoolean(false)
    private val found = MutableStateFlow<List<DiscoveredPc>>(emptyList())
    val devices = found.asStateFlow()
    @Volatile private var socket: DatagramSocket? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        Log.i(TAG, "Searching for Stream Deck Companion on UDP port $DISCOVERY_PORT")
        Thread({
            try {
                DatagramSocket(DISCOVERY_PORT, InetAddress.getByName("0.0.0.0")).use { listener ->
                    socket = listener
                    listener.soTimeout = 5_000
                    val buffer = ByteArray(2048)
                    while (running.get()) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            listener.receive(packet)
                            val data = JSONObject(String(packet.data, packet.offset, packet.length, Charsets.UTF_8))
                            if (data.optString("type") != "streamdeck_pc") continue
                            val port = data.optInt("port", 8765).takeIf { it in 1..65535 } ?: continue
                            val protocolVersion = data.optInt("protocol_version", 1)
                            if (protocolVersion != 1) continue
                            val pc = DiscoveredPc(
                                data.optString("name", "Windows PC").take(64),
                                packet.address.hostAddress ?: continue,
                                port,
                                data.optString("companion_id").take(64),
                                protocolVersion,
                            )
                            found.value = (
                                found.value.filterNot {
                                    it.host == pc.host || (pc.companionId.isNotBlank() && it.companionId == pc.companionId)
                                } + pc
                            ).takeLast(MAX_DEVICES)
                            Log.i(TAG, "Companion found: ${pc.name} at ${pc.host}:${pc.port}")
                        } catch (error: java.net.SocketTimeoutException) {
                            val cutoff = System.currentTimeMillis() - DEVICE_EXPIRY_MILLIS
                            found.value = found.value.filter { it.lastSeenAt >= cutoff }
                        } catch (error: Exception) {
                            if (running.get()) Log.w(TAG, "Ignoring invalid discovery datagram", error)
                        }
                    }
                }
            } catch (error: Exception) {
                if (running.get()) Log.e(TAG, "Discovery listener failed", error)
                running.set(false)
            }
        }, "streamdeck-discovery").apply { isDaemon = true; start() }
    }

    fun stop() {
        running.set(false)
        socket?.close()
        socket = null
    }

    private companion object {
        const val TAG = "StreamDeckDiscovery"
        const val DISCOVERY_PORT = 39394
        const val MAX_DEVICES = 10
        const val DEVICE_EXPIRY_MILLIS = 15_000L
    }
}
