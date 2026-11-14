package com.biskyt.mytvlauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import com.biskyt.mytvlauncher.core.apps.AppOrder
import com.biskyt.mytvlauncher.core.apps.AppPlace
import com.biskyt.mytvlauncher.core.apps.InstalledApp
import com.biskyt.mytvlauncher.data.db.AppPlaceDao
import com.biskyt.mytvlauncher.data.db.AppPlaceRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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
    private val scope: CoroutineScope
) {
    val shown: Flow<List<InstalledApp>> = dao.observe().map { rows ->
        val labels = rows.associate { it.packageName to it.label }
        AppOrder.shown(
            rows.map {
                AppPlace(it.packageName, it.position, it.hidden)
            }
        ).map { InstalledApp(it.packageName, labels.getValue(it.packageName)) }
    }

    /**
     * Requests for a look, each with how long to wait first. A request made while a look runs is taken once it ends.
     * Requests that come together (one app update sends removed, added and changed) wait [SETTLE_MS] and make one look.
     */
    private val requests = Channel<Long>(Channel.CONFLATED)

    /** One look at a time, also for a direct call to [look]. */
    private val looking = Mutex()

    init {
        scope.launch(Dispatchers.IO) {
            requests.consumeEach { wait ->
                delay(wait)
                requests.tryReceive() // asked again while waiting: this look covers it
                look()
            }
        }
    }

    /** Asks for a look at the installed apps, in the background: now, or after a short wait for an app change. */
    fun refresh(afterChange: Boolean = false) {
        requests.trySend(if (afterChange) SETTLE_MS else 0L)
    }

    /** Looks at the installed apps now, and saves the list if it changed. */
    suspend fun look() = withContext(Dispatchers.IO) { looking.withLock { lookNow() } }

    private suspend fun lookNow() {
        val apps = installed.list()
        val saved = dao.all()
        val labels = apps.associate { it.packageName to it.label }
        val places = AppOrder.merge(apps, saved.map { AppPlace(it.packageName, it.position, it.hidden) })
        val rows = places.map { AppPlaceRow(it.packageName, labels.getValue(it.packageName), it.position, it.hidden) }
        if (rows != saved) dao.replace(rows)
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

/** The apps a launcher can open: TV apps first, then phone apps (for a phone or a TV app with no TV entry). */
class PackageManagerApps(private val context: Context) : InstalledApps {
    override fun list(): List<InstalledApp> {
        val pm = context.packageManager
        val own = context.packageName
        fun query(category: String) = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)
        return (query(Intent.CATEGORY_LEANBACK_LAUNCHER) + query(Intent.CATEGORY_LAUNCHER))
            .map { it.activityInfo.packageName to it }
            .filter { (pkg, _) -> pkg != own }
            .distinctBy { it.first }
            .map { (pkg, info) -> InstalledApp(pkg, info.loadLabel(pm).toString()) }
    }
}
