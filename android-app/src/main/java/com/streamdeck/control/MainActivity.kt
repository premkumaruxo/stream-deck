package com.streamdeck.control

import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.streamdeck.control.data.DeckButtonEntity
import com.streamdeck.control.data.DeckDatabase
import com.streamdeck.control.data.MacroEntity
import com.streamdeck.control.data.ProfileEntity
import com.streamdeck.control.network.ConnectionConfig
import com.streamdeck.control.network.ConnectionState
import com.streamdeck.control.network.DiscoveredPc
import com.streamdeck.control.network.PcDiscovery
import com.streamdeck.control.network.PcConnection
import com.streamdeck.control.network.ConnectionManager
import com.streamdeck.control.ui.IconResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.streamdeck.control.ui.AppPreferences
import com.streamdeck.control.ui.AppThemeMode
import com.streamdeck.control.update.InstallResult
import com.streamdeck.control.update.UpdateInfo
import com.streamdeck.control.update.UpdateManager
import com.streamdeck.control.update.UpdateConfig
import com.streamdeck.control.update.UpdateSourceNotConfiguredException
import com.streamdeck.control.update.UpdateUiState
import com.streamdeck.control.update.UpdateVerificationException
import com.streamdeck.control.update.UpdateInstaller
import com.streamdeck.control.update.UpdateReminderDelay
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import java.io.File
import java.util.Calendar
import java.util.concurrent.atomic.AtomicBoolean

private val Cream = Color(0xFFF4F0E7)
private val Forest = Color(0xFF183D32)
private val Ink = Color(0xFF24332D)
private val Muted = Color(0xFF78817A)
private val Orange = Color(0xFFD8864B)

