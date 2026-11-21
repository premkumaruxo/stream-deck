package com.streamdeck.control.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.streamdeck.control.ui.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

class UpdateManager(
    private val context: Context,
    private val preferences: AppPreferences,
    private val repository: UpdateRepository = UpdateRepository(),
) {
    private val checkMutex = Mutex()
    private val installed = readInstalledVersion()
    private val apkDownloader = ApkDownloader(context)

    fun installedVersion(): InstalledAppVersion = installed

    suspend fun shouldCheckAutomatically(): Boolean {
        if (!UpdateConfig.isConfigured || !preferences.automaticUpdateCheck.first()) return false
        val lastCheck = maxOf(
            preferences.lastSuccessfulUpdateCheck(),
            preferences.lastUpdateCheckAttempt(),
        )
        return System.currentTimeMillis() - lastCheck >=
            UpdateConfig.CHECK_INTERVAL_MILLIS
    }

    suspend fun checkForUpdates(): UpdateInfo = checkMutex.withLock {
        if (!UpdateConfig.isConfigured) throw UpdateSourceNotConfiguredException()
        try {
            preferences.recordUpdateCheckAttempt(System.currentTimeMillis())
            val (remote, rawMetadata) = repository.fetchUpdate()
            val result = UpdateInfo(
                installed = installed,
                latestVersion = remote.latestVersion,
                latestVersionCode = remote.versionCode,
                downloadUrl = remote.downloadUrl,
                releaseNotes = remote.releaseNotes,
                sizeBytes = remote.sizeBytes,
                sha256 = remote.sha256,
                mandatory = remote.mandatory,
            )
            preferences.recordSuccessfulUpdateCheck(System.currentTimeMillis(), rawMetadata)
            result
        } catch (error: Exception) {
            Log.w(TAG, "Update check failed", error)
            throw error
        }
    }

    suspend fun cachedUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val cached = preferences.cachedUpdateMetadata() ?: return@withContext null
        runCatching {
            val remote = repository.parseMetadata(cached)
            UpdateInfo(
                installed = installed,
                latestVersion = remote.latestVersion,
                latestVersionCode = remote.versionCode,
                downloadUrl = remote.downloadUrl,
                releaseNotes = remote.releaseNotes,
                sizeBytes = remote.sizeBytes,
                sha256 = remote.sha256,
                mandatory = remote.mandatory,
            )
        }.onFailure { Log.w(TAG, "Cached update metadata was invalid", it) }.getOrNull()
    }

    suspend fun reminderAllowed(version: String, mandatory: Boolean): Boolean {
        if (mandatory) return true
        if (version.isBlank() || preferences.ignoredUpdateVersion() == version) return false
        return preferences.updateReminderSnoozeUntil() <= System.currentTimeMillis()
    }

    suspend fun download(
        info: UpdateInfo,
        onProgress: (Long, Long?) -> Unit,
    ): java.io.File = apkDownloader.download(info, installed, onProgress)

    suspend fun findDownloadedApk(info: UpdateInfo): java.io.File? =
        apkDownloader.findDownloadedApk(info)

    suspend fun cleanupAfterInstall() =
        apkDownloader.cleanupAfterInstall(installed.versionCode)

    private fun readInstalledVersion(): InstalledAppVersion {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return InstalledAppVersion(
            versionName = packageInfo.versionName ?: "Unknown",
            versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
            packageName = context.packageName,
        )
    }

    companion object {
        private const val TAG = "StreamDeckUpdate"
    }
}
