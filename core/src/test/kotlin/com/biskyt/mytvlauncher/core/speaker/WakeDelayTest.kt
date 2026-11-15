package com.biskyt.mytvlauncher.core.speaker

import org.junit.Assert.assertEquals
import org.junit.Test

class WakeDelayTest {
    @Test
    fun `Left and Right step through the delays, round from the last to the first and back`() {
        assertEquals(3, WakeDelay.step(2, 1))
        assertEquals(1, WakeDelay.step(2, -1))
        assertEquals(0, WakeDelay.step(10, 1))
        assertEquals(10, WakeDelay.step(0, -1))
        // A value saved that is not a choice steps from the choice below it.
        assertEquals(5, WakeDelay.step(4, 1))
    }
}