class MainActivity : ComponentActivity() {
    private var connectionManager: ConnectionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = DeckDatabase.get(this)
        val connection = PcConnection(applicationContext)
        val discovery = PcDiscovery(applicationContext)
        val connectionManager = ConnectionManager(applicationContext, connection, discovery)
        this.connectionManager = connectionManager
        val appPreferences = AppPreferences(applicationContext)
        val updateManager = UpdateManager(applicationContext, appPreferences)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DeckViewModel(db, connectionManager, appPreferences, updateManager) as T
        }
        setContent {
            MaterialTheme {
                val vm: DeckViewModel = viewModel(factory = factory)
                StreamDeckApp(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        connectionManager?.onForeground()
    }
}

class DeckViewModel(
    private val db: DeckDatabase,
    private val connectionManager: ConnectionManager,
    private val appPreferences: AppPreferences,
    private val updateManager: UpdateManager,
) : ViewModel() {
    private val dao = db.deckDao()
    val profiles = dao.observeProfiles()
    val favorites = dao.observeFavorites()
    val macros = dao.observeMacros()
    private val currentProfile = MutableStateFlow<ProfileEntity?>(null)
    val activeProfile = currentProfile.asStateFlow()
    private val selected = MutableStateFlow("")
    val selectedId = selected.asStateFlow()
    val buttons = selected.flatMapLatest { id ->
        if (id.isBlank()) kotlinx.coroutines.flow.flowOf(emptyList()) else dao.observeButtons(id)
    }
    val config = connectionManager.config
    val themeMode = appPreferences.theme
    val onlyButtonMode = appPreferences.onlyButtonMode
    val autoIcons = appPreferences.autoIcons
    val hapticsEnabled = appPreferences.haptics
    val automaticUpdateCheck = appPreferences.automaticUpdateCheck
    private val _updateState = MutableStateFlow(UpdateUiState(updateManager.installedVersion()))
    val updateState = _updateState.asStateFlow()
    private val updateOperationMutex = Mutex()
    private val updateDownloadMutex = Mutex()
    private val updateCheckRequested = AtomicBoolean(false)
    private val updateDownloadRequested = AtomicBoolean(false)
    val discoveredDevices = connectionManager.devices
    val connectionState = connectionManager.state
    val connectionStatus = connectionManager.status
    val usbNetworkAvailable = connectionManager.usbNetworkAvailable
    private val message = MutableStateFlow("")
    val statusMessage = message.asStateFlow()
    private val pairingToken = MutableStateFlow("")
    val token = pairingToken.asStateFlow()
    init {
        connectionManager.start(viewModelScope) { raw ->
            runCatching {
                val json = JSONObject(raw)
                when (json.optString("type")) {
                    "paired" -> {
                        val issuedToken = json.optString("token")
                        pairingToken.value = issuedToken
                        message.value = "Device paired. Saving the secure token and authenticating."
                    }

                    "authenticated" -> message.value = "Securely connected"
                    "command_result" -> message.value = if (json.optBoolean("ok")) {
                        "Action completed on PC"
                    } else {
                        "Command failed on PC"
                    }
                    "error" -> message.value = json.optString("message", "Command failed")
                    "connection_error" -> message.value = json.optString("message", "PC connection lost.")
                }
            }.onFailure { message.value = "Received an invalid response from the PC." }
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (dao.profile("video-editing") == null) {
                dao.saveProfile(ProfileEntity("video-editing", "Video Editing", "A ready-to-use creative workspace"))
                listOf(
                    Triple("Premiere Pro", "🎬", "launch_app|premiere_pro"),
                    Triple("Photoshop", "🎨", "launch_app|photoshop"),
                    Triple("After Effects", "✨", "launch_app|after_effects"),
                    Triple("DaVinci Resolve", "🎞️", "launch_app|resolve"),
                    Triple("OBS", "📹", "launch_app|obs"),
                    Triple("Save", "💾", "keyboard|CTRL+S"),
                    Triple("Undo", "↩️", "keyboard|CTRL+Z"),
                    Triple("Redo", "↪️", "keyboard|CTRL+Y"),
                    Triple("Cut", "✂️", "keyboard|CTRL+X"),
                    Triple("Copy", "▢", "keyboard|CTRL+C"),
                    Triple("Paste", "▣", "keyboard|CTRL+V"),
                    Triple("Delete", "⌫", "keyboard|DELETE"),
                    Triple("Play/Pause", "▶", "media|play_pause"),
                    Triple("Add Marker", "📍", "keyboard|M"),
                    Triple("Export", "📤", "keyboard|CTRL+M"),
                    Triple("Volume +", "🔊", "media|volume_up"),
                    Triple("Volume -", "🔉", "media|volume_down"),
                    Triple("Mute", "🔇", "media|mute"),
                    Triple("Fullscreen", "⛶", "keyboard|F11"),
                ).forEachIndexed { index, item ->
                    dao.saveButton(
                        DeckButtonEntity(UUID.randomUUID().toString(), "video-editing", item.first, emoji = "",
                            actionType = item.third.substringBefore('|'), actionValue = item.third.substringAfter('|'), position = index,
                            iconName = IconResolver.resolve(item.third.substringBefore('|'), item.third.substringAfter('|'), item.first),
                            iconMode = "auto"),
                    )
                }
            }
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            dao.allButtons().forEach { button ->
                if (button.iconMode == "auto") {
                    dao.saveButton(button.copy(iconName = IconResolver.resolve(
                        button.actionType, button.actionValue, button.title, button.subtitle,
                    )))
                }
            }
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                updateManager.cleanupAfterInstall()
                updateManager.cachedUpdate()?.let { cached ->
                    val cachedApk = if (cached.isAvailable) updateManager.findDownloadedApk(cached) else null
                    _updateState.value = _updateState.value.copy(
                        update = cached,
                        downloadedApkPath = cachedApk?.absolutePath,
                    )
                    refreshUpdateReminder(cached)
                }
                if (updateManager.shouldCheckAutomatically()) {
                    performUpdateCheck(showFailure = false, autoDownloadAvailable = true)
                }
            } catch (error: Exception) {
                android.util.Log.w("StreamDeckUpdate", "Unable to initialize automatic update check", error)
            }
        }
    }

    override fun onCleared() {
        connectionManager.close()
    }

    fun selectProfile(id: String) {
        selected.value = id
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            currentProfile.value = dao.profile(id)
        }
    }

    fun addProfile(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            dao.saveProfile(ProfileEntity(id, clean))
            selected.value = id
        }
    }

    fun addButton(title: String, actionType: String, actionValue: String) {
        val profileId = selected.value
        if (title.isBlank() || profileId.isBlank()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val position = dao.allButtons().count { it.profileId == profileId }
            dao.saveButton(DeckButtonEntity(UUID.randomUUID().toString(), profileId, title.trim(),
                emoji = "", actionType = actionType, actionValue = actionValue.trim(), position = position,
                iconName = IconResolver.resolve(actionType, actionValue, title), iconMode = "auto"))
        }
    }

    fun saveButton(button: DeckButtonEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val resolved = if (button.iconMode == "auto") {
                button.copy(iconName = IconResolver.resolve(button.actionType, button.actionValue, button.title, button.subtitle))
            } else button
            dao.saveButton(resolved)
        }
    }

    fun duplicateButton(button: DeckButtonEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val position = dao.allButtons().count { it.profileId == button.profileId }
            dao.saveButton(button.copy(id = UUID.randomUUID().toString(), title = "${button.title} copy", position = position))
        }
    }

    fun moveButton(button: DeckButtonEntity, delta: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val items = dao.allButtons().filter { it.profileId == button.profileId }.sortedBy { it.position }.toMutableList()
            val from = items.indexOfFirst { it.id == button.id }
            if (from < 0 || items.isEmpty()) return@launch
            val to = (from + delta).coerceIn(0, items.lastIndex)
            if (to != from) {
                val moved = items.removeAt(from)
                items.add(to, moved)
                items.forEachIndexed { index, item -> dao.saveButton(item.copy(position = index)) }
            }
        }
    }

    fun deleteButton(buttonId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { dao.deleteButton(buttonId) }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            dao.deleteButtonsForProfile(profileId)
            dao.deleteProfile(profileId)
            if (selected.value == profileId) selected.value = ""
        }
    }

    fun execute(button: DeckButtonEntity): Boolean {
        if (button.confirmationRequired) {
            message.value = "This action requires confirmation."
            return false
        }
        if (button.actionType == "macro") {
            viewModelScope.launch { runMacro(button.actionValue) }
            return true
        }
        val sent = connectionManager.send(button.actionType, button.actionValue)
        message.value = if (sent) "" else "PC disconnected. Reconnect in Settings."
        return sent
    }

    private suspend fun runMacro(id: String) {
        val macro = dao.allMacros().firstOrNull { it.id == id }
        if (macro == null) {
            message.value = "Macro not found."
            return
        }
        val steps = runCatching { JSONArray(macro.actionsJson) }.getOrElse {
            message.value = "Macro configuration is invalid."
            return
        }
        for (index in 0 until steps.length()) {
            val step = steps.optJSONObject(index) ?: continue
            if (step.optString("type") == "delay") {
                kotlinx.coroutines.delay(step.optLong("value", 0).coerceIn(0, 60_000))
            } else if (!connectionManager.send(step.optString("type"), step.optString("value"))) {
                message.value = "Macro stopped: PC disconnected."
                return
            }
        }
        message.value = ""
    }

    fun saveMacro(macro: MacroEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { dao.saveMacro(macro) }
    }

    fun deleteMacro(id: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { dao.deleteMacro(id) }
    }

    fun updateLayout(rows: Int, columns: Int) {
        val profileId = selected.value
        if (profileId.isBlank()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val profile = dao.profile(profileId) ?: return@launch
            dao.saveProfile(profile.copy(rows = rows.coerceIn(2, 12), columns = columns.coerceIn(2, 12)))
            currentProfile.value = profile.copy(rows = rows.coerceIn(2, 12), columns = columns.coerceIn(2, 12))
        }
    }

    fun renameProfile(profile: ProfileEntity, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            dao.saveProfile(profile.copy(name = name.trim()))
        }
    }

    fun duplicateProfile(profile: ProfileEntity) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            dao.saveProfile(profile.copy(id = id, name = "${profile.name} copy", isFavorite = false))
            dao.allButtons().filter { it.profileId == profile.id }.forEach { button ->
                dao.saveButton(button.copy(id = UUID.randomUUID().toString(), profileId = id))
            }
            selectProfile(id)
        }
    }

    suspend fun setTheme(mode: AppThemeMode) = appPreferences.setTheme(mode)
    suspend fun setOnlyButtonMode(enabled: Boolean) = appPreferences.setOnlyButtonMode(enabled)
    suspend fun setAutoIcons(enabled: Boolean) = appPreferences.setAutoIcons(enabled)
    suspend fun setHaptics(enabled: Boolean) = appPreferences.setHaptics(enabled)
    suspend fun setAutomaticUpdateCheck(enabled: Boolean) = appPreferences.setAutomaticUpdateCheck(enabled)

    fun checkForUpdates() {
        if (_updateState.value.isChecking || !updateCheckRequested.compareAndSet(false, true)) return
        _updateState.value = _updateState.value.copy(isChecking = true)
        viewModelScope.launch {
            try {
                performUpdateCheck(showFailure = true)
            } finally {
                updateCheckRequested.set(false)
            }
        }
    }

    private suspend fun performUpdateCheck(
        showFailure: Boolean,
        autoDownloadAvailable: Boolean = false,
    ) {
        updateOperationMutex.withLock {
            _updateState.value = _updateState.value.copy(
                isChecking = true,
                error = null,
                verificationFailed = false,
                downloadFailed = false,
            )
            try {
                val result = updateManager.checkForUpdates()
                _updateState.value = _updateState.value.copy(
                    update = result,
                    isChecking = false,
                    error = null,
                    verificationFailed = false,
                    downloadFailed = false,
                    downloadedApkPath = _updateState.value.downloadedApkPath
                        .takeIf { _updateState.value.update?.latestVersionCode == result.latestVersionCode },
                )
                refreshUpdateReminder(result)
                if (autoDownloadAvailable && result.isAvailable) startDownload(result)
            } catch (error: Exception) {
                _updateState.value = _updateState.value.copy(
                    isChecking = false,
                    error = if (!showFailure) null else when (error) {
                        is UpdateSourceNotConfiguredException ->
                            "Set GITHUB_USERNAME and GITHUB_REPOSITORY in UpdateConfig.kt to enable update checks."
                        else -> "Couldn't check for updates. Check your connection and try again."
                    },
                )
            }
        }
    }

    private suspend fun refreshUpdateReminder(info: UpdateInfo) {
        val visible = info.isAvailable &&
            updateManager.reminderAllowed(info.latestVersionCode.toString(), info.mandatory)
        _updateState.value = _updateState.value.copy(showReminder = visible)
    }

    fun downloadUpdate() {
        val info = _updateState.value.update ?: return
        startDownload(info)
    }

    fun onInstallerVerificationFailed() {
        _updateState.value = _updateState.value.copy(
            downloadedApkPath = null,
            error = "Update verification failed. The rejected APK was removed.",
            verificationFailed = true,
            downloadFailed = false,
        )
    }

    private fun startDownload(info: UpdateInfo) {
        if (!info.isAvailable || _updateState.value.isDownloading || _updateState.value.isChecking ||
            !updateDownloadRequested.compareAndSet(false, true)
        ) return
        viewModelScope.launch {
            try {
                updateDownloadMutex.withLock {
                    _updateState.value = _updateState.value.copy(
                        isDownloading = true,
                        downloadBytes = 0L,
                        downloadTotalBytes = info.sizeBytes,
                        downloadedApkPath = null,
                        error = null,
                        verificationFailed = false,
                        downloadFailed = false,
                    )
                    try {
                        val apk = updateManager.download(info) { received, total ->
                            _updateState.value = _updateState.value.copy(
                                downloadBytes = received,
                                downloadTotalBytes = total ?: info.sizeBytes,
                            )
                        }
                        _updateState.value = _updateState.value.copy(
                            isDownloading = false,
                            downloadedApkPath = apk.absolutePath,
                            error = null,
                            downloadFailed = false,
                        )
                    } catch (error: Exception) {
                        val verificationFailed = error is UpdateVerificationException
                        android.util.Log.w("StreamDeckUpdate", "APK download failed", error)
                        _updateState.value = _updateState.value.copy(
                            isDownloading = false,
                            downloadedApkPath = null,
                            error = if (verificationFailed) "Update verification failed. The downloaded file was removed."
                                else "Download failed. Check your connection and try again.",
                            verificationFailed = verificationFailed,
                            downloadFailed = !verificationFailed,
                        )
                    }
                }
            } finally {
                updateDownloadRequested.set(false)
            }
        }
    }

    fun remindLater(delay: UpdateReminderDelay) {
        val info = _updateState.value.update?.takeIf { it.isAvailable } ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val until = when (delay) {
                UpdateReminderDelay.LATER_TODAY -> Calendar.getInstance().apply {
                    add(Calendar.HOUR_OF_DAY, 6)
                }.timeInMillis
                UpdateReminderDelay.TOMORROW -> Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                UpdateReminderDelay.IN_THREE_DAYS -> now + 3L * 24L * 60L * 60L * 1000L
            }
            appPreferences.snoozeUpdateReminder(until)
            _updateState.value = _updateState.value.copy(showReminder = false)
            delay((until - now).coerceAtLeast(0L))
            refreshUpdateReminder(info)
        }
    }

    fun ignoreUpdateVersion() {
        val version = _updateState.value.update?.takeIf { it.isAvailable }?.latestVersionCode?.toString() ?: return
        viewModelScope.launch {
            appPreferences.ignoreUpdateVersion(version)
            _updateState.value = _updateState.value.copy(showReminder = false)
        }
    }

    suspend fun saveConnection(config: ConnectionConfig) = connectionManager.saveConfig(config)
    suspend fun forgetPc() = connectionManager.forgetPc()

    suspend fun backup(): String {
        val profiles = dao.allProfiles()
        val result = JSONArray()
        val allButtons = dao.allButtons()
        profiles.forEach { profile ->
            val items = JSONArray()
            allButtons.filter { it.profileId == profile.id }.forEach { b ->
                items.put(JSONObject().put("id", b.id).put("title", b.title).put("subtitle", b.subtitle)
                    .put("emoji", b.emoji).put("actionType", b.actionType).put("actionValue", b.actionValue)
                    .put("position", b.position).put("confirmationRequired", b.confirmationRequired)
                    .put("backgroundColor", b.backgroundColor).put("textColor", b.textColor)
                    .put("iconColor", b.iconColor).put("borderColor", b.borderColor)
                    .put("borderWidth", b.borderWidth).put("cornerRadius", b.cornerRadius)
                    .put("shadowElevation", b.shadowElevation).put("fontSize", b.fontSize)
                    .put("iconSize", b.iconSize).put("spanWidth", b.spanWidth)
                    .put("spanHeight", b.spanHeight).put("haptic", b.haptic)
                    .put("pressAnimation", b.pressAnimation).put("imageUri", b.imageUri)
                    .put("iconName", b.iconName).put("iconMode", b.iconMode))
            }
            result.put(JSONObject().put("id", profile.id).put("name", profile.name)
                .put("description", profile.description).put("columns", profile.columns).put("rows", profile.rows)
                .put("buttons", items))
        }
        val macroData = JSONArray()
        dao.allMacros().forEach { macro ->
            macroData.put(JSONObject().put("id", macro.id).put("name", macro.name).put("actionsJson", macro.actionsJson))
        }
        return JSONObject().put("version", 2).put("profiles", result).put("macros", macroData).toString(2)
    }

    suspend fun restore(json: String) {
        val data = JSONObject(json)
        require(data.optInt("version", 0) in 1..2) { "Unsupported backup version" }
        val list = data.getJSONArray("profiles")
        val macroData = data.optJSONArray("macros") ?: JSONArray()
        val importedMacroIds = buildMap {
            for (i in 0 until macroData.length()) {
                val macro = macroData.getJSONObject(i)
                put(macro.optString("id"), UUID.randomUUID().toString())
            }
        }
        db.withTransaction {
            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val id = UUID.randomUUID().toString()
                dao.saveProfile(ProfileEntity(id, item.getString("name"), item.optString("description"),
                    item.optInt("columns", 4), item.optInt("rows", 4)))
                val buttons = item.optJSONArray("buttons") ?: JSONArray()
                for (j in 0 until buttons.length()) {
                    val b = buttons.getJSONObject(j)
                    dao.saveButton(DeckButtonEntity(
                        id = UUID.randomUUID().toString(), profileId = id, title = b.getString("title"),
                        subtitle = b.optString("subtitle"), emoji = b.optString("emoji", "●"),
                        actionType = b.getString("actionType"),
                        actionValue = if (b.getString("actionType") == "macro") {
                            importedMacroIds[b.getString("actionValue")] ?: b.getString("actionValue")
                        } else b.getString("actionValue"),
                        position = b.optInt("position", j), confirmationRequired = b.optBoolean("confirmationRequired"),
                        backgroundColor = b.optString("backgroundColor", "#FFFFFF"),
                        textColor = b.optString("textColor", "#183D32"), iconColor = b.optString("iconColor", "#183D32"),
                        borderColor = b.optString("borderColor", "#E8E5DC"), borderWidth = b.optDouble("borderWidth", 0.0).toFloat(),
                        cornerRadius = b.optInt("cornerRadius", 20), shadowElevation = b.optInt("shadowElevation", 2),
                        fontSize = b.optInt("fontSize", 12), iconSize = b.optInt("iconSize", 23),
                        spanWidth = b.optInt("spanWidth", 1), spanHeight = b.optInt("spanHeight", 1),
                        haptic = b.optBoolean("haptic", true), pressAnimation = b.optBoolean("pressAnimation", true),
                        imageUri = b.optString("imageUri"),
                        iconName = b.optString("iconName", IconResolver.resolve(
                            b.getString("actionType"), b.optString("actionValue"), b.getString("title"),
                        )),
                        iconMode = b.optString("iconMode", "auto"),
                    ))
                }
            }
            for (i in 0 until macroData.length()) {
                val macro = macroData.getJSONObject(i)
                val oldId = macro.optString("id")
                dao.saveMacro(MacroEntity(importedMacroIds[oldId] ?: UUID.randomUUID().toString(),
                    macro.getString("name"), macro.getString("actionsJson")))
            }
        }
    }
}

