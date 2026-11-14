package com.biskyt.mytvlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import com.biskyt.mytvlauncher.ui.AppRoot
import com.biskyt.mytvlauncher.ui.Navigator

/** The home screen. It is the TV's home app when chosen as one (see RELEASING.md). */
class MainActivity : ComponentActivity() {
    private val nav = Navigator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppRoot(nav) }
    }

    // Each time Home comes to the front: a look at the installed apps in the background, after Home has drawn from the
    // saved list. It catches a change that sent no callback (see AppRepository.watch).
    override fun onStart() {
        super.onStart()
        container.apps.refresh()
    }

    // Home pressed while the launcher is running. Only a short press gets here: Android keeps the Home key itself.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        takeIntent(intent)
    }

    @VisibleForTesting
    internal fun takeIntent(intent: Intent) {
        if (!intent.hasCategory(Intent.CATEGORY_HOME)) return
        container.homePressed(inFront = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        nav.home()
    }
}
