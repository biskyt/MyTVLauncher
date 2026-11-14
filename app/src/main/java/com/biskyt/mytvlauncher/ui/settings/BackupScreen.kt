package com.biskyt.mytvlauncher.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.biskyt.mytvlauncher.AppContainer
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.data.BackupException
import com.biskyt.mytvlauncher.data.BackupFile
import com.biskyt.mytvlauncher.ui.common.Muted
import com.biskyt.mytvlauncher.ui.common.PillButton
import com.biskyt.mytvlauncher.ui.common.rememberFirstFocus
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * What the Backup & restore page does and shows (as MyPlexPlayer's BackupViewModel): the backup file on this TV, a
 * status line, and a file that waits for the user to confirm a restore. One action runs at a time.
 */
private class BackupState(private val c: AppContainer, private val context: Context, private val scope: CoroutineScope) {
    private val file = c.backupFile

    val needsPermission: Boolean get() = file.needsPermission

    var saved by mutableStateOf<BackupFile.Saved?>(null)
        private set
    var status by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var confirming by mutableStateOf<Uri?>(null)
        private set

    /** Android 9: whether "Back up now" asks for the storage permission (else the page asks when it opens). */
    private var backUpAfterPermission = false

    /** Android 9: asked already when the page opened, on any visit (a refusal is not asked again until a restart). */
    val askedOnOpening: Boolean get() = c.backupPermissionAsked

    fun look() {
        scope.launch { saved = runCatching { file.find() }.getOrNull() }
    }

    fun backUp() = run(R.string.backup_saving) {
        val s = file.write(c.backup.export().toByteArray())
        saved = s
        context.getString(R.string.backup_saved_to, s.path)
    }

    fun ask(uri: Uri) {
        confirming = uri
        status = null
    }

    /** Files opened with MyTVLauncher from another app (see MainActivity), offered to restore as one chosen here. */
    suspend fun takeOpenedFiles() = c.openedFile.filterNotNull().collect {
        c.openedFile.value = null
        ask(it)
    }

    fun cancel() {
        confirming = null
    }

    fun failed(message: String) {
        status = message
    }

    fun askingPermission(forBackUp: Boolean) {
        backUpAfterPermission = forBackUp
        if (!forBackUp) c.backupPermissionAsked = true
    }

    /**
     * Android 9: the answer to the storage permission. Given, the backup file can be seen: one there already (from
     * before a reinstall) is offered to restore, never saved over; otherwise "Back up now" carries on.
     */
    fun permissionResult(granted: Boolean) {
        val backUpNext = backUpAfterPermission
        backUpAfterPermission = false
        if (!granted) {
            if (backUpNext) failed(context.getString(R.string.backup_permission_denied))
            return
        }
        scope.launch {
            val found = runCatching { file.find() }.getOrElse {
                if (it is CancellationException) throw it
                return@launch failed(context.getString(R.string.backup_find_failed, it.message ?: it.javaClass.simpleName))
            }
            saved = found
            when {
                !backUpNext -> Unit
                found != null -> status = context.getString(R.string.backup_already_there)
                else -> backUp()
            }
        }
    }

    fun restore() {
        val uri = confirming ?: return
        // A backup still saving: the file waits, and Restore can be pressed again.
        if (busy) return
        confirming = null
        run(R.string.backup_restoring) {
            // In the app's scope: leaving the page must not stop a restore halfway.
            c.appScope.async { file.read(uri) { c.backup.restore(it) } }.await()
            context.getString(R.string.backup_restored)
        }
    }

    /** Runs one action at a time. A failure shows on the page: storage errors differ from TV to TV, and none may crash. */
    @Suppress("TooGenericExceptionCaught")
    private fun run(working: Int, block: suspend () -> String?) {
        if (busy) return
        busy = true
        status = context.getString(working)
        scope.launch {
            status = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackupException) {
                context.getString(if (e.reason == BackupException.Reason.NEWER) R.string.backup_newer else R.string.backup_not_a_backup)
            } catch (e: Exception) {
                context.getString(R.string.backup_failed, e.message ?: e.javaClass.simpleName)
            }
            busy = false
        }
    }
}

/**
 * Android TV fills in for system apps that a TV leaves out (such as the file picker) with stand-ins that only say "no
 * application can handle this request" (as MyPlexPlayer's BackupScreen).
 */
private val ActivityInfo.isStandIn get() = "stub" in packageName.lowercase()

