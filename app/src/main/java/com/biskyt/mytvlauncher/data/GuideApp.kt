package com.biskyt.mytvlauncher.data

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import com.biskyt.mytvlauncher.openApp
import com.biskyt.mytvlauncher.tryStart

/**
 * What the TV guide button (in the top bar, and the remote's Guide key on Home) opens (the owner, 2026-11-15): the TV's
 * own guide, or MyTVPlayer's or MyPlexPlayer's. Saved by [key], so never change one.
 */
enum class GuideApp(val key: String, private val packageName: String?, val appName: String?) {
    SYSTEM("system", null, null),
    MY_TV_PLAYER("mytvplayer", "com.biskyt.mytvplayer", "MyTVPlayer"),
    MY_PLEX_PLAYER("myplexplayer", "com.biskyt.myplexplayer", "MyPlexPlayer");

    /**
     * Opens the guide; false if nothing opened. The sister apps open their guide with "<package>.action.TV_GUIDE"
     * (MyTVPlayer 1.19.0); an app without it yet opens at its start. The system guide is the TV's own TV app, by its
     * channels, else by its package.
     */
    fun open(context: Context): Boolean = when (packageName) {
        // Else the TV app of Android TV's own Live Channels, then of Philips TVs (the owner's main TV).
        null -> context.tryStart(Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI)) ||
            context.openApp("com.google.android.tv") ||
            context.openApp("org.droidtv.playtv")
        else -> context.tryStart(Intent("$packageName.action.TV_GUIDE").setPackage(packageName)) || context.openApp(packageName)
    }

    companion object {
        fun fromKey(key: String?): GuideApp = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
