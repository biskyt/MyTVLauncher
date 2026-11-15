package com.biskyt.mytvlauncher.diag

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import android.widget.ScrollView
import android.widget.TextView
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Reads the TV provider's Watch Next and channel items, as MyTVLauncher does, and tries to open each picture. For a
 * content:// picture it also shows whether each home app (MyTVLauncher among them) may open it. The report shows on
 * screen (Up and Down scroll it) and is saved to Download/MyTVLauncher-diagnostics.txt. Query values in addresses are
 * hidden (see [redacted]).
 */
class MainActivity : Activity() {
    private lateinit var text: TextView
    private lateinit var scroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        text = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextColor(Color.WHITE)
            setPadding(48, 32, 48, 32)
            text = "Reading…"
        }
        scroll = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            isFocusable = true
            addView(text)
        }
        setContentView(scroll)
        scroll.requestFocus()
        if (checkSelfPermission(TV_LISTINGS) == PackageManager.PERMISSION_GRANTED) {
            run()
        } else {
            requestPermissions(arrayOf(TV_LISTINGS), 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) = run()

    /** Up and Down scroll a page at a time, so a long report is quick to read on a TV. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
            scroll.smoothScrollBy(0, scroll.height * 3 / 4)
            true
        }
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_CHANNEL_UP -> {
            scroll.smoothScrollBy(0, -scroll.height * 3 / 4)
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }

    private fun run() {
        thread {
            val report = runCatching { report() }.getOrElse { redactedText("The report failed: $it\n${it.stackTraceToString()}") }
            val saved = runCatching { save(report) }.getOrElse { "not saved: $it" }
            runOnUiThread { text.text = "Saved: $saved\n\n$report" }
        }
    }

    private fun report(): String = buildString {
        appendLine("Watch Next diagnostics, ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("READ_TV_LISTINGS: ${if (checkSelfPermission(TV_LISTINGS) == PackageManager.PERMISSION_GRANTED) "given" else "NOT given"}")
        val homes = homeApps()
        val default = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            ?.activityInfo?.packageName
        appendLine("Home apps (uid): " + homes.joinToString { "${it.first} (${it.second})" } + "; default: $default")
        appendLine()

        appendLine("== Watch Next ==")
        query(TvContract.WatchNextPrograms.CONTENT_URI) { c ->
            programme(c, homes)
        }
        appendLine()

        appendLine("== Channels ==")
        val names = mutableMapOf<Long, String>()
        query(TvContract.Channels.CONTENT_URI) { c ->
            val id = c.long(TvContract.Channels._ID) ?: return@query
            val name = c.string(TvContract.Channels.COLUMN_DISPLAY_NAME)
            names[id] = name ?: "?"
            appendLine(
                "#$id ${c.string(TvContract.Channels.COLUMN_PACKAGE_NAME)} \"$name\" type=${c.string(TvContract.Channels.COLUMN_TYPE)} " +
                    "browsable=${c.long(TvContract.Channels.COLUMN_BROWSABLE)}"
            )
            c.string(TvContract.Channels.COLUMN_APP_LINK_POSTER_ART_URI)?.let { appendLine("   app link poster: ${redacted(it)}") }
        }
        appendLine()

        appendLine("== Channel items (the first $PER_CHANNEL of each channel are tested) ==")
        val tested = mutableMapOf<Long, Int>()
        query(TvContract.PreviewPrograms.CONTENT_URI) { c ->
            val channel = c.long(TvContract.PreviewPrograms.COLUMN_CHANNEL_ID) ?: -1
            val n = tested.getOrDefault(channel, 0)
            tested[channel] = n + 1
            if (n < PER_CHANNEL) {
                append("[channel #$channel \"${names[channel]}\"] ")
                programme(c, homes)
            }
        }
        tested.forEach { (channel, n) -> appendLine("channel #$channel \"${names[channel]}\": $n items") }
    }

    /** One Watch Next or channel item: its app, type, title and pictures, each tried. */
    private fun StringBuilder.programme(c: Cursor, homes: List<Pair<String, Int>>) {
        appendLine(
            "${c.string(TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME)} \"${c.string(TvContract.WatchNextPrograms.COLUMN_TITLE)}\" " +
                "type=${c.string(TvContract.WatchNextPrograms.COLUMN_TYPE)} browsable=${c.long(TvContract.WatchNextPrograms.COLUMN_BROWSABLE)} " +
                "posterRatio=${c.string(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_ASPECT_RATIO)} " +
                "thumbRatio=${c.string(TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_ASPECT_RATIO)}"
        )
        for ((label, column) in listOf("poster", TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI, "thumbnail", TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI)
            .chunked(2).map { it[0] to it[1] }) {
            val uri = c.string(column) ?: continue
            appendLine("   $label: ${redacted(uri)}")
            appendLine("      open: ${tryOpen(uri)}")
            if (uri.startsWith("content://")) appendLine("      ${provider(uri)}; grants: ${grants(uri, homes)}")
        }
    }

    /** Opens a picture as MyTVLauncher's picture loader would, and says what came back. */
    private fun tryOpen(uri: String): String = try {
        val bytes = when {
            uri.startsWith("http://") || uri.startsWith("https://") -> {
                val connection = URL(uri).openConnection() as HttpURLConnection
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.instanceFollowRedirects = true
                val code = connection.responseCode
                if (code !in 200..299) return "HTTP $code ${connection.responseMessage}"
                connection.inputStream.use { it.readUpTo() }.also { connection.disconnect() }
            }
            else -> contentResolver.openInputStream(Uri.parse(uri))?.use { it.readUpTo() } ?: return "no stream"
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        "OK ${bytes.size} bytes, ${options.outMimeType ?: "not a picture"} ${options.outWidth}x${options.outHeight}"
    } catch (e: Exception) {
        "FAILED ${e.javaClass.simpleName}: ${e.message?.let(::redactedText)}"
    }

    /** The picture's provider: its app, whether other apps may use it, and what it needs. */
    private fun provider(uri: String): String {
        val authority = Uri.parse(uri).authority ?: return "no authority"
        val info = packageManager.resolveContentProvider(authority, 0) ?: return "provider $authority not visible"
        return "provider ${info.packageName} exported=${info.exported} readPermission=${info.readPermission} " +
            "grantUriPermissions=${info.grantUriPermissions}"
    }

    /** Whether each home app (and MyTVLauncher, if installed) may read [uri]. */
    private fun grants(uri: String, homes: List<Pair<String, Int>>): String = homes.joinToString { (pkg, uid) ->
        val answer = runCatching {
            checkUriPermission(Uri.parse(uri), 0, uid, Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED
        }.fold({ if (it) "yes" else "no" }, { "unknown (${it.javaClass.simpleName})" })
        "$pkg $answer"
    }

    private fun homeApps(): List<Pair<String, Int>> {
        val names = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }.toMutableSet()
        names += LAUNCHER
        return names.mapNotNull { pkg -> runCatching { pkg to packageManager.getPackageUid(pkg, 0) }.getOrNull() }
    }

    private fun StringBuilder.query(uri: Uri, row: (Cursor) -> Unit) {
        try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                appendLine("(${c.count} rows)")
                while (c.moveToNext()) row(c)
            } ?: appendLine("(no cursor)")
        } catch (e: Exception) {
            appendLine("query FAILED ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun save(report: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return "not saved (Android 9: read it on screen)"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "MyTVLauncher-diagnostics.txt")
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return "not saved"
        contentResolver.openOutputStream(uri)?.use { it.write(report.toByteArray()) }
        return "Download/MyTVLauncher-diagnostics.txt"
    }

    private companion object {
        const val TV_LISTINGS = "android.permission.READ_TV_LISTINGS"
        const val LAUNCHER = "com.biskyt.mytvlauncher"
        const val PER_CHANNEL = 3
        const val TIMEOUT_MS = 5000
    }
}

private fun Cursor.string(column: String): String? = getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getString)
private fun Cursor.long(column: String): Long? = getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getLong)

/** Reads at most 4 MB: enough to decode a picture's size. */
private fun InputStream.readUpTo(): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    while (out.size() < 4 * 1024 * 1024) {
        val n = read(buffer)
        if (n < 0) break
        out.write(buffer, 0, n)
    }
    return out.toByteArray()
}
