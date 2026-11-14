package com.biskyt.mytvlauncher.core.recents

/** When an app was last in front, by Android's usage stats (ms since 1970). */
data class AppUse(val packageName: String, val lastUsedMs: Long)

/** The recent apps panel (a double Home press). */
object RecentApps {
    const val LIMIT = 12

    /**
     * The apps to show, most recent first: only apps the launcher can open and shows ([launchable], which leaves out
     * the launcher itself), each once (usage stats give a row per day), at most [limit].
     */
    fun order(uses: List<AppUse>, launchable: Set<String>, limit: Int = LIMIT): List<String> = uses
        .filter { it.lastUsedMs > 0 && it.packageName in launchable }
        .sortedByDescending { it.lastUsedMs }
        .map { it.packageName }
        .distinct()
        .take(limit)
}
