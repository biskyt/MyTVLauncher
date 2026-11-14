package com.biskyt.mytvlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import com.biskyt.mytvlauncher.core.apps.AppOrder
import com.biskyt.mytvlauncher.core.apps.AppPlace
import com.biskyt.mytvlauncher.core.apps.InstalledApp
import com.biskyt.mytvlauncher.core.apps.LaunchEntries
import com.biskyt.mytvlauncher.core.apps.LaunchEntry
import com.biskyt.mytvlauncher.data.db.AppPlaceDao
import com.biskyt.mytvlauncher.data.db.AppPlaceRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** An app in All apps: whether it is [hidden] from Home. */
data class AppEntry(val packageName: String, val label: String, val hidden: Boolean)

/** Where the installed apps come from: the TV's package manager, or a fake one in tests. */
fun interface InstalledApps {
    fun list(): List<InstalledApp>
}

/**
 * The apps on Home. Home reads the saved list ([shown]), so its first frame needs no look at the installed apps. The
 * look ([refresh]) runs in the background: when the launcher starts and each time Home comes to the front, when the
 * language changes (the apps' names), and when an app is installed, removed or changed. It writes the list only when
 * it changed.
 */
class AppRepository(
    private val dao: AppPlaceDao,
    private val installed: InstalledApps,
    private val settings: SettingsRepository,
    private val scope: CoroutineScope
) {
    /** Every app, hidden apps too, in Home's order (All apps). */
    val all: Flow<List<AppEntry>> = dao.observe().map { rows -> rows.map { AppEntry(it.packageName, it.label, it.hidden) } }

    private val looks = Refresher(scope, SETTLE_MS) { lookNow() }

    /** Asks for a look at the installed apps, in the background: now, or after a short wait for an app change. */
    fun refresh(afterChange: Boolean = false) = looks.request(afterChange)

    /** Looks at the installed apps now, and saves the list if it changed. */
    suspend fun look() = looks.now()

    /** Hides [packageName] from Home, or shows it again; it stays in All apps either way. */
    suspend fun setHidden(packageName: String, hidden: Boolean) = looks.exclusive { dao.setHidden(packageName, hidden) }

    /** Saves the order the user moved the apps on Home into ([shownOrder]: package names, as Home shows them). */
    suspend fun saveOrder(shownOrder: List<String>) = looks.exclusive {
        val rows = dao.all().associateBy { it.packageName }
        val places = AppOrder.reorder(rows.values.map { AppPlace(it.packageName, it.position, it.hidden) }, shownOrder)
        dao.replace(places.map { rows.getValue(it.packageName).copy(position = it.position) })
    }

    private suspend fun lookNow() {
        val apps = installed.list()
        val saved = dao.all()
        val labels = apps.associate { it.packageName to it.label }
        // Once, for places saved before 0.3.0: the TV's system apps with only a phone entry go off Home.
        val hideAgain = !settings.systemPhoneAppsHidden()
        val places = AppOrder.merge(apps, saved.map { AppPlace(it.packageName, it.position, it.hidden) }, hideAgain)
        val rows = places.map { AppPlaceRow(it.packageName, labels.getValue(it.packageName), it.position, it.hidden) }
        if (rows != saved) dao.replace(rows)
        if (hideAgain) settings.setSystemPhoneAppsHidden()
    }

    private companion object {
        const val SETTLE_MS = 500L
    }

    /** Looks again whenever an app is installed, removed or changed. */
    fun watch(context: Context) {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return
        launcherApps.registerCallback(
            object : LauncherApps.Callback() {
                override fun onPackageAdded(packageName: String, user: UserHandle) = refresh(afterChange = true)
                override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh(afterChange = true)
                override fun onPackageChanged(packageName: String, user: UserHandle) = refresh(afterChange = true)
                override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
                    refresh(afterChange = true)
                override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
                    refresh(afterChange = true)
            },
            Handler(Looper.getMainLooper())
        )
    }
}

/** The apps a launcher can open: TV entries, and phone entries of apps that are not system apps (see LaunchEntries). */
class PackageManagerApps(private val context: Context) : InstalledApps {
    override fun list(): List<InstalledApp> {
        val pm = context.packageManager
        fun query(category: String, tv: Boolean) = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0).map {
            val system = it.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
            LaunchEntry(it.activityInfo.packageName, it.loadLabel(pm).toString(), tv, system)
        }
        return LaunchEntries.pick(
            query(Intent.CATEGORY_LEANBACK_LAUNCHER, tv = true) + query(Intent.CATEGORY_LAUNCHER, tv = false),
            context.packageName
        )
    }
}