@Composable
private fun StreamDeckApp(vm: DeckViewModel) {
    val profiles by vm.profiles.collectAsState(initial = emptyList())
    val buttons by vm.buttons.collectAsState(initial = emptyList())
    val favorites by vm.favorites.collectAsState(initial = emptyList())
    val macros by vm.macros.collectAsState(initial = emptyList())
    val connection by vm.connectionState.collectAsState()
    val config by vm.config.collectAsState(initial = ConnectionConfig())
    val message by vm.statusMessage.collectAsState()
    val connectionStatus by vm.connectionStatus.collectAsState()
    val usbNetworkAvailable by vm.usbNetworkAvailable.collectAsState()
    val pairedToken by vm.token.collectAsState()
    val discoveredDevices by vm.discoveredDevices.collectAsState()
    val themeMode by vm.themeMode.collectAsState(initial = AppThemeMode.SYSTEM)
    val onlyButtonMode by vm.onlyButtonMode.collectAsState(initial = false)
    val autoIcons by vm.autoIcons.collectAsState(initial = true)
    val hapticsEnabled by vm.hapticsEnabled.collectAsState(initial = true)
    val automaticUpdateCheck by vm.automaticUpdateCheck.collectAsState(initial = true)
    val updateState by vm.updateState.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf("Deck") }
    LaunchedEffect(updateState.update?.latestVersionCode, updateState.update?.mandatory) {
        val update = updateState.update
        if (update?.isAvailable == true && update.mandatory) selectedTab = "App Update"
    }
    var showConnection by rememberSaveable { mutableStateOf(false) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var editingButton by remember { mutableStateOf<DeckButtonEntity?>(null) }
    var showDesigner by rememberSaveable { mutableStateOf(false) }
    var showMacroBuilder by rememberSaveable { mutableStateOf(false) }
    var editingMacro by remember { mutableStateOf<MacroEntity?>(null) }
    var profileDialog by remember { mutableStateOf<Pair<String, ProfileEntity>?>(null) }
    val selectedId by vm.selectedId.collectAsState()
    val activeProfile = profiles.firstOrNull { it.id == selectedId } ?: profiles.firstOrNull()
    LaunchedEffect(activeProfile?.id) { activeProfile?.let { vm.selectProfile(it.id) } }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.backup().toByteArray(Charsets.UTF_8)) }
                    ?: error("Unable to open the selected file")
            }.onFailure {
                android.widget.Toast.makeText(context, "Backup failed: ${it.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Unable to open the selected file")
                vm.restore(json)
            }.onSuccess {
                android.widget.Toast.makeText(context, "Backup restored", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure {
                android.widget.Toast.makeText(context, "Restore failed: ${it.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val darkSystem = isSystemInDarkTheme()
    val useDarkTheme = themeMode == AppThemeMode.DARK || (themeMode == AppThemeMode.SYSTEM && darkSystem)
    val colorScheme = if (useDarkTheme) {
        darkColorScheme(
            primary = Color(0xFFD0D4D1),
            onPrimary = Color(0xFF171A19),
            background = Color(0xFF111312),
            onBackground = Color(0xFFE7E9E8),
            surface = Color(0xFF191C1A),
            onSurface = Color(0xFFE7E9E8),
            secondary = Color(0xFF9DA39F),
        )
    } else {
        lightColorScheme(
            primary = Forest,
            onPrimary = Color.White,
            background = Color(0xFFF3F2EE),
            onBackground = Ink,
            surface = Color(0xFFFFFFFF),
            onSurface = Ink,
            secondary = Orange,
        )
    }
    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            containerColor = colorScheme.background,
            bottomBar = {
                if (!onlyButtonMode || selectedTab != "Deck") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center) {
                    Surface(shape = RoundedCornerShape(32.dp), color = Color.White, shadowElevation = 8.dp) {
                        Row(Modifier.fillMaxWidth().padding(5.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf("Home", "Deck", "Profiles", "Controls", "Settings").forEach { tab ->
                                val selected = selectedTab == tab ||
                                    (selectedTab in setOf("Layouts", "Add Control", "Macros") && tab == "Deck") ||
                                    (selectedTab == "App Update" && tab == "Settings")
                                Surface(
                                    onClick = { selectedTab = tab },
                                    shape = RoundedCornerShape(28.dp),
                                    color = if (selected) Forest else Color.Transparent,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Column(Modifier.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            when (tab) {
                                                "Home" -> Icons.Default.Home
                                                "Settings" -> Icons.Default.Settings
                                                else -> Icons.Default.MoreVert
                                            },
                                            contentDescription = tab,
                                            tint = if (selected) Color.White else Muted,
                                            modifier = Modifier.size(19.dp),
                                        )
                                        Text(tab, fontSize = 9.sp, color = if (selected) Color.White else Muted)
                                    }
                                }
                            }
                        }
                    }
                }
                }
            },
            floatingActionButton = {
                if (selectedTab == "Deck" && !onlyButtonMode) FloatingActionButton(
                    onClick = { editingButton = null; showDesigner = true }, containerColor = Forest, contentColor = Color.White,
                ) { Icon(Icons.Default.Add, contentDescription = "Add button") }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Header(
                    connection,
                    activeProfile?.name ?: "Create your first profile",
                    onlyButtonMode,
                    onExitOnlyButtonMode = { scope.launch { vm.setOnlyButtonMode(false) } },
                    onSettings = { showConnection = true },
                )
                updateState.update?.takeIf {
                    selectedTab != "App Update" && updateState.showReminder && it.isAvailable &&
                        (it.mandatory || !onlyButtonMode)
                }?.let { availableUpdate ->
                    UpdateReminderBanner(
                        version = availableUpdate.latestVersion,
                        mandatory = availableUpdate.mandatory,
                        onView = { selectedTab = "App Update" },
                        onRemindLater = vm::remindLater,
                        onIgnore = vm::ignoreUpdateVersion,
                    )
                }
                when (selectedTab) {
                    "Home" -> HomeScreen(connection, activeProfile?.name, favorites.map { it.name },
                        onOpenDeck = { selectedTab = "Deck" },
                        onAdd = { editingButton = null; showDesigner = true },
                        onConnect = { showConnection = true },
                        onProfile = { selectedTab = "Profiles" },
                        onControls = { selectedTab = "Controls" },
                        onLayouts = { selectedTab = "Layouts" },
                        onQuick = { type, key ->
                            if (!vm.executeControl(type, key)) {
                                android.widget.Toast.makeText(context, "PC disconnected", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        })
                    "Deck" -> DeckScreen(activeProfile, buttons, editMode, message, onlyButtonMode, hapticsEnabled,
                        onToggleEdit = { editMode = !editMode },
                        onPress = vm::execute,
                        onEdit = { editingButton = it; showDesigner = true },
                        onMove = vm::moveButton,
                        onDuplicate = vm::duplicateButton,
                        onDelete = vm::deleteButton,
                        onAdd = { editingButton = null; showDesigner = true },
                        onLayouts = { selectedTab = "Layouts" },
                        onAddControl = { selectedTab = "Add Control" },
                        onMacros = { selectedTab = "Macros" })
                    "Profiles" -> ProfilesScreen(
                        profiles = profiles, selected = selectedId,
                        onSelect = { vm.selectProfile(it); selectedTab = "Deck" },
                        onAdd = { profileDialog = "create" to ProfileEntity("", "") },
                        onRename = { profileDialog = "rename" to it },
                        onDuplicate = vm::duplicateProfile,
                        onDelete = vm::deleteProfile,
                    )
                    "Controls" -> ControlsScreen(onPress = { type, key ->
                        if (!vm.executeControl(type, key)) {
                            android.widget.Toast.makeText(context, "PC disconnected", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }, onAddControl = { selectedTab = "Add Control" }, onMacro = { selectedTab = "Macros" })
                    "Layouts" -> LayoutsScreen(activeProfile, vm::updateLayout)
                    "Add Control" -> AddControlScreen(
                        macros = macros,
                        onAdd = { title, type, value ->
                            vm.addButton(title, type, value)
                            selectedTab = "Deck"
                        },
                        onCreate = { editingButton = null; showDesigner = true },
                    )
                    "Macros" -> MacroScreen(
                        macros = macros,
                        onNew = { editingMacro = null; showMacroBuilder = true },
                        onEdit = { editingMacro = it; showMacroBuilder = true },
                        onDelete = vm::deleteMacro,
                        onAddToDeck = { macro ->
                            vm.addButton(macro.name, "macro", macro.id)
                            selectedTab = "Deck"
                        },
                    )
                    "Settings" -> SettingsScreen(config, connection, message, pairedToken,
                        connectionStatus, usbNetworkAvailable, themeMode, onlyButtonMode, autoIcons, hapticsEnabled,
                        onTheme = { scope.launch { vm.setTheme(it) } },
                        onOnlyButtonMode = { scope.launch { vm.setOnlyButtonMode(it) } },
                        onAutoIcons = { scope.launch { vm.setAutoIcons(it) } },
                        onHaptics = { scope.launch { vm.setHaptics(it) } },
                        automaticUpdateCheck = automaticUpdateCheck,
                        onAutomaticUpdateCheck = { scope.launch { vm.setAutomaticUpdateCheck(it) } },
                        onAppUpdate = { selectedTab = "App Update" },
                        onForgetPc = { scope.launch { vm.forgetPc() } },
                        onConnection = { showConnection = true },
                        onExport = { exportLauncher.launch("stream-deck-backup.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/*")) })
                    "App Update" -> UpdateCenterScreen(
                        state = updateState,
                        updateSourceConfigured = UpdateConfig.isConfigured,
                        automaticUpdateCheck = automaticUpdateCheck,
                        onAutomaticUpdateCheck = { scope.launch { vm.setAutomaticUpdateCheck(it) } },
                        onCheck = vm::checkForUpdates,
                        onDownload = vm::downloadUpdate,
                        onInstallerVerificationFailed = vm::onInstallerVerificationFailed,
                        onRemindLater = vm::remindLater,
                        onIgnore = vm::ignoreUpdateVersion,
                        onBack = { selectedTab = "Settings" },
                    )
                    else -> HomeScreen(connection, activeProfile?.name, favorites.map { it.name },
                        onOpenDeck = { selectedTab = "Deck" }, onAdd = { selectedTab = "Add Control" },
                        onConnect = { showConnection = true }, onProfile = { selectedTab = "Profiles" },
                        onControls = { selectedTab = "Controls" }, onLayouts = { selectedTab = "Layouts" },
                        onQuick = { type, key -> vm.executeControl(type, key) })
                }
            }
        }
    }
    if (showConnection) ConnectionDialog(config, pairedToken, discoveredDevices,
        onDismiss = { showConnection = false },
        onForgetPc = { scope.launch { vm.forgetPc(); showConnection = false } }) {
        scope.launch {
            runCatching { vm.saveConnection(it) }
                .onSuccess { showConnection = false }
                .onFailure {
                    android.widget.Toast.makeText(context, it.message ?: "Connection settings are invalid",
                        android.widget.Toast.LENGTH_LONG).show()
                }
        }
    }
    if (showDesigner) {
        Dialog(onDismissRequest = { showDesigner = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            ButtonDesigner(
                initial = editingButton,
                macros = macros,
                profileId = activeProfile?.id.orEmpty(),
                nextPosition = buttons.size,
                onDismiss = { showDesigner = false },
                onSave = { button -> vm.saveButton(button); showDesigner = false },
            )
        }
    }
    if (showMacroBuilder) {
        Dialog(onDismissRequest = { showMacroBuilder = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            MacroBuilder(editingMacro, onDismiss = { showMacroBuilder = false }, onSave = {
                vm.saveMacro(it); showMacroBuilder = false
            })
        }
    }
    profileDialog?.let { (mode, profile) ->
        TextEntryDialog(
            title = if (mode == "create") "Create profile" else "Rename profile",
            hint = "Profile name",
            initialValue = if (mode == "rename") profile.name else "",
            onDismiss = { profileDialog = null },
        ) { name ->
            if (mode == "create") vm.addProfile(name) else vm.renameProfile(profile, name)
            profileDialog = null
        }
    }
}

private fun DeckViewModel.executeControl(type: String, key: String): Boolean {
    return execute(DeckButtonEntity(UUID.randomUUID().toString(), "", key, actionType = type,
        actionValue = key, position = 0))
}

@Composable
private fun Header(
    state: ConnectionState,
    profile: String,
    onlyButtonMode: Boolean,
    onExitOnlyButtonMode: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Home, contentDescription = "Stream Deck", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text("Stream Deck", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(profile, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .25f)),
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(
                    if (state == ConnectionState.CONNECTED) Color(0xFF66A17D) else Color(0xFF9A9A96),
                    CircleShape,
                ))
                Text(state.label(), Modifier.padding(start = 7.dp), fontSize = 11.sp)
            }
        }
        if (onlyButtonMode) {
            IconButton(onClick = onExitOnlyButtonMode) {
                Icon(Icons.Default.Close, contentDescription = "Exit Only Button Mode")
            }
        }
        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
    }
}

