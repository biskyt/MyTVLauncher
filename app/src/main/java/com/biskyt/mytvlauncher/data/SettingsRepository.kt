package com.biskyt.mytvlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.biskyt.mytvlauncher.core.ui.AccentPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

/** The launcher's settings: the accent colour (saved as its ARGB value, as MyPlexPlayer saves it) and the rows shown. */
class SettingsRepository(private val context: Context) {
    val accent: Flow<Int> = context.dataStore.data.map { it[ACCENT] ?: AccentPalette.DEFAULT }

    /** Whether Home shows Continue watching (Settings › Home rows). On by default. */
    val watchNextShown: Flow<Boolean> = context.dataStore.data.map { it[WATCH_NEXT_SHOWN] ?: true }.distinctUntilChanged()

    suspend fun setAccent(argb: Int) {
        context.dataStore.edit { it[ACCENT] = argb }
    }

    suspend fun setWatchNextShown(on: Boolean) {
        context.dataStore.edit { it[WATCH_NEXT_SHOWN] = on }
    }

    /** Back to the defaults: for tests (the settings file outlives each test's app). */
    suspend fun reset() {
        context.dataStore.edit { it.clear() }
    }

    private companion object {
        val ACCENT = intPreferencesKey("accent")
        val WATCH_NEXT_SHOWN = booleanPreferencesKey("watch_next_shown")
    }
}
