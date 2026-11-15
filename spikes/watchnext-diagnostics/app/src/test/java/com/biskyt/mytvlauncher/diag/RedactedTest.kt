package com.biskyt.mytvlauncher.diag

import org.junit.Assert.assertEquals
import org.junit.Test

class RedactedTest {
    @Test
    fun `query values are hidden, names and the rest stay`() {
        assertEquals(
            "http://192.168.1.5:32400/photo/:/transcode?width=…&X-Plex-Token=…",
            redacted("http://192.168.1.5:32400/photo/:/transcode?width=300&X-Plex-Token=abc123")
        )
    }

    @Test
    fun `a user name and password are hidden, and addresses inside a message too`() {
        assertEquals("http://…@host/p", redacted("http://user:secret@host/p"))
        assertEquals(
            "FileNotFoundException: http://h:32400/a?X-Plex-Token=… failed",
            redactedText("FileNotFoundException: http://h:32400/a?X-Plex-Token=abc failed")
        )
    }

    @Test
    fun `an address with no query stays as it is, and a fragment is dropped`() {
        assertEquals("content://com.example.art/art/1.png", redacted("content://com.example.art/art/1.png"))
        assertEquals("https://a.b/c?t=…", redacted("https://a.b/c?t=secret#frag"))
    }
}