private fun ConnectionState.label() = when (this) {
    ConnectionState.CONNECTED -> "Connected"
    ConnectionState.DISCOVERING -> "Discovering"
    ConnectionState.CONNECTING -> "Connecting"
    ConnectionState.AUTHENTICATING -> "Authenticating"
    ConnectionState.RECONNECTING -> "Reconnecting"
    ConnectionState.ERROR -> "Error"
    ConnectionState.DISCONNECTED -> "Disconnected"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeScreen(state: ConnectionState, profile: String?, favorites: List<String>,
                       onOpenDeck: () -> Unit, onAdd: () -> Unit, onConnect: () -> Unit, onProfile: () -> Unit,
                       onControls: () -> Unit, onLayouts: () -> Unit, onQuick: (String, String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Forest), shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onConnect)) {
                Column(Modifier.padding(22.dp)) {
                    Text(if (state == ConnectionState.CONNECTED) "Your PC is ready" else "Connect your PC",
                        color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (state == ConnectionState.CONNECTED) "Secure local connection active" else "Pair your Windows companion to control your PC",
                        color = Color.White.copy(alpha = .75f), modifier = Modifier.padding(top = 5.dp))
                    TextButton(onClick = onConnect, modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                        Icon(Icons.Default.Link, null); Spacer(Modifier.width(6.dp)); Text(if (state == ConnectionState.CONNECTED) "Connection settings" else "Connect PC")
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Current profile", color = Muted, fontSize = 13.sp)
                    Text(profile ?: "Create a profile", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Forest)
                }
                Button(onClick = onOpenDeck, shape = RoundedCornerShape(50)) { Text("Open deck") }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onProfile)) {
                Column(Modifier.padding(18.dp)) {
                    Text("Quick controls", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("Volume −", "media", "volume_down"),
                            Triple("Mute", "media", "mute"),
                            Triple("Volume +", "media", "volume_up"),
                            Triple("Screenshot", "system", "screenshot"),
                        ).forEach { (label, type, key) ->
                            Surface(shape = RoundedCornerShape(14.dp), color = Cream, onClick = { onQuick(type, key) }) {
                                Text(label, Modifier.padding(horizontal = 12.dp, vertical = 10.dp), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onLayouts, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f)) {
                    Text("Choose layout")
                }
                Button(onClick = onControls, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Forest)) {
                    Text("Add control")
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Favorites", Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onProfile) { Text("All profiles") }
            }
            Text(favorites.ifEmpty { listOf("Video Editing") }.joinToString("  ·  "), color = Muted, fontSize = 13.sp)
        }
        item {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Add a button")
            }
        }
    }
}

