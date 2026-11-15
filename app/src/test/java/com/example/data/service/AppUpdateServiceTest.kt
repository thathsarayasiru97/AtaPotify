package com.example.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateServiceTest {

    @Test
    fun `isRemoteVersionNewer detects major minor and patch increments`() {
        assertTrue(AppUpdateService.isRemoteVersionNewer("v1.0.1", "1.0"))
        assertTrue(AppUpdateService.isRemoteVersionNewer("v1.1.0", "1.0.9"))
        assertTrue(AppUpdateService.isRemoteVersionNewer("v1.2", "1.1"))
        assertTrue(AppUpdateService.isRemoteVersionNewer("v1.3.5", "1.3"))
        assertTrue(AppUpdateService.isRemoteVersionNewer("2.0", "1.9.9"))
        assertTrue(AppUpdateService.isRemoteVersionNewer("V1.2.0", "1.1"))
    }

    @Test
    fun `isRemoteVersionNewer ignores equal or older releases`() {
        assertFalse(AppUpdateService.isRemoteVersionNewer("v1.0", "1.0"))
        assertFalse(AppUpdateService.isRemoteVersionNewer("v1.2", "1.2"))
        assertFalse(AppUpdateService.isRemoteVersionNewer("v1.0.0", "1.0"))
        assertFalse(AppUpdateService.isRemoteVersionNewer("v0.9.9", "1.0"))
        assertFalse(AppUpdateService.isRemoteVersionNewer("1.2.3", "1.2.4"))
    }

    @Test
    fun `isRemoteVersionNewer prefers stable release over matching prerelease`() {
        assertTrue(AppUpdateService.isRemoteVersionNewer("v1.2.0", "1.2.0-beta1"))
        assertFalse(AppUpdateService.isRemoteVersionNewer("v1.2.0-rc1", "1.2.0"))
    }

    @Test
    fun `formatApkSize formats megabytes and kilobytes cleanly`() {
        assertEquals("", AppUpdateService.formatApkSize(0L))
        assertEquals("512 KB", AppUpdateService.formatApkSize(512L * 1024L))
        assertEquals("14.5 MB", AppUpdateService.formatApkSize((14.5 * 1024.0 * 1024.0).toLong()))
    }
}
