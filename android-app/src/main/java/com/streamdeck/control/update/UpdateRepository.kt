package com.streamdeck.control.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class RemoteUpdate(
    val latestVersion: String,
    val versionCode: Long,
    val downloadUrl: String,
    val releaseNotes: List<String>,
    val sizeBytes: Long?,
    val sha256: String?,
    val mandatory: Boolean,
)

class UpdateRepository(
    private val metadataUrl: String? = UpdateConfig.metadataUrl,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun fetchUpdate(): Pair<RemoteUpdate, String> = withContext(Dispatchers.IO) {
        val source = metadataUrl ?: throw UpdateSourceNotConfiguredException()
        requireHttps(source, "Update metadata URL")
        val request = Request.Builder().url(source).header("Accept", "application/json").build()
        HttpsRedirects.execute(client, request, UpdateConfig.metadataRedirectHosts).use { response ->
            check(response.isSuccessful) { "Update server returned HTTP ${response.code}" }
            check(response.request.url.isHttps) { "Update metadata redirected to an insecure URL" }
            val body = response.body ?: error("Update metadata response was empty")
            val bytes = body.byteStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_METADATA_BYTES) { "Update metadata was too large" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val json = String(bytes, Charsets.UTF_8)
            parseMetadata(json) to json
        }
    }

    fun parseMetadata(json: String): RemoteUpdate {
        val root = JSONObject(json)
        val data = root.optJSONObject("android") ?: root
        val latestVersion = data.optString("version").ifBlank {
            data.optString("latestVersion")
        }.trim()
        require(latestVersion.isNotEmpty()) { "Update metadata has no version name" }
        val versionCode = if (data.has("versionCode")) {
            data.optLong("versionCode", -1L)
        } else {
            data.optLong("latestVersionCode", -1L)
        }
        require(versionCode > 0) { "Update metadata has an invalid version code" }
        val downloadUrl = data.optString("downloadUrl").trim()
        UpdateConfig.requireGitHubReleaseUrl(downloadUrl)
        val notesJson = data.optJSONArray("releaseNotes") ?: JSONArray()
        val releaseNotes = buildList {
            for (index in 0 until notesJson.length()) {
                val note = notesJson.optString(index).trim()
                if (note.isNotEmpty()) add(note.take(MAX_NOTE_LENGTH))
            }
        }.take(MAX_RELEASE_NOTES)
        val sizeBytes = if (data.has("sizeBytes") && !data.isNull("sizeBytes")) {
            data.optLong("sizeBytes", -1L).also { require(it > 0L) { "Update metadata has an invalid file size" } }
        } else null
        val sha256 = if (data.has("sha256") && !data.isNull("sha256")) {
            data.optString("sha256").trim().takeIf { it.isNotEmpty() }
        } else null
        sha256?.also {
            require(SHA256_REGEX.matches(it)) { "Update metadata has an invalid SHA-256 checksum" }
        }
        return RemoteUpdate(
            latestVersion,
            versionCode,
            downloadUrl,
            releaseNotes,
            sizeBytes,
            sha256,
            mandatory = data.optBoolean("mandatory", false),
        )
    }

    private fun requireHttps(url: String, description: String) {
        val parsed = url.toHttpUrlOrNull()
        require(parsed != null && parsed.isHttps && parsed.username.isEmpty() && parsed.password.isEmpty()) {
            "$description must use HTTPS"
        }
    }

    private companion object {
        const val MAX_METADATA_BYTES = 512 * 1024
        const val MAX_NOTE_LENGTH = 500
        const val MAX_RELEASE_NOTES = 30
        val SHA256_REGEX = Regex("(?i)[0-9a-f]{64}")
    }
}