@Composable
private fun DeckScreen(profile: ProfileEntity?, buttons: List<DeckButtonEntity>, editMode: Boolean, statusMessage: String,
                       onlyButtonMode: Boolean, hapticsEnabled: Boolean,
                       onToggleEdit: () -> Unit, onPress: (DeckButtonEntity) -> Unit,
                       onEdit: (DeckButtonEntity) -> Unit, onMove: (DeckButtonEntity, Int) -> Unit,
                       onDuplicate: (DeckButtonEntity) -> Unit, onDelete: (String) -> Unit,
                       onAdd: () -> Unit, onLayouts: () -> Unit, onAddControl: () -> Unit,
                       onMacros: () -> Unit) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (onlyButtonMode) {
        when {
            buttons.size <= 1 -> 1
            buttons.size <= 4 -> if (landscape) buttons.size else 2
            buttons.size <= 6 -> 3
            buttons.size <= 12 -> if (landscape) 4 else 3
            else -> if (landscape) 6 else 4
        }
    } else {
        (profile?.columns ?: 4).coerceIn(2, 8).let { saved ->
            if (landscape) saved.coerceAtLeast(4) else saved.coerceAtMost(3)
        }
    }

    Row(Modifier.fillMaxSize()) {
        if (!onlyButtonMode) {
            Surface(
                modifier = Modifier.width(if (landscape) 82.dp else 68.dp).fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(Modifier.fillMaxSize().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Controls" to Icons.Default.Home,
                        "Profiles" to Icons.Default.MoreVert,
                        "Favorites" to Icons.Default.Edit,
                        "Settings" to Icons.Default.Settings,
                    ).forEachIndexed { index, (label, icon) ->
                        Surface(
                            onClick = {
                                when (index) {
                                    0 -> onAddControl()
                                    1 -> onLayouts()
                                    2 -> onAdd()
                                    3 -> onMacros()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (index == 0) MaterialTheme.colorScheme.primary.copy(alpha = .12f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        ) {
                            Column(Modifier.padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(icon, contentDescription = label, modifier = Modifier.size(21.dp),
                                    tint = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .68f))
                                Text(label, fontSize = 9.sp, maxLines = 1,
                                    color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = .68f))
                            }
                        }
                    }
                }
            }
        }

        Column(Modifier.weight(1f).fillMaxSize().padding(horizontal = if (onlyButtonMode) 18.dp else 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(profile?.name ?: "Create a profile", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("${buttons.size} configured buttons", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .58f))
                    Box(Modifier.fillMaxWidth().height(16.dp)) {
                        if (statusMessage.isNotBlank()) {
                            Text(
                                statusMessage,
                                fontSize = 11.sp,
                                color = Orange,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (!onlyButtonMode) {
                    TextButton(onClick = onLayouts) { Text("Layout") }
                    TextButton(onClick = onToggleEdit) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(if (editMode) "Done" else "Edit")
                    }
                }
            }
            if (buttons.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (!onlyButtonMode) Button(onClick = onAdd, shape = RoundedCornerShape(16.dp)) { Text("Add your first button") }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (onlyButtonMode) 12.dp else 9.dp),
                    verticalArrangement = Arrangement.spacedBy(if (onlyButtonMode) 12.dp else 9.dp),
                ) {
                    items(buttons, key = { it.id }, span = { GridItemSpan(if (onlyButtonMode) 1 else it.spanWidth.coerceIn(1, columns)) }) { button ->
                        DeckButton(button, editMode, columns, hapticsEnabled,
                            onClick = { onPress(button.copy(confirmationRequired = false)) },
                            onEdit = { onEdit(button) },
                            onDuplicate = { onDuplicate(button) },
                            onDelete = { onDelete(button.id) },
                            onMove = { delta -> onMove(button, delta) })
                    }
                }
            }
        }

        if (!onlyButtonMode && landscape) {
            Surface(Modifier.width(104.dp).fillMaxSize().padding(end = 8.dp), color = Color.Transparent) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("QUICK CONTROLS", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .55f))
                    listOf(
                        "Volume" to Icons.AutoMirrored.Filled.VolumeUp,
                        "Brightness" to Icons.Default.BrightnessHigh,
                        "Display" to Icons.Default.DesktopWindows,
                        "Theme" to Icons.Default.Settings,
                    ).forEach { (label, icon) ->
                        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Icon(icon, contentDescription = label, modifier = Modifier.size(19.dp))
                                Text(label, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeckButton(
    button: DeckButtonEntity,
    editMode: Boolean,
    columns: Int,
    hapticsEnabled: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val pressScale by animateFloatAsState(
        if (pressed && button.pressAnimation) .985f else 1f,
        animationSpec = tween(
            durationMillis = if (pressed) 75 else 115,
            easing = FastOutSlowInEasing,
        ),
        label = "button-press",
    )
    val pressOffset by animateDpAsState(
        if (pressed && button.pressAnimation) 1.dp else 0.dp,
        animationSpec = tween(
            durationMillis = if (pressed) 75 else 115,
            easing = FastOutSlowInEasing,
        ),
        label = "button-press-offset",
    )
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    var dragOffset by remember(button.id) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var showActions by remember { mutableStateOf(false) }
    var showConfirmation by remember { mutableStateOf(false) }
    val bg = colorFromHex(button.backgroundColor, Color.White)
    val pressedBg = lerp(bg, Color.Black, .035f)
    val surfaceColor by animateColorAsState(
        if (pressed && button.pressAnimation) pressedBg else bg,
        animationSpec = tween(
            durationMillis = if (pressed) 75 else 115,
            easing = FastOutSlowInEasing,
        ),
        label = "button-surface",
    )
    val elevation by animateDpAsState(
        if (pressed && button.pressAnimation) (button.shadowElevation - 1).coerceAtLeast(0).dp
        else button.shadowElevation.dp,
        animationSpec = tween(
            durationMillis = if (pressed) 75 else 115,
            easing = FastOutSlowInEasing,
        ),
        label = "button-elevation",
    )
    val fg = colorFromHex(button.textColor, Forest)
    val borderColor = colorFromHex(button.borderColor, Cream)
    val bitmap = rememberImageBitmap(button.imageUri)
    LaunchedEffect(interaction, editMode, button.haptic, hapticsEnabled) {
        interaction.interactions.collect { event ->
            if (event is PressInteraction.Press && !editMode && button.haptic && hapticsEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth().height((96 * button.spanHeight.coerceIn(1, 4)).dp)
            .graphicsLayer { translationX = dragOffset.x; translationY = dragOffset.y }
            .border(button.borderWidth.dp, borderColor, RoundedCornerShape(button.cornerRadius.dp))
            .combinedClickable(interactionSource = interaction, indication = LocalIndication.current,
                onClick = { if (editMode) showActions = true else {
                    if (button.confirmationRequired) showConfirmation = true else onClick()
                } }, onLongClick = { showActions = true }, onClickLabel = button.title)
            .pointerInput(editMode, button.id, columns) {
                if (editMode) {
                    var total = androidx.compose.ui.geometry.Offset.Zero
                    detectDragGesturesAfterLongPress(
                        onDragStart = { total = androidx.compose.ui.geometry.Offset.Zero },
                        onDrag = { change, amount ->
                            change.consume()
                            total += amount
                            dragOffset = total
                        },
                        onDragEnd = {
                            val cellWidth = with(density) { (configuration.screenWidthDp.dp / columns.toFloat()).toPx() }
                            val cellHeight = with(density) { 106.dp.toPx() }
                            var delta = (total.x / cellWidth).roundToInt() +
                                (total.y / cellHeight).roundToInt() * columns
                            if (delta == 0 && total.getDistance() > 24f) delta = if (kotlin.math.abs(total.x) > kotlin.math.abs(total.y)) {
                                if (total.x > 0) 1 else -1
                            } else if (total.y > 0) columns else -columns
                            if (delta == 0) showActions = true else onMove(delta)
                            dragOffset = androidx.compose.ui.geometry.Offset.Zero
                        },
                        onDragCancel = { dragOffset = androidx.compose.ui.geometry.Offset.Zero },
                    )
                }
            },
        shape = RoundedCornerShape(button.cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Column(
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationY = with(density) { pressOffset.toPx() }
            }.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            if (bitmap != null) {
                Image(bitmap, contentDescription = button.title, modifier = Modifier.size(button.iconSize.dp),
                    contentScale = ContentScale.Crop)
            } else {
                Icon(
                    IconResolver.icon(button.iconName),
                    contentDescription = button.title,
                    modifier = Modifier.size(button.iconSize.dp),
                    tint = colorFromHex(button.iconColor, MaterialTheme.colorScheme.onSurface),
                )
            }
            Text(button.title, fontSize = button.fontSize.sp, fontWeight = FontWeight.Medium, maxLines = 2, color = fg)
            if (button.subtitle.isNotBlank()) Text(button.subtitle, fontSize = 10.sp, maxLines = 1, color = fg.copy(alpha = .68f))
        }
    }
    if (showActions) AlertDialog(
        onDismissRequest = { showActions = false },
        title = { Text(button.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { showActions = false; onEdit() }) { Text("Edit button") }
                TextButton(onClick = { showActions = false; onEdit() }) { Text("Resize") }
                TextButton(onClick = { onMove(-1); showActions = false }) { Text("Move earlier") }
                TextButton(onClick = { onMove(1); showActions = false }) { Text("Move later") }
                TextButton(onClick = { showActions = false; onDuplicate() }) { Text("Duplicate") }
                TextButton(onClick = { showActions = false; onDelete() }) { Text("Delete") }
            }
        },
        confirmButton = { TextButton(onClick = { showActions = false }) { Text("Done") } },
    )
    if (showConfirmation) AlertDialog(
        onDismissRequest = { showConfirmation = false },
        title = { Text("Confirm action") },
        text = { Text("Run “${button.title}” on the connected PC?") },
        confirmButton = {
            Button(onClick = { showConfirmation = false; onClick() }) { Text("Run action") }
        },
        dismissButton = { TextButton(onClick = { showConfirmation = false }) { Text("Cancel") } },
    )
}

@Composable
private fun ProfilesScreen(
    profiles: List<ProfileEntity>,
    selected: String,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    onRename: (ProfileEntity) -> Unit,
    onDuplicate: (ProfileEntity) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Profiles", Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Forest)
            Button(onClick = onAdd, shape = RoundedCornerShape(50)) { Icon(Icons.Default.Add, null); Text("New") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(profiles.size) { i ->
                val profile = profiles[i]
                Card(shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = if (profile.id == selected) Color(0xFFE5EDE7) else Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("◈", color = Forest, fontSize = 20.sp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(profile.name, fontWeight = FontWeight.Medium)
                            Text(profile.description.ifBlank { "${profile.columns} × ${profile.rows} layout" }, fontSize = 12.sp, color = Muted)
                        }
                        if (profile.id == selected) Icon(Icons.Default.Check, null, tint = Forest)
                        TextButton(onClick = { onSelect(profile.id) }) { Text("Select") }
                      }
                      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                          TextButton(onClick = { onRename(profile) }) { Text("Rename") }
                          TextButton(onClick = { onDuplicate(profile) }) { Text("Duplicate") }
                          TextButton(onClick = { onDelete(profile.id) }) { Text("Delete", color = Orange) }
                      }
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonDesigner(
    initial: DeckButtonEntity?,
    macros: List<MacroEntity>,
    profileId: String,
    nextPosition: Int,
    onDismiss: () -> Unit,
    onSave: (DeckButtonEntity) -> Unit,
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var subtitle by remember(initial?.id) { mutableStateOf(initial?.subtitle.orEmpty()) }
    var imageUri by remember(initial?.id) { mutableStateOf(initial?.imageUri.orEmpty()) }
    var iconMode by remember(initial?.id) { mutableStateOf(initial?.iconMode ?: "auto") }
    var iconName by remember(initial?.id) {
        mutableStateOf(
            initial?.iconName?.takeIf { it.isNotBlank() }
                ?: IconResolver.resolve(
                    initial?.actionType ?: "keyboard",
                    initial?.actionValue ?: "CTRL+S",
                    initial?.title.orEmpty(),
                    initial?.subtitle.orEmpty(),
                ),
        )
    }
    var background by remember(initial?.id) { mutableStateOf(initial?.backgroundColor ?: "#183D32") }
    var textColor by remember(initial?.id) { mutableStateOf(initial?.textColor ?: "#FFFFFF") }
    var iconColor by remember(initial?.id) { mutableStateOf(initial?.iconColor ?: "#FFFFFF") }
    var borderColor by remember(initial?.id) { mutableStateOf(initial?.borderColor ?: "#E8E5DC") }
    var borderWidth by remember(initial?.id) { mutableStateOf(initial?.borderWidth ?: 0f) }
    var radius by remember(initial?.id) { mutableStateOf(initial?.cornerRadius ?: 20) }
    var elevation by remember(initial?.id) { mutableStateOf(initial?.shadowElevation ?: 2) }
    var fontSize by remember(initial?.id) { mutableStateOf(initial?.fontSize ?: 12) }
    var iconSize by remember(initial?.id) { mutableStateOf(initial?.iconSize ?: 23) }
    var spanWidth by remember(initial?.id) { mutableStateOf(initial?.spanWidth ?: 1) }
    var spanHeight by remember(initial?.id) { mutableStateOf(initial?.spanHeight ?: 1) }
    var haptic by remember(initial?.id) { mutableStateOf(initial?.haptic ?: true) }
    var animation by remember(initial?.id) { mutableStateOf(initial?.pressAnimation ?: true) }
    var type by remember(initial?.id) { mutableStateOf(initial?.actionType ?: "keyboard") }
    var value by remember(initial?.id) { mutableStateOf(initial?.actionValue ?: "CTRL+S") }
    var confirmation by remember(initial?.id) { mutableStateOf(initial?.confirmationRequired ?: false) }
    var actionMenu by remember { mutableStateOf(false) }
    var showIconPicker by remember(initial?.id) { mutableStateOf(initial == null) }
    val context = LocalContext.current
    val previewImage = rememberImageBitmap(imageUri)
    LaunchedEffect(iconMode, type, value, title, subtitle) {
        if (iconMode == "auto") iconName = IconResolver.resolve(type, value, title, subtitle)
    }
    val validColors = listOf(background, textColor, iconColor, borderColor).all { candidate ->
        runCatching { android.graphics.Color.parseColor(candidate) }.isSuccess
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            imageUri = uri.toString()
        }
    }

    Surface(Modifier.fillMaxSize(), color = Cream, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (initial == null) "Design button" else "Edit button", fontSize = 23.sp,
                        fontWeight = FontWeight.Bold, color = Forest)
                    Text("Make it yours. Your changes save to this deck.", fontSize = 12.sp, color = Muted)
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("LIVE PREVIEW", fontSize = 11.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                Card(
                    modifier = Modifier.fillMaxWidth().height((100 * spanHeight.coerceIn(1, 3)).dp)
                        .border(borderWidth.dp, colorFromHex(borderColor, Cream), RoundedCornerShape(radius.dp)),
                    shape = RoundedCornerShape(radius.dp),
                    colors = CardDefaults.cardColors(containerColor = colorFromHex(background, Color.White)),
                    elevation = CardDefaults.cardElevation(elevation.dp),
                ) {
                    Column(Modifier.fillMaxSize().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        if (previewImage != null) {
                            Image(previewImage, contentDescription = "Button image preview",
                                modifier = Modifier.size(iconSize.dp), contentScale = ContentScale.Crop)
                        } else {
                            Icon(
                                IconResolver.icon(iconName),
                                contentDescription = "Button icon preview",
                                modifier = Modifier.size(iconSize.dp),
                                tint = colorFromHex(iconColor, Forest),
                            )
                        }
                        Text(title.ifBlank { "Button title" }, fontSize = fontSize.sp, color = colorFromHex(textColor, Forest),
                            fontWeight = FontWeight.SemiBold)
                        Text(subtitle.ifBlank { if (imageUri.isBlank()) "Preview" else "Image selected" },
                            fontSize = 11.sp, color = colorFromHex(textColor, Forest).copy(alpha = .7f))
                    }
                }
                DesignerSection("CONTENT") {
                    OutlinedTextField(title, { title = it.take(40) }, label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(subtitle, { subtitle = it.take(60) }, label = { Text("Subtitle") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            IconResolver.icon(iconName),
                            contentDescription = "Selected icon",
                            modifier = Modifier.size(32.dp),
                            tint = colorFromHex(iconColor, Forest),
                        )
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text("Icon", fontWeight = FontWeight.Medium)
                            Text(IconResolver.registry.firstOrNull { it.name == iconName }?.label ?: "Dashboard",
                                fontSize = 12.sp, color = Muted)
                        }
                        TextButton(onClick = { showIconPicker = true }) {
                            Text("Choose icon")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { iconMode = "auto" }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (iconMode == "auto") "Auto Detect" else "Reset to automatic")
                        }
                        TextButton(onClick = { showIconPicker = true }) {
                            Text(if (iconMode == "manual") "Manual icon" else "Choose manually")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { imagePicker.launch(arrayOf("image/*")) }, shape = RoundedCornerShape(14.dp)) {
                            Text(if (imageUri.isBlank()) "Choose image" else "Change image")
                        }
                    }
                    if (imageUri.isNotBlank()) TextButton(onClick = { imageUri = "" }) { Text("Remove image") }
                }
                DesignerSection("APPEARANCE") {
                    if (!validColors) Text("Enter valid color values such as #183D32.", color = Orange, fontSize = 12.sp)
                    OutlinedTextField(background, { background = it }, label = { Text("Background color (#RRGGBB)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(textColor, { textColor = it }, label = { Text("Text color") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(iconColor, { iconColor = it }, label = { Text("Icon color") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(borderColor, { borderColor = it }, label = { Text("Border color") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    ValueStepper("Border", "${borderWidth.roundToInt()} dp", { borderWidth = (borderWidth + it).coerceIn(0f, 6f) })
                    ValueStepper("Radius", "$radius dp", { radius = (radius + it).coerceIn(0, 40) })
                    ValueStepper("Shadow", "$elevation dp", { elevation = (elevation + it).coerceIn(0, 16) })
                    ValueStepper("Font size", "$fontSize sp", { fontSize = (fontSize + it).coerceIn(8, 24) })
                    ValueStepper("Icon size", "$iconSize sp", { iconSize = (iconSize + it).coerceIn(12, 48) })
                }
                DesignerSection("GRID SIZE") {
                    ValueStepper("Width", "$spanWidth cells", { spanWidth = (spanWidth + it).coerceIn(1, 8) })
                    ValueStepper("Height", "$spanHeight cells", { spanHeight = (spanHeight + it).coerceIn(1, 4) })
                }
                DesignerSection("ACTION") {
                    Box {
                        OutlinedButtonLike("${type.replace('_', ' ').replaceFirstChar { it.uppercase() }}  ▾") {
                            actionMenu = true
                        }
                        DropdownMenu(expanded = actionMenu, onDismissRequest = { actionMenu = false }) {
                            listOf("keyboard", "mouse", "media", "system", "launch_app", "open_url", "macro").forEach { option ->
                                DropdownMenuItem(text = { Text(option.replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        type = option
                                        value = when (option) {
                                            "keyboard" -> "CTRL+S"
                                            "mouse" -> "left_click"
                                            "media" -> "play_pause"
                                            "system" -> "screenshot"
                                            "launch_app" -> "chrome"
                                            "open_url" -> "https://"
                                            "macro" -> macros.firstOrNull()?.id.orEmpty()
                                            else -> ""
                                        }
                                        actionMenu = false
                                    })
                            }
                        }
                    }
                    if (type == "macro" && macros.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButtonLike(macros.firstOrNull { it.id == value }?.name ?: "Choose macro") { expanded = true }
                            DropdownMenu(expanded, { expanded = false }) {
                                macros.forEach { macro -> DropdownMenuItem(
                                    text = { Text(macro.name) },
                                    onClick = { value = macro.id; expanded = false },
                                ) }
                            }
                        }
                    } else {
                        OutlinedTextField(value, { value = it }, label = { Text("Action value") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                }
                DesignerSection("BEHAVIOR") {
                    LabeledSwitch("Haptic feedback", haptic) { haptic = it }
                    LabeledSwitch("Press animation", animation) { animation = it }
                    LabeledSwitch("Require confirmation", confirmation) { confirmation = it }
                }
            }
            Button(
                onClick = {
                    if (title.isNotBlank() && profileId.isNotBlank() && value.isNotBlank()) {
                        onSave(DeckButtonEntity(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            profileId = profileId,
                            title = title.trim(),
                            subtitle = subtitle.trim(),
                            emoji = initial?.emoji.orEmpty(),
                            actionType = type,
                            actionValue = value.trim(),
                            position = initial?.position ?: nextPosition,
                            confirmationRequired = confirmation,
                            backgroundColor = background,
                            textColor = textColor,
                            iconColor = iconColor,
                            borderColor = borderColor,
                            borderWidth = borderWidth,
                            cornerRadius = radius,
                            shadowElevation = elevation,
                            fontSize = fontSize,
                            iconSize = iconSize,
                            spanWidth = spanWidth,
                            spanHeight = spanHeight,
                            haptic = haptic,
                            pressAnimation = animation,
                            imageUri = imageUri,
                            iconName = iconName,
                            iconMode = iconMode,
                        ))
                    }
                },
                enabled = title.isNotBlank() && profileId.isNotBlank() && value.isNotBlank() && validColors,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(54.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("Save button") }
        }
    }
    if (showIconPicker) {
        IconPickerDialog(
            selectedIcon = iconName,
            onDismiss = { showIconPicker = false },
            onSelect = { selected ->
                iconName = selected
                iconMode = "manual"
                showIconPicker = false
            },
        )
    }
}

@Composable
private fun IconPickerDialog(
    selectedIcon: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }
    val configuration = LocalConfiguration.current
    val gridHeight = (configuration.screenHeightDp * .34f).coerceAtLeast(100f).dp
    val icons = remember(query, category) {
        IconResolver.registry.filter { entry ->
            (category == "All" || entry.category == category) &&
                (query.isBlank() || entry.name.contains(query.trim(), ignoreCase = true) ||
                    entry.label.contains(query.trim(), ignoreCase = true))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose icon") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = configuration.screenHeightDp.dp * .8f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search icons") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search icons")
                    },
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(IconResolver.categories) { item ->
                        val isSelected = category == item
                        Surface(
                            onClick = { category = item },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                item,
                                Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
                if (icons.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(gridHeight), contentAlignment = Alignment.Center) {
                        Text("No matching icons", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(68.dp),
                        modifier = Modifier.fillMaxWidth().height(gridHeight),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(icons, key = { it.name }) { entry ->
                            val isSelected = selectedIcon == entry.name
                            Surface(
                                onClick = { onSelect(entry.name) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = .14f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().height(64.dp).padding(5.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        entry.icon,
                                        contentDescription = entry.label,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        entry.label,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun DesignerSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ValueStepper(title: String, value: String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), color = Ink)
        TextButton(onClick = { onChange(-1) }) { Text("−") }
        Text(value, color = Forest, fontSize = 12.sp)
        TextButton(onClick = { onChange(1) }) { Text("+") }
    }
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onCheckedChange)
    }
}

@Composable
private fun OutlinedButtonLike(label: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = Cream, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, Modifier.padding(14.dp), color = Forest)
    }
}

private data class ControlPreset(val category: String, val title: String, val type: String, val value: String)

@Composable
private fun AddControlScreen(macros: List<MacroEntity>, onAdd: (String, String, String) -> Unit, onCreate: () -> Unit) {
    val presets = listOf(
        ControlPreset("Keyboard", "Save · Ctrl+S", "keyboard", "CTRL+S"),
        ControlPreset("Keyboard", "Undo · Ctrl+Z", "keyboard", "CTRL+Z"),
        ControlPreset("Keyboard", "Copy · Ctrl+C", "keyboard", "CTRL+C"),
        ControlPreset("Keyboard", "Paste · Ctrl+V", "keyboard", "CTRL+V"),
        ControlPreset("Keyboard", "Alt + Tab", "keyboard", "ALT+TAB"),
        ControlPreset("Mouse", "Left click", "mouse", "left_click"),
        ControlPreset("Mouse", "Right click", "mouse", "right_click"),
        ControlPreset("Application", "Premiere Pro", "launch_app", "premiere_pro"),
        ControlPreset("Application", "Photoshop", "launch_app", "photoshop"),
        ControlPreset("Application", "After Effects", "launch_app", "after_effects"),
        ControlPreset("Application", "DaVinci Resolve", "launch_app", "resolve"),
        ControlPreset("Application", "OBS Studio", "launch_app", "obs"),
        ControlPreset("Application", "Open Chrome", "launch_app", "chrome"),
        ControlPreset("Application", "File Explorer", "launch_app", "file_explorer"),
        ControlPreset("Media", "Play / Pause", "media", "play_pause"),
        ControlPreset("Media", "Volume +", "media", "volume_up"),
        ControlPreset("Media", "Volume −", "media", "volume_down"),
        ControlPreset("Media", "Mute", "media", "mute"),
        ControlPreset("System", "Screenshot", "system", "screenshot"),
        ControlPreset("System", "Lock PC", "system", "lock"),
        ControlPreset("System", "Task Manager", "system", "task_manager"),
        ControlPreset("Web", "Open web page", "open_url", "https://example.com"),
        ControlPreset("Custom", "Custom keyboard shortcut", "keyboard", "CTRL+SHIFT+S"),
    )
    var category by rememberSaveable { mutableStateOf("All") }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Add control", fontSize = 24.sp, color = Forest, fontWeight = FontWeight.Bold)
                Text("Choose an action to add to your active deck", color = Muted, fontSize = 12.sp)
            }
            TextButton(onClick = onCreate) { Icon(Icons.Default.Add, null); Text("Custom") }
        }
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 10.dp)) {
            items(listOf("All", "Keyboard", "Mouse", "Application", "Media", "System", "Web", "Macro")) { item ->
                Surface(
                    onClick = { category = item }, shape = RoundedCornerShape(50),
                    color = if (category == item) Forest else Color.White,
                ) {
                    Text(item, Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        color = if (category == item) Color.White else Forest, fontSize = 12.sp)
                }
            }
        }
        val filtered = if (category == "All") presets else presets.filter { it.category == category }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(filtered.size) { index ->
                val control = filtered[index]
                Card(
                    onClick = { onAdd(control.title, control.type, control.value) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).background(Cream, CircleShape), contentAlignment = Alignment.Center) {
                            Text(control.category.take(1), color = Forest, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(control.title, fontWeight = FontWeight.SemiBold)
                            Text("${control.category} · ${control.value}", fontSize = 11.sp, color = Muted)
                        }
                        Icon(Icons.Default.Add, null, tint = Forest)
                    }
                }
            }
            if (category == "All" || category == "Macro") {
                items(macros.size) { index ->
                    val macro = macros[index]
                    Card(onClick = { onAdd(macro.name, "macro", macro.id) },
                        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("≋", color = Forest, fontSize = 22.sp)
                            Text(macro.name, Modifier.weight(1f).padding(start = 12.dp), fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.Add, null, tint = Forest)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroScreen(
    macros: List<MacroEntity>,
    onNew: () -> Unit,
    onEdit: (MacroEntity) -> Unit,
    onDelete: (String) -> Unit,
    onAddToDeck: (MacroEntity) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Macro builder", fontSize = 24.sp, color = Forest, fontWeight = FontWeight.Bold)
                Text("Chain actions and timed pauses into one button.", color = Muted, fontSize = 12.sp)
            }
            Button(onClick = onNew, shape = RoundedCornerShape(50)) { Icon(Icons.Default.Add, null); Text("New") }
        }
        Spacer(Modifier.height(14.dp))
        if (macros.isEmpty()) {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(22.dp)) {
                    Text("Start with a simple sequence", fontWeight = FontWeight.SemiBold, color = Forest)
                    Text("Add shortcuts and delays, then add your macro to the deck.", color = Muted,
                        modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(macros.size) { index ->
                val macro = macros[index]
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(macro.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Forest)
                        Text("${runCatching { JSONArray(macro.actionsJson).length() }.getOrDefault(0)} steps",
                            color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        Row {
                            TextButton(onClick = { onEdit(macro) }) { Text("Edit") }
                            TextButton(onClick = { onAddToDeck(macro) }) { Text("Add to deck") }
                            TextButton(onClick = { onDelete(macro.id) }) { Text("Delete", color = Orange) }
                        }
                    }
                }
            }
        }
    }
}

private data class MacroStep(val type: String, val value: String)

@Composable
private fun MacroBuilder(initial: MacroEntity?, onDismiss: () -> Unit, onSave: (MacroEntity) -> Unit) {
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    val steps = remember(initial?.id) {
        mutableStateOf(
            initial?.let { runCatching {
                val array = JSONArray(it.actionsJson)
                (0 until array.length()).map { i ->
                    val item = array.getJSONObject(i)
                    MacroStep(item.optString("type"), item.optString("value"))
                }
            }.getOrDefault(emptyList()) } ?: emptyList(),
        )
    }
    var actionType by remember { mutableStateOf("keyboard") }
    var actionValue by remember { mutableStateOf("CTRL+S") }
    var delay by remember { mutableStateOf("500") }
    var editingStep by remember { mutableStateOf<Int?>(null) }
    var typeMenu by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = Cream, shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (initial == null) "New macro" else "Edit macro", Modifier.weight(1f),
                    fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Forest)
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
            OutlinedTextField(name, { name = it }, label = { Text("Macro name") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                steps.value.forEachIndexed { index, step ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}.", color = Muted)
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(if (step.type == "delay") "Wait ${step.value} ms" else "${step.type}: ${step.value}",
                                    fontWeight = FontWeight.Medium)
                                TextButton(onClick = {
                                    editingStep = index
                                    if (step.type == "delay") {
                                        delay = step.value
                                    } else {
                                        actionType = step.type
                                        actionValue = step.value
                                    }
                                }) { Text("Load to editor") }
                            }
                            TextButton(onClick = {
                                if (index > 0) {
                                    val copy = steps.value.toMutableList()
                                    val item = copy.removeAt(index); copy.add(index - 1, item); steps.value = copy
                                }
                            }) { Text("↑") }
                            TextButton(onClick = {
                                if (index < steps.value.lastIndex) {
                                    val copy = steps.value.toMutableList()
                                    val item = copy.removeAt(index); copy.add(index + 1, item); steps.value = copy
                                }
                            }) { Text("↓") }
                            TextButton(onClick = { steps.value = steps.value.toMutableList().also { it.removeAt(index) } }) {
                                Text("×", color = Orange)
                            }
                        }
                    }
                }
            }
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add an action", fontWeight = FontWeight.SemiBold, color = Forest)
                    Box {
                        OutlinedButtonLike("$actionType  ▾") { typeMenu = true }
                        DropdownMenu(typeMenu, { typeMenu = false }) {
                            listOf("keyboard", "mouse", "media", "system", "launch_app", "open_url").forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = {
                                    actionType = it
                                    actionValue = when (it) {
                                        "keyboard" -> "CTRL+S"
                                        "mouse" -> "left_click"
                                        "media" -> "play_pause"
                                        "system" -> "screenshot"
                                        "launch_app" -> "chrome"
                                        else -> "https://example.com"
                                    }
                                    editingStep = null
                                    typeMenu = false
                                })
                            }
                        }
                    }
                    OutlinedTextField(actionValue, { actionValue = it }, label = { Text("Action key / alias") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (actionValue.isNotBlank()) {
                                val step = MacroStep(actionType, actionValue.trim())
                                val target = editingStep
                                steps.value = if (target == null) steps.value + step else
                                    steps.value.toMutableList().also { if (target in it.indices) it[target] = step }
                                editingStep = null
                            }
                        }, modifier = Modifier.weight(1f), enabled = actionValue.isNotBlank()) {
                            Text(if (editingStep == null) "Add action" else "Update action")
                        }
                        OutlinedTextField(delay, { delay = it.filter(Char::isDigit).take(5) },
                            label = { Text("Delay ms") }, modifier = Modifier.weight(1f), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        TextButton(onClick = {
                            val ms = delay.toLongOrNull()?.coerceIn(0, 60_000) ?: 500
                            val step = MacroStep("delay", ms.toString())
                            val target = editingStep
                            steps.value = if (target == null) steps.value + step else
                                steps.value.toMutableList().also { if (target in it.indices) it[target] = step }
                            editingStep = null
                        }) { Text(if (editingStep == null) "Wait" else "Update wait") }
                    }
                }
            }
            Button(
                onClick = {
                    val json = JSONArray()
                    steps.value.forEach { json.put(JSONObject().put("type", it.type).put("value", it.value)) }
                    onSave(MacroEntity(initial?.id ?: UUID.randomUUID().toString(), name.trim(), json.toString()))
                },
                enabled = name.isNotBlank() && steps.value.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp),
            ) { Text("Save macro") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlsScreen(onPress: (String, String) -> Unit, onAddControl: () -> Unit, onMacro: () -> Unit) {
    val controls = listOf(
        "Media" to listOf("play_pause", "previous", "next", "volume_up", "volume_down", "mute"),
        "System" to listOf("screenshot", "lock", "task_manager", "open_settings", "file_explorer"),
        "Keyboard" to listOf("CTRL+S", "ALT+TAB", "WIN+D", "CTRL+Z", "CTRL+SHIFT+S"),
        "Mouse" to listOf("left_click", "right_click", "scroll_up", "scroll_down"),
    )
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Controls", Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Forest)
                TextButton(onClick = onAddControl) { Text("Add control") }
                TextButton(onClick = onMacro) { Text("Macros") }
            }
        }
        controls.forEach { (category, keys) ->
            item {
                Text(category, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keys.forEach { key ->
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White,
                            onClick = { onPress(if (category == "Keyboard") "keyboard" else if (category == "Mouse") "mouse" else if (category == "Media") "media" else "system", key) }) {
                            Text(key.replace('_', ' '), Modifier.padding(horizontal = 14.dp, vertical = 12.dp), color = Forest)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayoutsScreen(profile: ProfileEntity?, onSelect: (Int, Int) -> Unit) {
    val presets = listOf(2 to 2, 3 to 3, 4 to 4, 4 to 5, 5 to 5, 4 to 6, 5 to 6, 5 to 8)
    var customRows by rememberSaveable(profile?.id) { mutableStateOf(profile?.rows?.toString() ?: "4") }
    var customColumns by rememberSaveable(profile?.id) { mutableStateOf(profile?.columns?.toString() ?: "4") }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Layouts", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Forest)
        Text("Choose a grid for ${profile?.name ?: "your profile"}. Changes save immediately.",
            color = Muted, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(presets.size) { index ->
                val (rows, columns) = presets[index]
                val selected = profile?.rows == rows && profile.columns == columns
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) Color(0xFFE5EDE7) else Color.White,
                    onClick = { onSelect(rows, columns) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$columns × $rows", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text("${rows * columns} buttons", color = Muted, fontSize = 12.sp)
                        if (selected) Text("  ✓", color = Forest)
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Custom grid", fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(customColumns, { customColumns = it.filter(Char::isDigit).take(2) },
                                label = { Text("Columns") }, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(customRows, { customRows = it.filter(Char::isDigit).take(2) },
                                label = { Text("Rows") }, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        Button(onClick = {
                            val cols = customColumns.toIntOrNull()
                            val rows = customRows.toIntOrNull()
                            if (cols != null && rows != null && cols in 2..12 && rows in 2..12) onSelect(rows, cols)
                        }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Apply custom layout") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PillAction(label: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(50), color = Color.White, onClick = onClick) {
        Text(label, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = Forest, fontSize = 12.sp)
    }
}

private fun colorFromHex(value: String, fallback: Color): Color {
    return runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(fallback)
}

@Composable
private fun rememberImageBitmap(uri: String): ImageBitmap? {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, uri) {
        value = if (uri.isBlank()) null else withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri))
                    ?.use { BitmapFactory.decodeStream(it) }
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }
    return image
}

@Composable
private fun SettingsScreen(config: ConnectionConfig, state: ConnectionState, message: String, token: String,
                           connectionStatus: String, usbNetworkAvailable: Boolean,
                           themeMode: AppThemeMode, onlyButtonMode: Boolean, autoIcons: Boolean, hapticsEnabled: Boolean,
                           automaticUpdateCheck: Boolean, onAutomaticUpdateCheck: (Boolean) -> Unit,
                           onAppUpdate: () -> Unit,
                           onForgetPc: () -> Unit,
                           onTheme: (AppThemeMode) -> Unit, onOnlyButtonMode: (Boolean) -> Unit,
                           onAutoIcons: (Boolean) -> Unit, onHaptics: (Boolean) -> Unit,
                           onConnection: () -> Unit,
                           onExport: () -> Unit, onImport: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Forest)
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White),
            onClick = onConnection) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("PC connection", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text("${state.label()} · ${config.host.ifBlank { "No PC configured" }}", color = Muted, modifier = Modifier.padding(top = 6.dp))
                if (connectionStatus.isNotBlank()) {
                    Text(connectionStatus, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
                        fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
                Text(
                    if (usbNetworkAvailable) "USB network route detected"
                    else "USB network route not detected. A USB cable alone does not provide a network connection.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f),
                    fontSize = 11.sp,
                )
                if (message.isNotBlank()) Text(message, color = Forest, modifier = Modifier.padding(top = 6.dp))
                if (token.isNotBlank()) Text("Pairing token is stored securely on this device.", color = Muted)
                if (config.host.isNotBlank() || token.isNotBlank()) {
                    TextButton(onClick = onForgetPc) { Text("Forget PC") }
                }
            }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Appearance / Layout", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        val selected = themeMode == mode
                        Surface(onClick = { onTheme(mode) }, shape = RoundedCornerShape(12.dp),
                            color = if (selected) Forest else Cream) {
                            Text(mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = if (selected) Color.White else Forest, fontSize = 12.sp)
                        }
                    }
                }
                LabeledSwitch("Only Button Mode", onlyButtonMode, onOnlyButtonMode)
                LabeledSwitch("Auto Icon Detection", autoIcons, onAutoIcons)
                LabeledSwitch("Haptic feedback", hapticsEnabled, onHaptics)
            }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("App Update", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text("Check for new versions and view release notes.", color = Muted, fontSize = 13.sp)
                LabeledSwitch("Automatic update check", automaticUpdateCheck, onAutomaticUpdateCheck)
                Button(onClick = onAppUpdate, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Update Center")
                }
            }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), onClick = onExport) {
            Text("Export profiles and buttons", Modifier.padding(18.dp), fontWeight = FontWeight.Medium)
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), onClick = onImport) {
            Text("Import a JSON backup (adds profiles)", Modifier.padding(18.dp), fontWeight = FontWeight.Medium)
        }
        Text("Theme follows system appearance · Haptics enabled · Android 10+", color = Muted)
    }
}

