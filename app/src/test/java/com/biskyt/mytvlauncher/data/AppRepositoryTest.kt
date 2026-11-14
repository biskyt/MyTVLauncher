package com.biskyt.mytvlauncher.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.LauncherApp
import com.biskyt.mytvlauncher.core.apps.InstalledApp
import com.biskyt.mytvlauncher.testutil.FakeApps
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppRepositoryTest {
    private val container = ApplicationProvider.getApplicationContext<LauncherApp>().container

    @Test
    fun `a look saves the apps, a new app goes at the end, and a removed one goes`() = runBlocking {
        container.apps.look()
        assertEquals(listOf("Browser", "Music", "Settings", "Videos"), container.apps.shown.first().map { it.label })

        val radio = InstalledApp("com.example.radio", "Radio")
        FakeApps.apps = listOf(FakeApps.videos, radio, FakeApps.browser)
        container.apps.look()
        assertEquals(listOf("Browser", "Videos", "Radio"), container.apps.shown.first().map { it.label })
    }

    @Test
    fun `a look that finds no change writes nothing`() = runBlocking {
        container.apps.look()
        val before = container.db.appPlaces().all()
        container.apps.look()
        assertEquals(before, container.db.appPlaces().all())
    }
}
