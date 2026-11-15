package com.biskyt.mytvlauncher.data

import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.biskyt.mytvlauncher.core.notifications.TvNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Where the system notifications come from: Android's notification listener, or a fake one in tests. */
interface NotificationSource {
    /** Every notification now, while the launcher follows them; empty otherwise. */
    val all: StateFlow<List<TvNotification>>

    /** Whether the user gave the launcher notification access. */
    fun allowed(): Boolean

    /** Follows the notifications ([on]) or stops (Settings › Show notifications). */
    fun follow(on: Boolean)

    /** Opens the notification (what OK on it does in the TV's own panel); false if it has nothing to open. */
    fun open(key: String): Boolean

    fun dismiss(key: String)
}

/**
 * The launcher's notification listener (the owner, 2026-11-15). It is off in the manifest and turned on only while
 * Settings › Show notifications is on ([SystemNotifications.follow]), so Android binds it only then, and only once the
 * user gave notification access.
 */
class LauncherNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        connected = this
        publish()
    }

    override fun onListenerDisconnected() = stopped()

    override fun onDestroy() {
        stopped()
        super.onDestroy()
    }

    // One notice at a time, from what Android passes in: a download or a player can post many times a second, and each
    // read of the whole list copies every notice.
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notice = sbn?.toTv() ?: return
        notices.update { list -> list.filter { it.key != notice.key } + notice }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val key = sbn?.key ?: return
        notices.update { list -> list.filter { it.key != key } }
    }

    /** The whole list, once, when the listener connects. */
    private fun publish() {
        notices.value = activeNotifications.orEmpty().map { it.toTv() }
    }

    /** Only the listener in use: after a rebind, the old one can stop after the new one has published. */
    private fun stopped() {
        if (connected !== this) return
        connected = null
        notices.value = emptyList()
    }

    internal fun find(key: String): StatusBarNotification? = getActiveNotifications(arrayOf(key))?.firstOrNull()

    companion object {
        internal val notices = MutableStateFlow(emptyList<TvNotification>())

        @Volatile internal var connected: LauncherNotificationListener? = null
    }
}

private fun StatusBarNotification.toTv(): TvNotification {
    val extras = notification.extras
    return TvNotification(
        key = key,
        packageName = packageName,
        title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
        text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        postedAt = postTime,
        summary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
        clearable = isClearable
    )
}

/** The notifications from [LauncherNotificationListener], while it runs. */
class SystemNotifications(private val context: Context) : NotificationSource {
    private val listener = ComponentName(context, LauncherNotificationListener::class.java)

    override val all: StateFlow<List<TvNotification>> = LauncherNotificationListener.notices.asStateFlow()

    override fun allowed(): Boolean = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    /** Turns the listener on or off: only when that changes, as the app starts with the setting each time. */
    override fun follow(on: Boolean) {
        val state = if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        if (context.packageManager.getComponentEnabledSetting(listener) == state) return
        context.packageManager.setComponentEnabledSetting(listener, state, PackageManager.DONT_KILL_APP)
        if (on) NotificationListenerService.requestRebind(listener) else LauncherNotificationListener.notices.value = emptyList()
    }

    override fun open(key: String): Boolean {
        val service = LauncherNotificationListener.connected ?: return false
        val sbn = service.find(key) ?: return false
        val intent = sbn.notification.contentIntent ?: return false
        return try {
            // Android 14 and later block an app's start from another app's pending intent unless the sender allows it.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                intent.send(context, 0, null, null, null, null, options.toBundle())
            } else {
                intent.send()
            }
            if (sbn.notification.flags and Notification.FLAG_AUTO_CANCEL != 0) service.cancelNotification(key)
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.w("MyTVLauncher", "A notification's action was cancelled", e)
            false
        }
    }

    override fun dismiss(key: String) {
        LauncherNotificationListener.connected?.cancelNotification(key)
    }

    companion object {
        /** The adb command that gives notification access, for a TV with no screen for it. */
        fun adbGrant(packageName: String) =
            "adb shell cmd notification allow_listener $packageName/${LauncherNotificationListener::class.java.name}"
    }
}
