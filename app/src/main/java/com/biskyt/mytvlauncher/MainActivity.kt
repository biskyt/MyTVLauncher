package com.biskyt.mytvlauncher

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.input.key.Key
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import com.biskyt.mytvlauncher.core.keys.HomePressLog
import com.biskyt.mytvlauncher.data.GuideApp
import com.biskyt.mytvlauncher.ui.AppRoot
import com.biskyt.mytvlauncher.ui.Navigator
import com.biskyt.mytvlauncher.ui.common.hasTouchScreen
import com.biskyt.mytvlauncher.ui.common.isActing

/** The home screen. It is the TV's home app when chosen as one (see RELEASING.md). */
class MainActivity : ComponentActivity() {
    private val nav = Navigator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0) takeOpenedFile(intent)
        setContent { AppRoot(nav) }
    }

    // Each time Home comes to the front: a look at the installed apps in the background, after Home has drawn from the
    // saved list. It catches a change that sent no callback (see AppRepository.watch). Watch Next is read again if an app
    // changed its items while Home was not in front.
    override fun onStart() {
        super.onStart()
        container.apps.refresh()
        container.watchNext.cameToFront()
        // MyTVPlayer brings its rows up to date (from the user's other TVs): see HomeRowsNudge.
        container.homeRowsNudge.nudge()
        container.channels.cameToFront()
        if (container.notificationsShown.value == true) container.checkNotificationAccess()
    }

    override fun onStop() {
        container.watchNext.wentToBack()
        container.channels.wentToBack()
        super.onStop()
    }

    /** OK, Options and Guide presses that began in this window (see [dispatchKeyEvent]). */
    private val pressedHere = mutableSetOf<Int>()

    /**
     * As MyPlexPlayer: the rest of an OK, Options or Guide press that began in another window is kept from the TV and
     * from this window. A panel (a window of its own) acted on that press and closed; the TV would act on its release,
     * and this window on its repeats.
     */
    // (Activity's own, public method: lint takes androidx's ComponentActivity override of it for a restricted one.)
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // The remote's Guide key while the launcher is in front: the guide that Settings › TV guide chose. With the TV's
        // own guide (the default), the key goes on as before, for the TV to handle in its own way.
        if (event.keyCode == KeyEvent.KEYCODE_GUIDE && container.guideApp.value != GuideApp.SYSTEM) {
            if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) openGuide()
            return true
        }
        if (Key(event.keyCode).isActing()) {
            when {
                event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 -> pressedHere += event.keyCode
                event.action == KeyEvent.ACTION_DOWN -> if (event.keyCode !in pressedHere) return true
                event.action == KeyEvent.ACTION_UP -> if (!pressedHere.remove(event.keyCode)) return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Another window (a panel) has the keys now: a press begun here ends there, so it's no longer this window's.
        if (!hasFocus) pressedHere.clear()
        if (hasFocus && hasTouchScreen()) window.hideSystemBars()
    }

    // Home pressed while the launcher is running. Only a short press gets here: Android keeps the Home key itself.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeIntent(intent)
    }

    @VisibleForTesting
    internal fun takeIntent(intent: Intent) {
        takeOpenedFile(intent)
        if (!intent.hasCategory(Intent.CATEGORY_HOME)) return
        container.homePressed(inFront = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        // Two presses within half a second: the recent apps (the owner's test: a double press reaches the launcher).
        if (HomePressLog.isDouble(container.homePresses.value)) nav.recents() else nav.home()
    }
}

/** A backup file opened with MyTVLauncher from another app (Open with, or Share), for Backup & restore to offer. */
private fun MainActivity.takeOpenedFile(intent: Intent) {
    intent.openedFile()?.let { container.openedFile.value = it }
}

/** The file that a file manager opened with MyTVLauncher (as MyPlexPlayer's). */
fun Intent.openedFile(): Uri? = when (action) {
    Intent.ACTION_VIEW -> data
    Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
    else -> null
}

/**
 * Full screen on a phone or tablet: otherwise the status bar lies over the top of the app and takes its taps. A swipe
 * from the edge brings the bars back for a moment.
 */
fun Window.hideSystemBars() = WindowCompat.getInsetsController(this, decorView).run {
    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    hide(WindowInsetsCompat.Type.systemBars())
}