@Composable
private fun UpdateReminderBanner(
    version: String,
    mandatory: Boolean,
    onView: () -> Unit,
    onRemindLater: (UpdateReminderDelay) -> Unit,
    onIgnore: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .25f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    if (mandatory) "Required Stream Deck update" else "Stream Deck update available",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Text("Version $version is ready", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
            }
            Button(onClick = onView, shape = RoundedCornerShape(10.dp)) {
                Text(if (mandatory) "Update now" else "View update")
            }
            if (!mandatory) {
                TextButton(onClick = { onRemindLater(UpdateReminderDelay.TOMORROW) }) { Text("Later") }
                UpdateReminderOptions(onRemindLater, onIgnore)
            }
        }
    }
}

@Composable
private fun UpdateReminderOptions(
    onRemindLater: (UpdateReminderDelay) -> Unit,
    onIgnore: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Update reminder options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            UpdateReminderDelay.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = { expanded = false; onRemindLater(option) },
                )
            }
            DropdownMenuItem(
                text = { Text("Don't remind for this version") },
                onClick = { expanded = false; onIgnore() },
            )
        }
    }
}

@Composable
private fun UpdateCenterScreen(
    state: UpdateUiState,
    updateSourceConfigured: Boolean,
    automaticUpdateCheck: Boolean,
    onAutomaticUpdateCheck: (Boolean) -> Unit,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstallerVerificationFailed: () -> Unit,
    onRemindLater: (UpdateReminderDelay) -> Unit,
    onIgnore: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var installMessage by remember(state.downloadedApkPath) { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.Home, contentDescription = "Back to settings") }
            Text("App Update", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .22f)),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Installed version", Modifier.padding(start = 9.dp), fontWeight = FontWeight.SemiBold)
                        }
                        Text("Version ${state.installed.versionName}", fontSize = 14.sp)
                        Text("Build ${state.installed.versionCode}", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Update status", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
                            Spacer(Modifier.width(8.dp))
                            when {
                                state.isChecking -> Text("Checking for updates…", fontSize = 12.sp)
                                state.isDownloading -> Text("Downloading update", fontSize = 12.sp)
                                state.update?.isAvailable == true -> Text(
                                    if (state.update.mandatory) "Required update available" else "Update available",
                                    fontSize = 12.sp,
                                )
                                state.update != null -> {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                                        modifier = Modifier.size(16.dp), tint = Color(0xFF5D9872))
                                    Text(" You're up to date", fontSize = 12.sp)
                                }
                                !updateSourceConfigured -> Text("Update source not configured", fontSize = 12.sp)
                                else -> Text("Not checked yet", fontSize = 12.sp)
                            }
                        }
                        if (!updateSourceConfigured) {
                            Text(
                                "Set GITHUB_USERNAME and GITHUB_REPOSITORY in UpdateConfig.kt to enable checks.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f),
                            )
                        }
                        Button(onClick = onCheck, enabled = !state.isChecking && !state.isDownloading,
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            if (state.isChecking) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(17.dp), strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (state.isChecking) "Checking…" else "Check for Updates")
                        }
                    }
                }
            }

            state.update?.let { info ->
                if (info.isAvailable) {
                    item {
                        Card(shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .22f))) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary)
                                    Column(Modifier.weight(1f).padding(start = 9.dp)) {
                                        Text("Version ${info.latestVersion}", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                                        Text("Build ${info.latestVersionCode}", fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
                                    }
                                }
                                if (info.sizeBytes != null) {
                                    Text("Download size: ${formatFileSize(info.sizeBytes)}", fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f))
                                }
                                if (info.mandatory) {
                                    Text(
                                        "This release is marked as required. Android will still ask you to confirm installation.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                Text("What's New", fontWeight = FontWeight.SemiBold)
                                if (info.releaseNotes.isEmpty()) {
                                    Text("Release notes are not available.", fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = .68f))
                                } else {
                                    info.releaseNotes.forEach { note ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text("•", modifier = Modifier.padding(end = 8.dp), color = MaterialTheme.colorScheme.primary)
                                            Text(note, fontSize = 13.sp)
                                        }
                                    }
                                }
                                Text(
                                    if (info.sha256 != null) "APK package, version and SHA-256 will be verified before installation."
                                    else "SHA-256 checksum is not configured. Package, version and signing certificate are still verified.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f),
                                )
                                if (state.isDownloading) {
                                    val percent = state.downloadPercent
                                    if (percent == null) {
                                        LinearProgressIndicator(Modifier.fillMaxWidth())
                                    } else {
                                        LinearProgressIndicator(
                                            progress = { percent / 100f },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                    Text(
                                        buildString {
                                            append(formatFileSize(state.downloadBytes))
                                            state.downloadTotalBytes?.let {
                                                append(" / ")
                                                append(formatFileSize(it))
                                            }
                                            if (percent != null) {
                                                append(" · ")
                                                append(percent)
                                                append('%')
                                            }
                                        },
                                        fontSize = 12.sp,
                                    )
                                } else if (state.downloadedApkPath != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                                            tint = Color(0xFF5D9872), modifier = Modifier.size(19.dp))
                                        Text("Update downloaded", Modifier.padding(start = 7.dp), fontWeight = FontWeight.Medium)
                                    }
                                    if (installMessage.isNotBlank()) {
                                        Text(installMessage, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            runCatching {
                                                UpdateInstaller(context).install(
                                                    File(state.downloadedApkPath),
                                                    info.latestVersionCode,
                                                    info.sha256,
                                                )
                                            }.onSuccess { result ->
                                                installMessage = when (result) {
                                                    InstallResult.STARTED -> "Android's package installer was opened."
                                                    InstallResult.PERMISSION_REQUIRED ->
                                                        "Allow installs from Stream Deck in Android settings, then tap Install Update again."
                                                }
                                            }.onFailure {
                                                android.util.Log.e("StreamDeckUpdate", "Unable to start APK installer", it)
                                                if (it is UpdateVerificationException) {
                                                    onInstallerVerificationFailed()
                                                    installMessage = "Update verification failed. Download it again before installing."
                                                } else {
                                                    installMessage = "Unable to open the installer. Download the update again and retry."
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Install Update")
                                    }
                                } else {
                                    Button(onClick = onDownload, enabled = !state.isDownloading && !state.isChecking,
                                        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Update Now")
                                    }
                                }
                                if (!info.mandatory) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { onRemindLater(UpdateReminderDelay.TOMORROW) }) {
                                            Text("Remind me tomorrow")
                                        }
                                        Spacer(Modifier.weight(1f))
                                        TextButton(onClick = onIgnore) { Text("Don't remind for this version") }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF5D9872))
                                Text("You're up to date", Modifier.padding(start = 9.dp), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            if (state.error != null) {
                item {
                    Card(shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(state.error, Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                            if (state.update?.isAvailable == true && (state.downloadFailed || state.verificationFailed)) {
                                TextButton(onClick = onDownload) { Text("Retry download") }
                            } else {
                                TextButton(onClick = onCheck) { Text("Retry check") }
                            }
                        }
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Update reminders", fontWeight = FontWeight.SemiBold)
                        LabeledSwitch("Automatic update check", automaticUpdateCheck, onAutomaticUpdateCheck)
                        Text("Checks at most once every 24 hours. Update checks require an internet connection.",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kib = bytes / 1024.0
    if (kib < 1024.0) return String.format(java.util.Locale.ROOT, "%.1f KB", kib)
    return String.format(java.util.Locale.ROOT, "%.1f MB", kib / 1024.0)
}

@Composable
private fun ConnectionDialog(config: ConnectionConfig, pairingToken: String, discovered: List<DiscoveredPc>,
                             onDismiss: () -> Unit, onForgetPc: () -> Unit,
                             onSave: (ConnectionConfig) -> Unit) {
    var host by rememberSaveable(config.host) { mutableStateOf(config.host) }
    var port by rememberSaveable(config.port) { mutableStateOf(config.port) }
    var token by rememberSaveable(config.token) { mutableStateOf(config.token.ifBlank { pairingToken }) }
    var companionId by rememberSaveable(config.companionId) { mutableStateOf(config.companionId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connect to your PC") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the Windows companion's local IP address. Pairing is confirmed on the PC.", color = Muted)
                if (discovered.isNotEmpty()) {
                    Text("Nearby PCs", fontWeight = FontWeight.SemiBold)
                    discovered.forEach { pc ->
                        TextButton(onClick = {
                            host = pc.host
                            port = pc.port.toString()
                            companionId = pc.companionId
                        }) {
                            Text("${pc.name} · ${pc.host}:${pc.port}")
                        }
                    }
                }
                OutlinedTextField(host, {
                    host = it
                    if (it != config.host) companionId = ""
                }, label = { Text("PC IP address or hostname") }, singleLine = true)
                OutlinedTextField(port, { port = it }, label = { Text("Port") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(token, { token = it }, label = { Text("Device token (leave blank to pair)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(ConnectionConfig(host, port, token, companionId)) }) { Text("Connect") }
        },
        dismissButton = {
            Row {
                if (config.host.isNotBlank() || token.isNotBlank()) {
                    TextButton(onClick = onForgetPc) { Text("Forget PC") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun TextEntryDialog(
    title: String,
    hint: String,
    initialValue: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable(title, initialValue) { mutableStateOf(initialValue) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, label = { Text(hint) }, singleLine = true) },
        confirmButton = { Button(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text(if (title.startsWith("Rename")) "Save" else "Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
