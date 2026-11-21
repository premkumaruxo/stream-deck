package com.streamdeck.control.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

class UpdateInstaller(private val context: Context) {
    fun install(apk: File, expectedVersionCode: Long, expectedSha256: String?): InstallResult {
        try {
            ApkVerifier.verify(
                context,
                apk,
                expectedVersionCode,
                expectedSha256,
                requireManagedCacheFile = true,
            )
        } catch (error: UpdateVerificationException) {
            if (!ApkVerifier.deleteManagedCacheFile(context, apk)) {
                throw IllegalStateException("Unable to safely remove the rejected update APK", error)
            }
            throw error
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            return InstallResult.PERMISSION_REQUIRED
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.updates",
            apk,
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
        return InstallResult.STARTED
    }

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
