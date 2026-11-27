package com.biskyt.mytvlauncher.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The launcher reads the TV provider only where the TV has Watch Next and app channels (Android 8 and later). */
@RunWith(AndroidJUnit4::class)
class TvRowSourcesTest {
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    @Config(sdk = [25])
    fun `Android 7 gets no provider, so no class from Android 8 is loaded`() {
        assertSame(NoTvRows, watchNextSourceFor(context))
        assertSame(NoTvRows, channelSourceFor(context))
    }

    @Test
    fun `Android 8 and later read the TV provider`() {
        assertTrue(watchNextSourceFor(context) is ProviderWatchNext)
        assertTrue(channelSourceFor(context) is ProviderChannels)
    }
}
