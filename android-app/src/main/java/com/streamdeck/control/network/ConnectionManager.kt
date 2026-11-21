package com.streamdeck.control.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class ConnectionManager(
    context: Context,
    private val connection: PcConnection,
    private val discovery: PcDiscovery,
) {
    private val appContext = context.applicationContext
    private val connectivity = appContext.getSystemService(ConnectivityManager::class.java)
    private val mutableState = MutableStateFlow(ConnectionState.DISCONNECTED)
    private val mutableStatus = MutableStateFlow("Searching for the Windows Companion")
    private val mutableUsbRoute = MutableStateFlow(false)
    private val retrySignals = Channel<Unit>(Channel.CONFLATED)
    private val operationMutex = Mutex()
    private val started = AtomicBoolean(false)
    private val configurationLoaded = CompletableDeferred<Unit>()
    private var scope: CoroutineScope? = null
    private var retryJob: Job? = null
    private var observedConfig: ConnectionConfig? = null
    @Volatile private var activeConfig = ConnectionConfig()
    @Volatile private var networkSignature = ""
    @Volatile private var networkAvailable = false
    @Volatile private var usbRouteAvailable = false
    @Volatile private var allowAutomaticPairing = true
    @Volatile private var onMessage: (String) -> Unit = {}

    val state = mutableState.asStateFlow()
    val status = mutableStatus.asStateFlow()
    val usbNetworkAvailable = mutableUsbRoute.asStateFlow()
    val config = connection.config
    val devices = discovery.devices

    fun start(scope: CoroutineScope, messageListener: (String) -> Unit) {
        if (!started.compareAndSet(false, true)) return
        this.scope = scope
        onMessage = messageListener

        connection.observe(
            state = { newState ->
                mutableState.value = newState
                if (newState == ConnectionState.CONNECTED) {
                    Log.i(TAG, "Connected")
                    mutableStatus.value = "Connected to ${activeConfig.host}"
                    persistActiveEndpoint()
                    retryJob?.cancel()
                    retryJob = null
                } else if (newState == ConnectionState.RECONNECTING || newState == ConnectionState.ERROR) {
                    if (mutableStatus.value != USB_UNAVAILABLE_MESSAGE) {
                        mutableStatus.value = "Connection lost. Searching for the companion…"
                    }
                    scheduleRetry()
                }
            },
            message = { raw ->
                val json = runCatching { org.json.JSONObject(raw) }.getOrNull()
                if (json?.optString("type") == "paired") {
                    val token = json.optString("token")
                    val pairedConfig = activeConfig.copy(token = token)
                    if (token.isNotBlank() && pairedConfig.host.isNotBlank()) {
                        Log.i(TAG, "Pairing approved; securely saving the new device token")
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            runCatching {
                                connection.saveConfig(pairedConfig)
                                activeConfig = pairedConfig
                                observedConfig = pairedConfig
                                connectTo(pairedConfig)
                            }.onFailure {
                                Log.e(TAG, "Could not save the approved pairing", it)
                            }
                        }
                    }
                }
                onMessage(raw)
                if (json?.optString("type") == "error" &&
                    json.optString("message").contains("Pairing token rejected", ignoreCase = true)
                ) {
                    Log.w(TAG, "Saved pairing was rejected by the discovered Companion")
                    connection.disconnect()
                    mutableStatus.value = "Saved pairing was rejected. Reconnect and pair this PC again."
                    mutableState.value = ConnectionState.ERROR
                    retryJob?.cancel()
                    retryJob = null
                }
            },
        )

        connectivity.registerDefaultNetworkCallback(networkCallback)
        discovery.start()

        scope.launch {
            discovery.devices.collect { devices ->
                if (devices.isNotEmpty() && mutableState.value != ConnectionState.CONNECTED) {
                    retrySignals.trySend(Unit)
                    scheduleRetry()
                }
            }
        }
        scope.launch {
            connection.config.collect { config ->
                val old = observedConfig
                observedConfig = config
                configurationLoaded.complete(Unit)
                activeConfig = config
                allowAutomaticPairing = config.automaticPairingAllowed
                if (config.token.isNotBlank()) Log.i(TAG, "Saved pairing token found")
                if (old == null || config != old) {
                    if (mutableState.value != ConnectionState.CONNECTED) {
                        scheduleRetry(INITIAL_DISCOVERY_DELAY_MILLIS)
                    }
                }
            }
        }
    }

    fun onForeground() {
        onNetworkChanged()
        discovery.start()
        if (mutableState.value != ConnectionState.CONNECTED) {
            Log.i(TAG, "Foreground; checking the saved companion connection")
            retrySignals.trySend(Unit)
            scheduleRetry(0L)
        }
    }

    suspend fun saveConfig(config: ConnectionConfig) {
        allowAutomaticPairing = true
        retryJob?.cancel()
        retryJob = null
        val savedConfig = config.copy(automaticPairingAllowed = true)
        activeConfig = savedConfig
        observedConfig = savedConfig
        connection.saveConfig(savedConfig)
        if (networkAvailable) connectTo(savedConfig) else scheduleRetry(0L)
    }

    suspend fun forgetPc() {
        allowAutomaticPairing = false
        retryJob?.cancel()
        retryJob = null
        connection.disconnect()
        activeConfig = ConnectionConfig(automaticPairingAllowed = false)
        observedConfig = activeConfig
        connection.forgetConfig()
        mutableStatus.value = "PC pairing forgotten. Connect manually to pair again."
        mutableState.value = ConnectionState.DISCONNECTED
    }

    fun send(type: String, key: String, value: String = key): Boolean =
        connection.send(type, key, value)

    fun close() {
        if (!started.compareAndSet(true, false)) return
        retryJob?.cancel()
        retryJob = null
        connectivity.unregisterNetworkCallback(networkCallback)
        discovery.stop()
        scope = null
        connection.close()
    }

    @Synchronized
    private fun scheduleRetry(initialDelay: Long = RETRY_MIN_MILLIS) {
        val currentScope = scope ?: return
        if (!started.get() || mutableState.value == ConnectionState.CONNECTED || retryJob?.isActive == true) return
        if (!allowAutomaticPairing && activeConfig.host.isBlank()) return
        retryJob = currentScope.launch {
            configurationLoaded.await()
            var backoff = initialDelay
            while (started.get() && mutableState.value != ConnectionState.CONNECTED) {
                if (!networkAvailable) {
                    mutableState.value = ConnectionState.DISCONNECTED
                    mutableStatus.value = if (usbRouteAvailable) {
                        "USB network interface detected; waiting for a usable route"
                    } else "Waiting for a network connection"
                    waitForSignalOrTimeout(NO_NETWORK_RETRY_MILLIS)
                    continue
                }

                mutableState.value = ConnectionState.DISCOVERING
                mutableStatus.value = if (usbRouteAvailable) {
                    "Searching USB network and Wi-Fi for the companion"
                } else {
                    "Searching for Stream Deck Companion"
                }
                discovery.start()
                if (discovery.devices.value.none { isFresh(it) } && activeConfig.host.isBlank()) {
                    waitForSignalOrTimeout(DISCOVERY_WAIT_MILLIS)
                } else if (discovery.devices.value.none { isFresh(it) } && backoff == INITIAL_DISCOVERY_DELAY_MILLIS) {
                    waitForSignalOrTimeout(DISCOVERY_WAIT_MILLIS)
                }

                operationMutex.withLock {
                    val candidates = buildCandidates()
                    for (candidate in candidates) {
                        if (!started.get() || mutableState.value == ConnectionState.CONNECTED) break
                        if (candidate.config.token.isBlank() && !allowAutomaticPairing) continue
                        activeConfig = candidate.config
                        connectTo(candidate.config)
                        val result = withTimeoutOrNull(
                            if (candidate.config.token.isBlank()) FIRST_PAIR_TIMEOUT_MILLIS else CONNECTION_ATTEMPT_TIMEOUT_MILLIS,
                        ) {
                            mutableState.first {
                                it == ConnectionState.CONNECTED ||
                                    it == ConnectionState.RECONNECTING ||
                                    it == ConnectionState.ERROR ||
                                    it == ConnectionState.DISCONNECTED
                            }
                        }
                        if (result == ConnectionState.CONNECTED) return@launch
                        connection.disconnect()
                    }
                }

                if (mutableState.value != ConnectionState.CONNECTED) {
                    mutableState.value = ConnectionState.RECONNECTING
                    Log.i(TAG, "Retrying companion discovery and connection")
                    waitForSignalOrTimeout(backoff)
                    backoff = min((backoff.coerceAtLeast(RETRY_MIN_MILLIS) * 2L), RETRY_MAX_MILLIS)
                }
            }
        }
    }

    private suspend fun waitForSignalOrTimeout(timeoutMillis: Long) {
        withTimeoutOrNull(timeoutMillis) { retrySignals.receive() }
    }

    private suspend fun connectTo(config: ConnectionConfig) {
        if (config.host.isBlank() || !networkAvailable) return
        mutableState.value = ConnectionState.CONNECTING
        mutableStatus.value = "Connecting to ${config.host}:${config.port}"
        Log.i(TAG, "Connecting to ${config.host}:${config.port}")
        connection.connect(config)
    }

    private fun buildCandidates(): List<ConnectionCandidate> {
        val saved = activeConfig
        val freshDevices = discovery.devices.value.filter(::isFresh)
        val sorted = freshDevices.sortedWith(
            compareByDescending<DiscoveredPc> { isUsbRoute(it.host) }
                .thenByDescending { saved.companionId.isNotBlank() && it.companionId == saved.companionId }
                .thenByDescending { saved.host.isNotBlank() && it.host == saved.host },
        )
        val discoveredCandidates = sorted.map { pc ->
            ConnectionCandidate(
                pc,
                saved.copy(
                    host = pc.host,
                    port = pc.port.toString(),
                    companionId = pc.companionId.ifBlank { saved.companionId },
                ),
            )
        }
        val savedCandidate = if (saved.host.isNotBlank() &&
            discoveredCandidates.none { it.config.host == saved.host && it.config.port == saved.port }
        ) {
            listOf(ConnectionCandidate(null, saved))
        } else emptyList()
        return discoveredCandidates + savedCandidate
    }

    private fun isUsbRoute(host: String): Boolean {
        val target = runCatching { InetAddress.getByName(host) as? Inet4Address }.getOrNull() ?: return false
        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList().any { network ->
                val name = network.name.lowercase()
                if (!network.isUp || !USB_INTERFACE_PATTERN.containsMatchIn(name)) return@any false
                network.interfaceAddresses.any { address ->
                    val local = address.address as? Inet4Address ?: return@any false
                    sameSubnet(local, target, address.networkPrefixLength)
                }
            }
        }.getOrDefault(false)
    }

    private fun sameSubnet(local: Inet4Address, remote: Inet4Address, prefixLength: Short): Boolean {
        val prefix = prefixLength.toInt().coerceIn(0, 32)
        val localBytes = local.address
        val remoteBytes = remote.address
        val fullBytes = prefix / 8
        val remainingBits = prefix % 8
        for (index in 0 until fullBytes) {
            if (localBytes[index] != remoteBytes[index]) return false
        }
        if (remainingBits == 0) return true
        val mask = (0xff shl (8 - remainingBits)) and 0xff
        return (localBytes[fullBytes].toInt() and 0xff and mask) ==
            (remoteBytes[fullBytes].toInt() and 0xff and mask)
    }

    private fun persistActiveEndpoint() {
        val currentScope = scope ?: return
        val config = activeConfig
        currentScope.launch {
            runCatching {
                if (config.host.isNotBlank()) connection.saveConfig(config)
            }.onFailure {
                Log.w(TAG, "Could not persist the discovered Companion endpoint", it)
            }
        }
    }

    private fun isFresh(device: DiscoveredPc): Boolean =
        System.currentTimeMillis() - device.lastSeenAt <= DEVICE_FRESHNESS_MILLIS

    private fun onNetworkChanged() {
        if (!started.get()) return
        val active = connectivity.activeNetwork
        val capabilities = active?.let { connectivity.getNetworkCapabilities(it) }
        val links = active?.let { connectivity.getLinkProperties(it) }
        val available = active != null && capabilities != null
        val usb = findUsbNetworkInterface() || links?.interfaceName?.let { USB_INTERFACE_PATTERN.containsMatchIn(it.lowercase()) } == true
        val signature = buildNetworkSignature(active, capabilities, links)
        val changed = networkSignature.isNotEmpty() && signature != networkSignature
        networkSignature = signature
        val networkWasAvailable = networkAvailable
        networkAvailable = available
        usbRouteAvailable = usb
        mutableUsbRoute.value = usb

        if (available) {
            Log.i(TAG, "Network available${if (usb) " (USB network route detected)" else ""}")
            if (!networkWasAvailable || changed) {
                if (changed) {
                    Log.i(TAG, "Network changed; closing the previous WebSocket")
                    connection.disconnect()
                }
                mutableStatus.value = if (usb) "USB network route available" else "Network available"
                retrySignals.trySend(Unit)
                scheduleRetry(if (changed) 0L else INITIAL_DISCOVERY_DELAY_MILLIS)
            }
        } else {
            if (networkWasAvailable) {
                Log.i(TAG, "Network unavailable")
                connection.disconnect()
            }
            mutableStatus.value = if (usb) USB_UNAVAILABLE_MESSAGE else "Waiting for a network connection"
            mutableState.value = ConnectionState.DISCONNECTED
        }
    }

    private fun buildNetworkSignature(
        network: Network?,
        capabilities: NetworkCapabilities?,
        links: LinkProperties?,
    ): String = listOf(
        network?.toString().orEmpty(),
        capabilities?.let {
            listOf(
                NetworkCapabilities.TRANSPORT_WIFI,
                NetworkCapabilities.TRANSPORT_ETHERNET,
                NetworkCapabilities.TRANSPORT_CELLULAR,
                NetworkCapabilities.TRANSPORT_VPN,
            ).joinToString(",") { transport -> it.hasTransport(transport).toString() }
        }.orEmpty(),
        links?.interfaceName.orEmpty(),
        links?.routes?.joinToString(",").orEmpty(),
        links?.linkAddresses?.joinToString(",").orEmpty(),
    ).joinToString("|")

    private fun findUsbNetworkInterface(): Boolean = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().any {
            it.isUp && USB_INTERFACE_PATTERN.containsMatchIn(it.name.lowercase())
        }
    }.getOrDefault(false)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = onNetworkChanged()
        override fun onLost(network: Network) = onNetworkChanged()
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) =
            onNetworkChanged()
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) =
            onNetworkChanged()
    }

    companion object {
        private const val TAG = "StreamDeckConnection"
        private const val INITIAL_DISCOVERY_DELAY_MILLIS = 1_500L
        private const val DISCOVERY_WAIT_MILLIS = 2_500L
        private const val RETRY_MIN_MILLIS = 1_500L
        private const val RETRY_MAX_MILLIS = 60_000L
        private const val NO_NETWORK_RETRY_MILLIS = 5_000L
        private const val CONNECTION_ATTEMPT_TIMEOUT_MILLIS = 8_000L
        private const val FIRST_PAIR_TIMEOUT_MILLIS = 120_000L
        private const val DEVICE_FRESHNESS_MILLIS = 15_000L
        private const val USB_UNAVAILABLE_MESSAGE =
            "USB connected, but USB network connection is unavailable"
        private val USB_INTERFACE_PATTERN = Regex("usb|rndis|ncm|tether")
    }

    private data class ConnectionCandidate(val device: DiscoveredPc?, val config: ConnectionConfig)
}

private fun <T> java.util.Enumeration<T>.toList(): List<T> =
    buildList { while (hasMoreElements()) add(nextElement()) }
