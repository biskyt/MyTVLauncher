package com.biskyt.mytvlauncher.ui.settings

import com.biskyt.mytvlauncher.core.ui.AccentPalette
import org.junit.Test

/** Every accent preset has a name in Settings: a preset added in core without one would crash the Accent screen. */
class AccentNameTest {
    @Test
    fun `every preset has a name`() {
        AccentPalette.presets.forEach { accentName(it.id) }
    }
}
