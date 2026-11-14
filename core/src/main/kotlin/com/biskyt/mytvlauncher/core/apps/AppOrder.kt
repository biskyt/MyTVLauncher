package com.biskyt.mytvlauncher.core.apps

/**
 * An app that the TV can open from a launcher: its package name, and its name as the TV shows it. [hiddenAtFirst]: it
 * goes in All apps, but not on Home, when the launcher first finds it (see [LaunchEntries]).
 */
data class InstalledApp(val packageName: String, val label: String, val hiddenAtFirst: Boolean = false)

/** An app's entry for a launcher: TV ([tv], LEANBACK_LAUNCHER) or phone (LAUNCHER), and whether it is a [system] app. */
data class LaunchEntry(val packageName: String, val label: String, val tv: Boolean, val system: Boolean)

/** Which apps a launcher shows. */
object LaunchEntries {
    /**
     * Every app, each once by its TV entry first, not the launcher itself ([own]). An app that is part of the TV's own
     * software and has only a phone entry is [InstalledApp.hiddenAtFirst]: the TV's own launcher shows only TV
     * entries, and such system screens (the owner's Philips TV has two set-up screens) are not for Home. They stay in
     * All apps, so that every app still opens from the launcher. A phone app the owner installs is on Home.
     */
    fun pick(entries: List<LaunchEntry>, own: String): List<InstalledApp> = entries
        .filter { it.packageName != own }
        .sortedByDescending { it.tv }
        .distinctBy { it.packageName }
        .map { InstalledApp(it.packageName, it.label, hiddenAtFirst = !it.tv && it.system) }
}

/** An app's place on Home, as the launcher saves it. [hidden] apps are not on Home, but stay in All apps. */
data class AppPlace(val packageName: String, val position: Int, val hidden: Boolean = false)

/** The order of the apps on Home. */
object AppOrder {
    /**
     * The places after a look at the installed apps. An app that has a place keeps it, and its hidden state. An app
     * with no place yet goes after the others, in A to Z order of its name, hidden if it is
     * [InstalledApp.hiddenAtFirst]. An app that is no longer installed loses its place. With [hideAtFirstAgain] (once,
     * for places saved before 0.3.0), apps that are hidden at first are hidden though they have a place. The positions
     * are numbered again from 0, with no gaps.
     */
    fun merge(installed: List<InstalledApp>, saved: List<AppPlace>, hideAtFirstAgain: Boolean = false): List<AppPlace> {
        val names = installed.associateBy { it.packageName }
        val kept = saved.filter { it.packageName in names }.sortedBy { it.position }
            .map { if (hideAtFirstAgain && names.getValue(it.packageName).hiddenAtFirst) it.copy(hidden = true) else it }
        val known = kept.mapTo(HashSet()) { it.packageName }
        val added = installed.filter { it.packageName !in known }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .map { AppPlace(it.packageName, 0, hidden = it.hiddenAtFirst) }
        return (kept + added).mapIndexed { i, place -> place.copy(position = i) }
    }

    /**
     * [order] (package names, as Home shows them) with [packageName] moved [step] places (-1 left, 1 right). It stays
     * put at either end.
     */
    fun move(order: List<String>, packageName: String, step: Int): List<String> {
        val from = order.indexOf(packageName)
        val to = from + step
        if (from < 0 || to !in order.indices) return order
        return order.toMutableList().apply { add(to, removeAt(from)) }
    }

    /**
     * The places after the user moved apps on Home: the shown apps in [shownOrder], each hidden app kept just after
     * the app it followed, so that showing it again puts it back near where it was.
     */
    fun reorder(places: List<AppPlace>, shownOrder: List<String>): List<AppPlace> {
        val sorted = places.sortedBy { it.position }
        val hiddenAfter = mutableMapOf<String?, MutableList<AppPlace>>()
        var last: String? = null
        sorted.forEach { if (it.hidden) hiddenAfter.getOrPut(last) { mutableListOf() } += it else last = it.packageName }
        val byName = sorted.associateBy { it.packageName }
        val out = hiddenAfter[null].orEmpty().toMutableList()
        shownOrder.forEach { name ->
            byName[name]?.takeIf { !it.hidden }?.let(out::add)
            out += hiddenAfter[name].orEmpty()
        }
        // An app not in shownOrder (it changed meanwhile) keeps its order, after the others.
        val placed = out.mapTo(HashSet()) { it.packageName }
        out += sorted.filter { it.packageName !in placed }
        return out.mapIndexed { i, place -> place.copy(position = i) }
    }
}
