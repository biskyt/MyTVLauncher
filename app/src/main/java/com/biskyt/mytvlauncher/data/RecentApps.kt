package com.biskyt.mytvlauncher.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.biskyt.mytvlauncher.core.recents.AppUse

/**
 * Where the recent apps panel (a double Home press) reads which apps were in front last: Android's usage stats, or a
 * fake in tests. Android gives them only with usage access, which the owner turns on once.
 */
interface RecentsSource {
    fun allowed(): Boolean

    /** When each app was last in front, over the last days. Called in the background only. */
    fun uses(): List<AppUse>
}

class UsageStatsRecents(private val context: Context) : RecentsSource {
    // Android's own check of an app op for this app: both forms are deprecated on the newest Android, with no
    // replacement on Android 9 to 11.
    @Suppress("DEPRECATION")
    override fun allowed(): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun uses(): List<AppUse> {
        val stats = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val now = System.currentTimeMillis()
        return stats.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - WEEK_MS, now).orEmpty()
            .map { AppUse(it.packageName, it.lastTimeUsed) }
    }

    companion object {
        private const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L

        /** The command that gives usage access, for TVs with no screen for it. */
        fun adbGrant(packageName: String) = "adb shell appops set $packageName GET_USAGE_STATS allow"
    }
}
