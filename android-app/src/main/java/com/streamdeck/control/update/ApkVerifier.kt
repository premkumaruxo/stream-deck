package com.streamdeck.control.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.security.MessageDigest
import java.util.Locale

internal object ApkVerifier {
    fun verify(
        context: Context,
        apk: File,
        expectedVersionCode: Long,
        expectedSha256: String?,
        requireManagedCacheFile: Boolean = false,
    ) {
        if (!apk.isFile || apk.length() < MIN_APK_SIZE) {
            throw UpdateVerificationException("Downloaded APK is missing or incomplete")
        }
        if (requireManagedCacheFile) requireManagedCacheFile(context, apk)
        if (expectedSha256 != null) verifyChecksum(apk, expectedSha256)

        val archiveInfo = packageArchiveInfo(context, apk)
            ?: throw UpdateVerificationException("Downloaded file is not a valid APK")
        val installedInfo = installedPackageInfo(context)
        if (archiveInfo.packageName != context.packageName) {
            throw UpdateVerificationException("Downloaded APK is for a different application")
        }
        val archiveVersionCode = PackageInfoCompat.getLongVersionCode(archiveInfo)
        val installedVersionCode = PackageInfoCompat.getLongVersionCode(installedInfo)
        if (archiveVersionCode != expectedVersionCode || archiveVersionCode <= installedVersionCode) {
            throw UpdateVerificationException("Downloaded APK version does not match the update")
        }
        if (signerDigests(archiveInfo) != signerDigests(installedInfo)) {
            throw UpdateVerificationException("Downloaded APK signing key does not match the installed app")
        }
    }

    fun cleanupAfterInstall(context: Context, installedVersionCode: Long) {
        val directory = File(context.cacheDir, UPDATE_CACHE_DIRECTORY)
        val files = directory.listFiles() ?: return
        files.forEach { file ->
            val versionCode = APK_FILE_PATTERN.matchEntire(file.name)
                ?.groupValues?.get(1)?.toLongOrNull()
            val shouldDelete = file.name.endsWith(".part") ||
                (versionCode != null && versionCode <= installedVersionCode)
            if (shouldDelete && !file.delete()) {
                Log.w(TAG, "Unable to remove a stale update cache file")
            }
        }
    }

    fun deleteManagedCacheFile(context: Context, apk: File): Boolean {
        val updateDirectory = File(context.cacheDir, UPDATE_CACHE_DIRECTORY).canonicalFile
        val canonicalApk = apk.canonicalFile
        if (canonicalApk.parentFile != updateDirectory || !APK_FILE_PATTERN.matches(canonicalApk.name)) {
            return false
        }
        return !canonicalApk.exists() || canonicalApk.delete()
    }

    private fun requireManagedCacheFile(context: Context, apk: File) {
        val updateDirectory = File(context.cacheDir, UPDATE_CACHE_DIRECTORY).canonicalFile
        val canonicalApk = apk.canonicalFile
        if (canonicalApk.parentFile != updateDirectory ||
            !APK_FILE_PATTERN.matches(canonicalApk.name)
        ) {
            throw UpdateVerificationException("Update APK is not in the private update cache")
        }
    }

    private fun verifyChecksum(apk: File, expectedSha256: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        apk.inputStream().buffered().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") {
            "%02x".format(Locale.ROOT, it.toInt() and 0xff)
        }
        if (!MessageDigest.isEqual(
                actual.toByteArray(Charsets.US_ASCII),
                expectedSha256.lowercase(Locale.ROOT).toByteArray(Charsets.US_ASCII),
            )
        ) {
            throw UpdateVerificationException("Update verification failed: SHA-256 checksum mismatch")
        }
    }

    private fun signerDigests(info: PackageInfo): Set<String> {
        val signingInfo = info.signingInfo
            ?: throw UpdateVerificationException("APK signing information is unavailable")
        val signers = signingInfo.apkContentsSigners
        if (signers.isEmpty()) {
            throw UpdateVerificationException("APK signing information is unavailable")
        }
        return signers.map { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
        }.toSet()
    }

    private fun installedPackageInfo(context: Context): PackageInfo {
        val flags = PackageManager.GET_SIGNING_CERTIFICATES.toLong()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(flags),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, flags.toInt())
        }
    }

    private fun packageArchiveInfo(context: Context, apk: File): PackageInfo? {
        val flags = PackageManager.GET_SIGNING_CERTIFICATES.toLong()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(
                apk.absolutePath,
                PackageManager.PackageInfoFlags.of(flags),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags.toInt())
        }
    }

    private const val TAG = "StreamDeckUpdate"
    private const val MIN_APK_SIZE = 4L
    private const val UPDATE_CACHE_DIRECTORY = "updates"
    private val APK_FILE_PATTERN = Regex("streamdeck-update-(\\d+)\\.apk")
}
