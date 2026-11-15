package com.biskyt.mytvlauncher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.biskyt.mytvlauncher.core.watchnext.WatchNextItem
import java.net.URISyntaxException

/**
 * Starts the first of [intents] that the TV can open, as a new task; false if it can open none. A TV that refuses one
 * (a SecurityException) counts as not able to open it, so the launcher never crashes on a TV's own screens.
 */
fun Context.tryStart(vararg intents: Intent): Boolean = intents.any { intent ->
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

/** Opens [packageName]'s TV entry, else its phone entry. False if it has neither, or it will not open. */
fun Context.openApp(packageName: String): Boolean {
    val intent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
        ?: packageManager.getLaunchIntentForPackage(packageName)
        ?: return false
    return tryStart(intent)
}

/**
 * Opens a Watch Next item: the app's own intent for it, else the app. The intent comes from another app, so it opens
 * only that app (an intent for any other app, or for the launcher itself, is not started), with no grant of this
 * launcher's access to files or content.
 */
fun Context.openWatchNext(item: WatchNextItem): Boolean {
    val intent = item.intentUri?.let {
        try {
            Intent.parseUri(it, Intent.URI_INTENT_SCHEME)
        } catch (_: URISyntaxException) {
            null
        }
    }?.takeIf { it.component == null || it.component?.packageName == item.packageName }?.apply {
        selector = null
        `package` = item.packageName
        flags = flags and GRANT_FLAGS.inv()
    }
    return (intent != null && tryStart(intent)) || openApp(item.packageName)
}

private const val GRANT_FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

/** Opens Android's page for [packageName] (App info); false if the TV has no screen for it. */
fun Context.openAppInfo(packageName: String): Boolean =
    tryStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))

/** The TV guide that Settings › TV guide chose, or a message when nothing opened (the guide button, the Guide key). */
fun Context.openGuide() {
    val app = container.guideApp.value
    openOrSay(getString(R.string.guide_missing)) { app.open(this) }
}

/** The app's page in the Play Store, or on Google Play's website where the TV has no Play Store. */
fun Context.openInPlayStore(packageName: String): Boolean {
    val market = Uri.parse("market://details?id=$packageName")
    return tryStart(
        Intent(Intent.ACTION_VIEW, market).setPackage(PLAY_STORE),
        Intent(Intent.ACTION_VIEW, market),
        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
    )
}

private const val PLAY_STORE = "com.android.vending"

/** Opens Android's usage access settings (for recent apps); false if the TV has no screen for it. */
fun Context.openUsageAccess(): Boolean = tryStart(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

/** Opens Android's own settings; false if the TV has no screen for it. */
fun Context.openAndroidSettings(): Boolean = tryStart(Intent(Settings.ACTION_SETTINGS))

/** Opens Android's choice of home app, else its default apps; false if the TV has neither. */
fun Context.openHomeAppChoice(): Boolean =
    tryStart(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))

/** Runs [open]; if it fails, says [failed] in a toast, for [duration] (Toast.LENGTH_LONG or LENGTH_SHORT). */
fun Context.openOrSay(failed: String, duration: Int = Toast.LENGTH_LONG, open: Context.() -> Boolean) {
    if (!open()) Toast.makeText(this, failed, duration).show()
}
