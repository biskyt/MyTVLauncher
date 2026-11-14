package com.biskyt.mytvlauncher.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AccentPaletteTest {
    @Test
    fun `the default is mint, with dark text on it`() {
        assertEquals(0xFF2BC4B0.toInt(), AccentPalette.DEFAULT)
        assertEquals("mint", AccentPalette.presets.first().id)
        assertEquals(0xFF0F1114.toInt(), AccentPalette.from(AccentPalette.DEFAULT).onAccent)
    }

    @Test
    fun `MyPlexPlayer's yellow is one of the choices`() {
        assertEquals(0xFFE5A00D.toInt(), AccentPalette.presets.single { it.id == "yellow" }.argb)
    }
}
