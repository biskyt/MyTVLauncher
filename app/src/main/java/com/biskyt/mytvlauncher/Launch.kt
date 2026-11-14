package com.biskyt.mytvlauncher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

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

/** Opens Android's own settings; false if the TV has no screen for it. */
fun Context.openAndroidSettings(): Boolean = tryStart(Intent(Settings.ACTION_SETTINGS))

/** Opens Android's choice of home app, else its default apps; false if the TV has neither. */
fun Context.openHomeAppChoice(): Boolean =
    tryStart(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))

/** Runs [open]; if it fails, says [failed] in a toast, for [duration] (Toast.LENGTH_LONG or LENGTH_SHORT). */
fun Context.openOrSay(failed: String, duration: Int = Toast.LENGTH_LONG, open: Context.() -> Boolean) {
    if (!open()) Toast.makeText(this, failed, duration).show()
}
