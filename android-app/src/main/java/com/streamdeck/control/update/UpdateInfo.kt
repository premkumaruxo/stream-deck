package com.streamdeck.control.update

data class InstalledAppVersion(
    val versionName: String,
    val versionCode: Long,
    val packageName: String,
)

data class UpdateInfo(
    val installed: InstalledAppVersion,
    val latestVersion: String,
    val latestVersionCode: Long,
    val downloadUrl: String,
    val releaseNotes: List<String>,
    val sizeBytes: Long?,
    val sha256: String?,
    val mandatory: Boolean = false,
) {
    val isAvailable: Boolean
        get() = latestVersionCode > installed.versionCode
}

data class UpdateUiState(
    val installed: InstalledAppVersion,
    val update: UpdateInfo? = null,
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadBytes: Long = 0L,
    val downloadTotalBytes: Long? = null,
    val downloadedApkPath: String? = null,
    val error: String? = null,
    val verificationFailed: Boolean = false,
    val downloadFailed: Boolean = false,
    val showReminder: Boolean = false,
) {
    val downloadPercent: Int?
        get() = downloadTotalBytes?.takeIf { it > 0 }?.let {
            ((downloadBytes * 100L) / it).toInt().coerceIn(0, 100)
        }
}

enum class InstallResult {
    STARTED,
    PERMISSION_REQUIRED,
}

enum class UpdateReminderDelay(val label: String) {
    LATER_TODAY("Later today"),
    TOMORROW("Tomorrow"),
    IN_THREE_DAYS("In 3 days"),
}

class UpdateVerificationException(message: String) : Exception(message)

class UpdateSourceNotConfiguredException :
    Exception("Set GITHUB_USERNAME and GITHUB_REPOSITORY in UpdateConfig before checking for updates")
