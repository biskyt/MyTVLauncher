package com.biskyt.mytvlauncher.data

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.biskyt.mytvlauncher.LauncherApp
import com.biskyt.mytvlauncher.openWatchNext
import com.biskyt.mytvlauncher.testutil.FakeWatchNext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class WatchNextSafetyTest {
    private val app = ApplicationProvider.getApplicationContext<LauncherApp>()

    @Test
    fun `an item's intent for another app is not started`() {
        val other = Intent(Intent.ACTION_VIEW).setClassName("com.example.other", "com.example.other.Secret")
        val item = FakeWatchNext.film.copy(intentUri = other.toUri(Intent.URI_INTENT_SCHEME))
        app.openWatchNext(item)
        val started = shadowOf(app).nextStartedActivity
        assertNotEquals("com.example.other", started?.component?.packageName)
    }

    @Test
    fun `an item's own intent goes to its app only, with no access granted`() {
        val own = Intent(Intent.ACTION_VIEW).setData(android.net.Uri.parse("content://x/1")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val item = FakeWatchNext.film.copy(intentUri = own.toUri(Intent.URI_INTENT_SCHEME))
        app.openWatchNext(item)
        val started = shadowOf(app).nextStartedActivity
        assertEquals(item.packageName, started.`package`)
        assertEquals(0, started.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    @Test
    fun `two items with one key are saved as one`() = runBlocking {
        FakeWatchNext.items = listOf(FakeWatchNext.film, FakeWatchNext.film.copy(title = "Again"))
        app.container.watchNext.read()
        assertEquals(1, app.container.db.watchNext().all().size)
    }
}
