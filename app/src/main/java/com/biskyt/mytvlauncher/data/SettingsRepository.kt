package com.biskyt.mytvlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.biskyt.mytvlauncher.core.speaker.WakeDelay
import com.biskyt.mytvlauncher.core.ui.AccentPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

/** The settings that a backup holds (see BackupRepository). */
data class SavedSettings(
    val accent: Int,
    val watchNextShown: Boolean,
    val channelsShown: Boolean,
    val rowOrder: List<String>,
    val speakerWake: Boolean = false,
    val speakerWakeDelay: Int = WakeDelay.DEFAULT,
    val guideApp: GuideApp = GuideApp.SYSTEM
)

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

    /** Whether the launcher wakes the speaker when the TV wakes (Settings › Speaker wake). Off by default. */
    val speakerWake: Flow<Boolean> = context.dataStore.data.map { it[SPEAKER_WAKE] ?: false }.distinctUntilChanged()

    /** How many seconds after the TV wakes the speaker wake sound plays (see WakeDelay). */
    val speakerWakeDelay: Flow<Int> = context.dataStore.data.map { it[SPEAKER_WAKE_DELAY] ?: WakeDelay.DEFAULT }.distinctUntilChanged()

    /** What the TV guide button opens (Settings › TV guide). The TV's own guide by default. */
    val guideApp: Flow<GuideApp> = context.dataStore.data.map { GuideApp.fromKey(it[GUIDE_APP]) }.distinctUntilChanged()

    suspend fun setGuideApp(app: GuideApp) {
        context.dataStore.edit { it[GUIDE_APP] = app.key }
    }

    suspend fun setSpeakerWake(on: Boolean) {
        context.dataStore.edit { it[SPEAKER_WAKE] = on }
    }

    suspend fun setSpeakerWakeDelay(seconds: Int) {
        context.dataStore.edit { it[SPEAKER_WAKE_DELAY] = seconds }
    }

    /** The settings a backup holds: from the same flows as the screens, with their defaults. */
    suspend fun saved() = SavedSettings(
        accent.first(),
        watchNextShown.first(),
        channelsShown.first(),
        rowOrder.first(),
        speakerWake.first(),
        speakerWakeDelay.first(),
        guideApp.first()
    )

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
            it[SPEAKER_WAKE] = saved.speakerWake
            it[SPEAKER_WAKE_DELAY] = saved.speakerWakeDelay
            it[GUIDE_APP] = saved.guideApp.key
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
        val SPEAKER_WAKE = booleanPreferencesKey("speaker_wake")
        val SPEAKER_WAKE_DELAY = intPreferencesKey("speaker_wake_delay")
        val GUIDE_APP = stringPreferencesKey("guide_app")
    }
}
