package com.biskyt.mytvlauncher.data

import androidx.room.withTransaction
import com.biskyt.mytvlauncher.core.apps.AppPlace
import com.biskyt.mytvlauncher.core.speaker.WakeDelay
import com.biskyt.mytvlauncher.core.ui.AccentPalette
import com.biskyt.mytvlauncher.data.db.AppPlaceRow
import com.biskyt.mytvlauncher.data.db.HiddenRow
import com.biskyt.mytvlauncher.data.db.LauncherDatabase
import java.io.ByteArrayOutputStream
import java.io.InputStream
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** A file that cannot be restored: [reason] says why, for the message the user sees. */
class BackupException(val reason: Reason) : Exception(reason.name) {
    enum class Reason { NOT_A_BACKUP, NEWER }
}

/**
 * A backup (the owner, 2026-11-14: as MyPlexPlayer's): the settings, the apps' order and which are hidden, and the
 * user's Continue watching and channel choices. Other apps' items are not in it: the launcher reads them again.
 */
data class Backup(val createdAt: Long, val settings: SavedSettings, val places: List<AppPlace>, val hidden: List<HiddenRow>) {
    /** The backup as plain JSON (Android's own org.json: no other library for one small file). */
    fun toJson(): String = JSONObject()
        .put("app", APP)
        .put("version", VERSION)
        .put("createdAt", createdAt)
        .put(
            "settings",
            JSONObject()
                .put("accent", settings.accent)
                .put("watchNextShown", settings.watchNextShown)
                .put("channelsShown", settings.channelsShown)
                .put("rowOrder", JSONArray(settings.rowOrder))
                .put("speakerWake", settings.speakerWake)
                .put("speakerWakeDelay", settings.speakerWakeDelay)
                .put("guideApp", settings.guideApp.key)
        )
        .put(
            "apps",
            JSONArray(
                places.map {
                    JSONObject().put("packageName", it.packageName).put("position", it.position).put("hidden", it.hidden)
                }
            )
        )
        .put("hidden", JSONArray(hidden.map { JSONObject().put("kind", it.kind).put("key", it.key) }))
        .toString(2)

    companion object {
        /** Tells a MyTVLauncher backup from any other JSON file. */
        const val APP = "MyTVLauncher"
        const val VERSION = 1

        /**
         * Reads and checks a backup. The header is checked first, so that a file from a newer version is reported as
         * such even if its contents changed shape.
         */
        fun parse(text: String): Backup {
            val root = notABackupUnless { JSONObject(text) }
            if (root.optString("app") != APP) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
            if (root.optInt("version") > VERSION) throw BackupException(BackupException.Reason.NEWER)
            return notABackupUnless {
                val settings = root.getJSONObject("settings")
                Backup(
                    createdAt = root.optLong("createdAt"),
                    settings = SavedSettings(
                        // Only a colour the launcher offers: a hand-edited 0 would hide the focus on every screen.
                        accent =
                        settings.getInt("accent").takeIf { argb -> AccentPalette.presets.any { it.argb == argb } }
                            ?: AccentPalette.DEFAULT,
                        watchNextShown = settings.getBoolean("watchNextShown"),
                        channelsShown = settings.getBoolean("channelsShown"),
                        rowOrder = settings.optJSONArray("rowOrder").strings(),
                        // Since 0.6.0: a 0.5.0 backup has no speaker wake, so it restores as off.
                        speakerWake = settings.optBoolean("speakerWake", false),
                        speakerWakeDelay =
                        settings.optInt("speakerWakeDelay", WakeDelay.DEFAULT).takeIf { it in WakeDelay.CHOICES } ?: WakeDelay.DEFAULT,
                        // Since 0.7.0: an older backup restores the TV's own guide.
                        guideApp = GuideApp.fromKey(settings.optString("guideApp"))
                    ),
                    places = root.getJSONArray("apps").objects().map {
                        AppPlace(it.getString("packageName"), it.getInt("position"), it.getBoolean("hidden"))
                    }.distinctBy { it.packageName },
                    hidden = root.getJSONArray("hidden").objects().map { HiddenRow(it.getString("kind"), it.getString("key")) }
                )
            }
        }

        private inline fun <T> notABackupUnless(block: () -> T): T = try {
            block()
        } catch (e: JSONException) {
            throw BackupException(BackupException.Reason.NOT_A_BACKUP).apply { initCause(e) }
        }

        private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { getString(it) }

        private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
    }
}

/**
 * Writes and restores backups. A restore replaces the app places and the user's choices in one transaction (a bad file
 * changes nothing), then the settings, then looks at the installed apps: an app in the backup that is not installed
 * here is dropped, and an app installed here that is not in the backup goes at the end.
 */
class BackupRepository(
    private val db: LauncherDatabase,
    private val settings: SettingsRepository,
    private val apps: AppRepository
) {
    suspend fun export(): String {
        val (places, hidden) = db.withTransaction { db.appPlaces().all() to db.hidden().all() }
        return Backup(
            createdAt = System.currentTimeMillis(),
            settings = settings.saved(),
            places = places.map { AppPlace(it.packageName, it.position, it.hidden) },
            hidden = hidden
        ).toJson()
    }

    /**
     * Reads the whole file first: a file that is not a backup changes nothing. A backup is a few KB, so a file larger
     * than [MAX_BYTES] (any JSON file can be opened with the launcher) is not one, and is not read into memory.
     */
    suspend fun restore(input: InputStream) {
        val backup = Backup.parse(input.readAtMost(MAX_BYTES).decodeToString())
        // The app places and the choices in one transaction, with no look at the apps alongside it.
        apps.whileIdle {
            db.withTransaction {
                val labels = db.appPlaces().all().associate { it.packageName to it.label }
                db.appPlaces().replace(
                    backup.places.map { AppPlaceRow(it.packageName, labels[it.packageName] ?: it.packageName, it.position, it.hidden) }
                )
                db.hidden().replace(backup.hidden)
            }
        }
        settings.restore(backup.settings)
        // Done only once the apps installed here have their places: those not in the backup go at the end.
        apps.look()
    }

    private companion object {
        const val MAX_BYTES = 1 shl 20
    }
}

/** The stream's bytes, or NOT_A_BACKUP once more than [max] come (InputStream.readNBytes needs Android 13). */
private fun InputStream.readAtMost(max: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(BUFFER_BYTES)
    while (true) {
        val n = read(buffer)
        if (n < 0) return out.toByteArray()
        out.write(buffer, 0, n)
        if (out.size() > max) throw BackupException(BackupException.Reason.NOT_A_BACKUP)
    }
}

private const val BUFFER_BYTES = 8192
