package com.streamdeck.control.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ApkDownloader(private val context: Context) {
    private val downloadMutex = Mutex()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.MINUTES)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun download(
        info: UpdateInfo,
        installed: InstalledAppVersion,
        onProgress: (Long, Long?) -> Unit,
    ): File = downloadMutex.withLock {
        withContext(Dispatchers.IO) {
            val parsedUrl = info.downloadUrl.toHttpUrlOrNull()
            UpdateConfig.requireGitHubReleaseUrl(info.downloadUrl)
            require(parsedUrl != null) { "APK download URL is invalid" }
            require(installed.packageName == context.packageName) {
                "Update package does not match the installed app"
            }
            require(info.isAvailable) { "No newer app version is available" }

            val directory = File(context.cacheDir, UPDATE_CACHE_DIRECTORY)
            val cacheDirectory = context.cacheDir.canonicalFile
            check(
                (directory.isDirectory || directory.mkdirs()) &&
                    directory.canonicalFile.parentFile == cacheDirectory
            ) {
                "Unable to prepare the private update cache"
            }
            val target = File(directory, "streamdeck-update-${info.latestVersionCode}.apk")
            val partial = File(directory, "streamdeck-update-${info.latestVersionCode}.part")
            if (target.isFile) {
                try {
                    ApkVerifier.verify(context, target, info.latestVersionCode, info.sha256, requireManagedCacheFile = true)
                    return@withContext target
                } catch (error: UpdateVerificationException) {
                    Log.w(TAG, "Discarding an invalid cached update APK", error)
                    check(target.delete()) { "Unable to remove the invalid cached update APK" }
                }
            }
            check(!partial.exists() || partial.delete()) { "Unable to clear an incomplete APK download" }

            try {
                val request = Request.Builder().url(parsedUrl).header("Accept", "application/vnd.android.package-archive").build()
                HttpsRedirects.execute(client, request, UpdateConfig.releaseRedirectHosts).use { response ->
                    check(response.isSuccessful) { "APK server returned HTTP ${response.code}" }
                    check(response.request.url.isHttps) { "APK download redirected to an insecure URL" }
                    val body = response.body ?: error("APK download response was empty")
                    val responseLength = body.contentLength().takeIf { it > 0L }
                    if (responseLength != null && info.sizeBytes != null) {
                        require(responseLength == info.sizeBytes) { "APK size does not match update metadata" }
                    }
                    val total = responseLength ?: info.sizeBytes
                    require(total == null || total <= MAX_APK_BYTES) { "APK file exceeds the download size limit" }
                    body.byteStream().use { input ->
                        FileOutputStream(partial).use { output ->
                            val buffer = ByteArray(32 * 1024)
                            var downloaded = 0L
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                downloaded += count
                                require(downloaded <= MAX_APK_BYTES) { "APK file exceeds the download size limit" }
                                output.write(buffer, 0, count)
                                onProgress(downloaded, total)
                            }
                            output.fd.sync()
                            require(total == null || downloaded == total) { "APK download was incomplete" }
                        }
                    }
                }

                ApkVerifier.verify(context, partial, info.latestVersionCode, info.sha256)
                if (target.exists()) check(target.delete()) { "Unable to replace the previous update file" }
                check(partial.renameTo(target)) { "Unable to finalize the downloaded update" }
                target
            } catch (error: Exception) {
                partial.delete()
                if (error is UpdateVerificationException) {
                    target.delete()
                    Log.w(TAG, "Downloaded APK verification failed", error)
                }
                throw error
            }
        }
    }

    suspend fun findDownloadedApk(info: UpdateInfo): File? = withContext(Dispatchers.IO) {
        val apk = File(context.cacheDir, "$UPDATE_CACHE_DIRECTORY/streamdeck-update-${info.latestVersionCode}.apk")
        if (!apk.isFile || !info.isAvailable) return@withContext null
        try {
            ApkVerifier.verify(context, apk, info.latestVersionCode, info.sha256, requireManagedCacheFile = true)
            apk
        } catch (error: UpdateVerificationException) {
            Log.w(TAG, "Discarding an invalid cached update APK", error)
            check(apk.delete()) { "Unable to remove the invalid cached update APK" }
            null
        }
    }

    suspend fun cleanupAfterInstall(installedVersionCode: Long) = withContext(Dispatchers.IO) {
        ApkVerifier.cleanupAfterInstall(context, installedVersionCode)
    }

    private companion object {
        const val TAG = "StreamDeckUpdate"
        const val UPDATE_CACHE_DIRECTORY = "updates"
        const val MAX_APK_BYTES = 250L * 1024L * 1024L
    }
}
