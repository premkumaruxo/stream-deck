package com.streamdeck.control.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateInfoTest {
    @Test
    fun higherVersionCodeIsAvailable() {
        assertTrue(updateInfo(installedCode = 1, remoteCode = 2).isAvailable)
    }

    @Test
    fun equalVersionCodeIsNotAvailable() {
        assertFalse(updateInfo(installedCode = 2, remoteCode = 2).isAvailable)
    }

    @Test
    fun lowerVersionCodeIsNotAvailable() {
        assertFalse(updateInfo(installedCode = 2, remoteCode = 1).isAvailable)
    }

    private fun updateInfo(installedCode: Long, remoteCode: Long) = UpdateInfo(
        installed = InstalledAppVersion("1.0.9", installedCode, "com.streamdeck.control"),
        latestVersion = "1.0.10",
        latestVersionCode = remoteCode,
        downloadUrl = "https://github.com/example/repo/releases/download/v1.0.10/app.apk",
        releaseNotes = emptyList(),
        sizeBytes = null,
        sha256 = null,
    )
}