/** Android's own file picker (com.android.documentsui, or Google's com.google.android.documentsui). */
private val ActivityInfo.isAndroidPicker get() = packageName.endsWith(".documentsui")

private fun Context.handlers(intent: Intent) =
    packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).map { it.activityInfo }

/** Opens the picker in the backup folder (where the TV's picker can). */
private class OpenBackup : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).putExtra(DocumentsContract.EXTRA_INITIAL_URI, BackupFile.PICKER_START)
}

/**
 * Opens a way to choose the backup file with [launch]; false if the TV has none. Android's own picker where the TV has
 * it, in the backup folder. Anything else that answers may be a stand-in under another name, so then the installed file
 * managers are offered.
 */
private fun Context.pickBackup(chooserTitle: String, launch: (Intent) -> Unit): Boolean {
    val open = OpenBackup().createIntent(this, arrayOf("*/*"))
    val openers = handlers(open)
    val picker = openers.firstOrNull { it.isAndroidPicker }
    val get = Intent(Intent.ACTION_GET_CONTENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE)
    val (standIns, fileManagers) = handlers(get).partition { it.isStandIn }
    val tries = listOfNotNull(
        picker?.let { Intent(open).setPackage(it.packageName) },
        Intent.createChooser(get, chooserTitle)
            .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, standIns.map { ComponentName(it.packageName, it.name) }.toTypedArray())
            .takeIf { fileManagers.isNotEmpty() },
        open.takeIf { picker == null && openers.any { !it.isStandIn } }
    )
    // The first that opens: if the picker fails after all, a file manager is next.
    return tries.any {
        try {
            launch(it)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

/**
 * Settings › Backup & restore (the owner, 2026-11-14: as MyPlexPlayer's). One plain JSON file with a fixed name,
 * written again each time. The storage permission is asked for only on Android 9, and only here.
 */
@Composable
fun BackupScreen() {
    val context = LocalContext.current
    val container = context.container
    val scope = rememberCoroutineScope()
    // Android 9 looks once the permission is given (see permissionResult).
    val state = remember { BackupState(container, context, scope).also { if (!it.needsPermission) it.look() } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), state::permissionResult)
    fun askPermission(forBackUp: Boolean) {
        state.askingPermission(forBackUp)
        permission.launch(BackupFile.STORAGE_PERMISSION)
    }
    // Android 9: asked for when the page opens, so that a backup there already (from before a reinstall) shows.
    LaunchedEffect(Unit) { if (state.needsPermission && !state.askedOnOpening) askPermission(forBackUp = false) }
    LaunchedEffect(state) { state.takeOpenedFiles() }
    val backUp = { if (state.needsPermission) askPermission(forBackUp = true) else state.backUp() }
    val chooserTitle = stringResource(R.string.backup_choose_file)
    val noPicker = stringResource(R.string.backup_no_picker)
    val chooseFile = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) result.data?.data?.let(state::ask)
    }
    fun pick() {
        if (!context.pickBackup(chooserTitle) { chooseFile.launch(it) }) state.failed(noPicker)
    }

    SettingsPage(R.string.backup_title) {
        item {
            Column(Modifier.width(760.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Muted(stringResource(R.string.backup_intro), maxLines = 4)
                val last = state.saved?.let { s ->
                    val time = s.modifiedAt.takeIf { it > 0 }?.let {
                        "${DateFormat.getMediumDateFormat(context).format(Date(it))} ${DateFormat.getTimeFormat(context).format(Date(it))}"
                    }
                    listOfNotNull(time, s.path).joinToString(" · ")
                }
                Muted(if (last != null) stringResource(R.string.backup_last, last) else stringResource(R.string.backup_none), maxLines = 2)
            }
        }
        item {
            if (state.confirming != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.backup_confirm), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton(stringResource(R.string.backup_restore), state::restore, rememberFirstFocus(), primary = true)
                        PillButton(stringResource(R.string.backup_cancel), state::cancel)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton(stringResource(R.string.backup_now), backUp, rememberFirstFocus(), primary = true)
                    val s = state.saved
                    if (s != null) PillButton(stringResource(R.string.backup_restore), { state.ask(s.uri) })
                    val restoreFile = if (s == null) R.string.backup_restore_file else R.string.backup_restore_other_file
                    PillButton(stringResource(restoreFile), ::pick)
                }
            }
        }
        state.status?.let { item { Text(it, style = MaterialTheme.typography.bodyLarge) } }
    }
}
