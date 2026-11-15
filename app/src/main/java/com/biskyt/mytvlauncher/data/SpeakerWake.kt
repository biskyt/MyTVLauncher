package com.biskyt.mytvlauncher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The TV's sound output: whether something plays now, and the silent sound. A fake one in tests. */
interface SpeakerOutput {
    fun playing(): Boolean

    /** Plays about a second of silence, and returns when it ends. */
    suspend fun playSilence()
}

/**
 * Wakes a speaker that sleeps while the TV is in standby (the owner, 2026-11-14, low priority): when the TV wakes, the
 * launcher waits for the delay set in Settings, then plays about a second of silence, so that the speaker on HDMI or
 * ARC sees sound and wakes. Off by default. Once for each wake: a new wake cancels a wait that is still going. Nothing
 * plays while other sound plays already. The launcher runs while the TV is in standby, so it gets the wake. If the TV
 * stopped the launcher during standby, no receiver runs, and that wake plays nothing.
 */
class SpeakerWake(private val settings: SettingsRepository, private val output: SpeakerOutput, private val scope: CoroutineScope) {
    private var waking: Job? = null
    private var trying: Job? = null

    /** Follows the TV's wakes (ACTION_SCREEN_ON is sent only to receivers registered while the app runs). */
    fun start(context: Context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = woke()
        }
        // Exported: only the system can send this protected broadcast. "Not exported" needs a permission of its own before
        // Android 13.
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_SCREEN_ON), ContextCompat.RECEIVER_EXPORTED)
    }

    /**
     * The TV woke: the sound after the delay, if the setting is on and nothing plays. The setting is read again after
     * the delay, so that turning it off meanwhile stops the sound.
     */
    @Synchronized
    fun woke() {
        waking?.cancel()
        waking = scope.launch {
            if (!settings.speakerWake.first()) return@launch
            delay(settings.speakerWakeDelay.first().seconds)
            if (settings.speakerWake.first() && !output.playing()) output.playSilence()
        }
    }

    /** Settings › Speaker wake › Play it now: the sound at once, to try it. A press again starts it again. */
    @Synchronized
    fun playNow() {
        trying?.cancel()
        trying = scope.launch { output.playSilence() }
    }

    /** Waits until the wake and the try that go now end: for tests. */
    internal suspend fun idle() {
        waking?.join()
        trying?.join()
    }
}

/** The real output: Android's audio, with a second of silent 16-bit stereo sound for media. */
class AudioSpeaker(context: Context) : SpeakerOutput {
    private val audio = context.getSystemService(AudioManager::class.java)

    /**
     * Whether media plays now. Not Android's list of players: it also holds paused and idle ones (a TV service can
     * always have one), and their state is not public.
     */
    override fun playing(): Boolean = audio?.isMusicActive == true

    /** A TV that cannot open a sound output now (just after standby) logs it: the launcher must not stop for it. */
    @Suppress("TooGenericExceptionCaught")
    override suspend fun playSilence(): Unit = withContext(Dispatchers.IO) {
        try {
            play()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("MyTVLauncher", "The speaker wake sound did not play", e)
        }
    }

    private suspend fun play() {
        val bytes = RATE * BYTES_PER_FRAME
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setEncoding(
                    AudioFormat.ENCODING_PCM_16BIT
                ).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(bytes)
            .build()
        try {
            track.write(ByteArray(bytes), 0, bytes)
            track.play()
            delay(PLAY_MS)
        } finally {
            track.release()
        }
    }

    private companion object {
        const val RATE = 48_000
        const val BYTES_PER_FRAME = 4
        const val PLAY_MS = 1_100L
    }
}
