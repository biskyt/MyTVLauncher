package com.biskyt.mytvlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.biskyt.mytvlauncher.core.ui.AccentPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

/** The settings that a backup holds (see BackupRepository). */
data class SavedSettings(val accent: Int, val watchNextShown: Boolean, val channelsShown: Boolean, val rowOrder: List<String>)

/** The launcher's settings: the accent colour (saved as its ARGB value, as MyPlexPlayer saves it) and the rows shown. */
class SettingsRepository(private val context: Context) {
    val accent: Flow<Int> = context.dataStore.data.map { it[ACCENT] ?: AccentPalette.DEFAULT }

    /** Whether Home shows Continue watching (Settings › Home rows). On by default. */
    val watchNextShown: Flow<Boolean> = context.dataStore.data.map { it[WATCH_NEXT_SHOWN] ?: true }.distinctUntilChanged()

    /** Whether the TV's system apps with only a phone entry were hidden from Home once (0.3.0; see LaunchEntries). */
    suspend fun systemPhoneAppsHidden(): Boolean = context.dataStore.data.first()[SYSTEM_PHONE_APPS_HIDDEN] == true

    suspend fun setSystemPhoneAppsHidden() {
        context.dataStore.edit { it[SYSTEM_PHONE_APPS_HIDDEN] = true }
    }

    suspend fun setAccent(argb: Int) {
        context.dataStore.edit { it[ACCENT] = argb }
    }

    /** Whether Home shows the apps' channel rows (Settings › Home rows). On by default. */
    val channelsShown: Flow<Boolean> = context.dataStore.data.map { it[CHANNELS_SHOWN] ?: true }.distinctUntilChanged()

    suspend fun setChannelsShown(on: Boolean) {
        context.dataStore.edit { it[CHANNELS_SHOWN] = on }
    }

    suspend fun setWatchNextShown(on: Boolean) {
        context.dataStore.edit { it[WATCH_NEXT_SHOWN] = on }
    }

    /** The order of Home's rows, by key (see RowOrder): empty until the user moves one (Settings › Home rows). */
    val rowOrder: Flow<List<String>> = context.dataStore.data
        .map { prefs -> prefs[ROW_ORDER]?.split(',')?.filter { it.isNotEmpty() }.orEmpty() }
        .distinctUntilChanged()

    suspend fun setRowOrder(keys: List<String>) {
        context.dataStore.edit { it[ROW_ORDER] = keys.joinToString(",") }
    }

    /** The settings a backup holds: from the same flows as the screens, with their defaults. */
    suspend fun saved(): SavedSettings = combine(accent, watchNextShown, channelsShown, rowOrder, ::SavedSettings).first()

    /**
     * Restores a backup's settings in one change. The TV's system phone apps count as hidden once already: the
     * backup's app places say which apps are on Home.
     */
    suspend fun restore(saved: SavedSettings) {
        context.dataStore.edit {
            it[ACCENT] = saved.accent
            it[WATCH_NEXT_SHOWN] = saved.watchNextShown
            it[CHANNELS_SHOWN] = saved.channelsShown
            it[ROW_ORDER] = saved.rowOrder.joinToString(",")
            it[SYSTEM_PHONE_APPS_HIDDEN] = true
        }
    }

    /** Back to the defaults: for tests (the settings file outlives each test's app). */
    suspend fun reset() {
        context.dataStore.edit { it.clear() }
    }

    private companion object {
        val ACCENT = intPreferencesKey("accent")
        val WATCH_NEXT_SHOWN = booleanPreferencesKey("watch_next_shown")
        val CHANNELS_SHOWN = booleanPreferencesKey("channels_shown")
        val SYSTEM_PHONE_APPS_HIDDEN = booleanPreferencesKey("system_phone_apps_hidden")
        val ROW_ORDER = stringPreferencesKey("row_order")
    }
}
