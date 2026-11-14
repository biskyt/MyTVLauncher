package com.biskyt.mytvlauncher.core.apps

/** An app that the TV can open from a launcher: its package name, and its name as the TV shows it. */
data class InstalledApp(val packageName: String, val label: String)

/** An app's place on Home, as the launcher saves it. [hidden] apps are not on Home, but stay in All apps. */
data class AppPlace(val packageName: String, val position: Int, val hidden: Boolean = false)

/** The order of the apps on Home. */
object AppOrder {
    /**
     * The places after a look at the installed apps. An app that has a place keeps it, and its hidden state. An app
     * with no place yet goes after the others, shown, in A to Z order of its name. An app that is no longer
     * installed loses its place. The positions are numbered again from 0, with no gaps.
     */
    fun merge(installed: List<InstalledApp>, saved: List<AppPlace>): List<AppPlace> {
        val names = installed.associateBy { it.packageName }
        val kept = saved.filter { it.packageName in names }.sortedBy { it.position }
        val known = kept.mapTo(HashSet()) { it.packageName }
        val added = installed.filter { it.packageName !in known }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .map { AppPlace(it.packageName, 0) }
        return (kept + added).mapIndexed { i, place -> place.copy(position = i) }
    }

    /** The apps shown on Home: those with a place that is not hidden, in the order of their places. */
    fun shown(places: List<AppPlace>): List<AppPlace> = places.sortedBy { it.position }.filter { !it.hidden }
}
