package com.biskyt.mytvlauncher.core.speaker

import org.junit.Assert.assertEquals
import org.junit.Test

class WakeDelayTest {
    @Test
    fun `OK steps through the delays and back to the first`() {
        assertEquals(3, WakeDelay.next(2))
        assertEquals(10, WakeDelay.next(5))
        assertEquals(0, WakeDelay.next(10))
        // A value saved that is not a choice goes to the next choice.
        assertEquals(5, WakeDelay.next(4))
    }
}
