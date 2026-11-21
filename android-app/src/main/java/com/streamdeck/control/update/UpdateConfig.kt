package com.streamdeck.control.update

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object UpdateConfig {
    // Replace both placeholders before publishing; no endpoint is enabled until configured.
    const val GITHUB_USERNAME = "YOUR_GITHUB_USERNAME"
    const val GITHUB_REPOSITORY = "YOUR_GITHUB_REPOSITORY"
    const val RELEASE_BRANCH = "main"
    const val METADATA_URL_TEMPLATE =
        "https://raw.githubusercontent.com/GITHUB_USERNAME/GITHUB_REPOSITORY/main/latest.json"
    const val APK_ASSET_NAME = "StreamDeck-Android.apk"
    const val CHECK_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L

    val isConfigured: Boolean
        get() = GITHUB_USERNAME.isNotBlank() && GITHUB_REPOSITORY.isNotBlank() &&
            !GITHUB_USERNAME.startsWith("YOUR_") && !GITHUB_REPOSITORY.startsWith("YOUR_")

    val metadataUrl: String?
        get() = if (isConfigured) {
            "https://raw.githubusercontent.com/$GITHUB_USERNAME/$GITHUB_REPOSITORY/$RELEASE_BRANCH/latest.json"
        } else {
            null
        }

    fun requireGitHubReleaseUrl(url: String) {
        require(isConfigured) {
            "Configure GITHUB_USERNAME and GITHUB_REPOSITORY before checking for updates"
        }
        val parsed = url.toHttpUrlOrNull()
        require(parsed != null && parsed.isHttps && parsed.username.isEmpty() && parsed.password.isEmpty()) {
            "APK download URL must use HTTPS"
        }
        val path = parsed.pathSegments
        require(
            parsed.host == "github.com" &&
                path.size == 6 &&
                path[0].equals(GITHUB_USERNAME, ignoreCase = true) &&
                path[1].equals(GITHUB_REPOSITORY, ignoreCase = true) &&
                path[2] == "releases" &&
                path[3] == "download" &&
                path[5] == APK_ASSET_NAME
        ) {
            "APK URL must point to $APK_ASSET_NAME in this GitHub repository's Releases"
        }
    }

    val metadataRedirectHosts: Set<String> = setOf("raw.githubusercontent.com")
    val releaseRedirectHosts: Set<String> = setOf(
        "github.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
    )
}
