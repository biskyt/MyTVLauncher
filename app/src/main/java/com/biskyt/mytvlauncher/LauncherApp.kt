package com.biskyt.mytvlauncher

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.biskyt.mytvlauncher.data.InstalledApps
import com.biskyt.mytvlauncher.data.PackageManagerApps

open class LauncherApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, installedApps())
        // A look at the installed apps as the launcher starts, then one for each change (see AppRepository).
        locales = resources.configuration.locales
        container.apps.watch(this)
        container.apps.refresh()
    }

    private var locales: LocaleList? = null

    // The apps' names follow the language: a look when it changes, not for other changes (screen, keyboard).
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.locales == locales) return
        locales = newConfig.locales
        container.apps.refresh()
    }

    /** Where the installed apps come from: the package manager (a fake one in tests). */
    protected open fun installedApps(): InstalledApps = PackageManagerApps(this)
}

val Context.container: AppContainer get() = (applicationContext as LauncherApp).container
