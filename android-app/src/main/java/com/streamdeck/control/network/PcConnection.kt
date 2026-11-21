package com.streamdeck.control.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicLong
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.settingsStore by preferencesDataStore(name = "connection")

data class ConnectionConfig(
    val host: String = "",
    val port: String = "8765",
    val token: String = "",
    val companionId: String = "",
    val automaticPairingAllowed: Boolean = true,
)
enum class ConnectionState { DISCONNECTED, DISCOVERING, CONNECTING, AUTHENTICATING, CONNECTED, RECONNECTING, ERROR }

class PcConnection(private val context: Context) {
    private val hostKey = stringPreferencesKey("host")
    private val portKey = stringPreferencesKey("port")
    private val tokenKey = stringPreferencesKey("token")
    private val companionIdKey = stringPreferencesKey("companion_id")
    private val automaticPairingAllowedKey = booleanPreferencesKey("automatic_pairing_allowed")
    private val keystoreAlias = "stream_deck_pairing_token"
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val socket = AtomicReference<WebSocket?>(null)
    private val onState = AtomicReference<(ConnectionState) -> Unit>({})
    private val onMessage = AtomicReference<(String) -> Unit>({})
    private val closed = AtomicReference(false)
    private val generation = AtomicLong(0)

    val config: Flow<ConnectionConfig> = context.settingsStore.data.map {
        ConnectionConfig(
            it[hostKey] ?: "",
            it[portKey] ?: "8765",
            decryptToken(it[tokenKey].orEmpty()),
            it[companionIdKey].orEmpty(),
            it[automaticPairingAllowedKey] ?: true,
        )
    }

    suspend fun saveConfig(config: ConnectionConfig) {
        require(config.port.toIntOrNull()?.let { it in 1..65535 } == true) {
            "Port must be between 1 and 65535"
        }
        require(config.host.isNotBlank()) { "PC host is required" }
        context.settingsStore.edit {
            it[hostKey] = config.host.trim()
            it[portKey] = config.port.trim().ifBlank { "8765" }
            it[tokenKey] = encryptToken(config.token.trim())
            val companionId = config.companionId.trim()
            if (companionId.isEmpty()) it.remove(companionIdKey) else it[companionIdKey] = companionId
            it[automaticPairingAllowedKey] = true
        }
    }

    suspend fun forgetConfig() {
        context.settingsStore.edit {
            it.remove(hostKey)
            it.remove(portKey)
            it.remove(tokenKey)
            it.remove(companionIdKey)
            it[automaticPairingAllowedKey] = false
        }
    }

    fun connect(config: ConnectionConfig) {
        val host = config.host.trim()
        val port = config.port.toIntOrNull()?.takeIf { it in 1..65535 } ?: return
        if (host.isBlank()) return
        closed.set(false)
        val connectionGeneration = generation.incrementAndGet()
        socket.getAndSet(null)?.close(1000, "Reconnect")
        onState.get().invoke(ConnectionState.CONNECTING)
        android.util.Log.i(TAG, "Connecting to ${safeHost(host)}:$port")
        val request = try {
            Request.Builder().url("ws://$host:$port").build()
        } catch (error: IllegalArgumentException) {
            onState.get().invoke(ConnectionState.DISCONNECTED)
            onMessage.get().invoke(JSONObject().put("type", "error")
                .put("message", "Invalid PC address").toString())
            return
        }
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (generation.get() != connectionGeneration) {
                    webSocket.close(1000, "Superseded connection")
                    return
                }
                socket.set(webSocket)
                onState.get().invoke(ConnectionState.AUTHENTICATING)
                if (config.token.isBlank()) {
                    webSocket.send(JSONObject().put("type", "pair_request").put("device", "Android device").toString())
                } else {
                    android.util.Log.i(TAG, "Existing pairing token found")
                    webSocket.send(JSONObject().put("type", "authenticate").put("token", config.token).toString())
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (generation.get() != connectionGeneration) return
                runCatching { JSONObject(text) }.getOrNull()?.let { message ->
                    val type = message.optString("type")
                    if (type == "authenticated") {
                        android.util.Log.i(TAG, "Authentication successful")
                        onState.get().invoke(ConnectionState.CONNECTED)
                    } else if (type == "error" && message.optString("request_id").isBlank()) {
                        onState.get().invoke(ConnectionState.ERROR)
                    }
                }
                onMessage.get().invoke(text)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (generation.get() != connectionGeneration) return
                val wasActive = socket.compareAndSet(webSocket, null)
                if (!wasActive && socket.get() != null) return
                android.util.Log.w(TAG, "Connection failed: ${t.javaClass.simpleName}")
                onState.get().invoke(ConnectionState.RECONNECTING)
                onMessage.get().invoke(JSONObject().put("type", "connection_error")
                    .put("message", "PC connection lost. Reconnecting...").toString())
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (generation.get() != connectionGeneration) return
                val wasActive = socket.compareAndSet(webSocket, null)
                if (wasActive && !closed.get()) {
                    onState.get().invoke(ConnectionState.RECONNECTING)
                } else if (closed.get()) {
                    onState.get().invoke(ConnectionState.DISCONNECTED)
                }
            }
        }
        try {
            client.newWebSocket(request, listener)
        } catch (error: IllegalArgumentException) {
            onState.get().invoke(ConnectionState.DISCONNECTED)
            onMessage.get().invoke(JSONObject().put("type", "error")
                .put("message", "Invalid PC address").toString())
        }
    }

    fun observe(state: (ConnectionState) -> Unit, message: (String) -> Unit) {
        onState.set(state)
        onMessage.set(message)
    }

    fun send(type: String, key: String, value: String = key): Boolean {
        val ws = socket.get() ?: return false
        val requestId = java.util.UUID.randomUUID().toString()
        return ws.send(JSONObject().put("type", type).put("key", key).put("value", value)
            .put("request_id", requestId).toString())
    }

    fun disconnect() {
        closed.set(true)
        generation.incrementAndGet()
        socket.getAndSet(null)?.close(1000, "Network changed")
        onState.get().invoke(ConnectionState.DISCONNECTED)
    }

    fun close() {
        disconnect()
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    private fun encryptToken(token: String): String {
        if (token.isEmpty()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, tokenKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decryptToken(value: String): String {
        if (value.isEmpty()) return ""
        return try {
            val payload = Base64.decode(value, Base64.NO_WRAP)
            require(payload.size > 12) { "Saved pairing token is invalid" }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, tokenKey(), GCMParameterSpec(128, payload.copyOfRange(0, 12)))
            String(cipher.doFinal(payload.copyOfRange(12, payload.size)), Charsets.UTF_8)
        } catch (error: Exception) {
            throw IllegalStateException("Unable to decrypt the saved pairing token; pair this device again.", error)
        }
    }

    private fun tokenKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keystoreAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(
                    keystoreAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private fun safeHost(host: String): String =
        if (host.matches(Regex("[A-Za-z0-9._:-]{1,253}"))) host else "<host>"

    private companion object {
        const val TAG = "StreamDeckConnection"
    }
}
