package com.biskyt.mytvlauncher.ui.home

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.biskyt.mytvlauncher.R
import com.biskyt.mytvlauncher.container
import com.biskyt.mytvlauncher.data.Link
import com.biskyt.mytvlauncher.ui.common.LauncherIcons
import com.biskyt.mytvlauncher.ui.theme.LocalAccent
import com.biskyt.mytvlauncher.ui.theme.Palette
import java.util.Date
import kotlinx.coroutines.delay

/** The time of day in the TV's own format (12 or 24 hours), updated each minute (as MyPlexPlayer's Clock). */
@Composable
fun Clock(modifier: Modifier = Modifier.padding(horizontal = 8.dp)) {
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(MINUTE_MS - System.currentTimeMillis() % MINUTE_MS)
            value = System.currentTimeMillis()
        }
    }
    // Made again each minute: it keeps the time zone and the 12 or 24 hour choice from when it was made.
    Text(
        DateFormat.getTimeFormat(context).format(Date(now)),
        style = MaterialTheme.typography.titleSmall,
        color = Palette.textDim,
        modifier = modifier
    )
}

/** How the TV is connected (Wi-Fi, Ethernet or none), and a VPN badge when its traffic goes through a VPN. */
@Composable
fun NetworkStatus(modifier: Modifier = Modifier) {
    val known by LocalContext.current.container.network.state.collectAsState()
    val state = known ?: return
    val (icon, label) = when (state.link) {
        Link.WIFI -> LauncherIcons.Wifi to stringResource(R.string.network_wifi)
        Link.ETHERNET -> LauncherIcons.Ethernet to stringResource(R.string.network_ethernet)
        Link.OTHER -> LauncherIcons.Wifi to stringResource(R.string.network_other)
        Link.NONE -> LauncherIcons.NoNetwork to stringResource(R.string.network_none)
    }
    Row(
        modifier.padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (state.link == Link.NONE) Palette.warning else Palette.textDim,
            modifier = Modifier.size(20.dp).semantics { contentDescription = label }
        )
        if (state.vpn) {
            val accent = LocalAccent.current.color
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(LauncherIcons.Shield, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.network_vpn), style = MaterialTheme.typography.labelMedium, color = Palette.textDim)
            }
        }
    }
}

private const val MINUTE_MS = 60_000L
