package com.biskyt.mytvlauncher.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

/** How the TV is connected now. */
enum class Link { WIFI, ETHERNET, OTHER, NONE }

/** The default network: how it is connected ([link]), and whether it goes through a VPN. */
data class NetworkState(val link: Link, val vpn: Boolean)

/**
 * The default network as Android reports it, for the top bar's network icon and VPN badge (as MyPlexPlayer's
 * VpnMonitor). A VPN on the router, or on another device, cannot be seen. Android tells of each change: nothing is
 * checked over and over, and only while the top bar shows it. The state is null until the first look, which runs
 * in the background, so the start-up frame makes no call to Android for it.
 */
class NetworkMonitor(context: Context, scope: CoroutineScope) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    val state: StateFlow<NetworkState?> by lazy {
        callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    trySend(caps.toState())
                }

                override fun onLost(network: Network) {
                    trySend(NONE)
                }
            }
            // With no network, Android calls back nothing: this first look shows it.
            trySend(now())
            connectivity?.registerDefaultNetworkCallback(callback)
            awaitClose { connectivity?.unregisterNetworkCallback(callback) }
        }.stateIn(scope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), null)
    }

    private fun now(): NetworkState = connectivity?.let { it.getNetworkCapabilities(it.activeNetwork)?.toState() } ?: NONE

    private fun NetworkCapabilities.toState() = NetworkState(
        link = when {
            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Link.ETHERNET
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Link.WIFI
            else -> Link.OTHER
        },
        vpn = hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    )

    private companion object {
        val NONE = NetworkState(Link.NONE, vpn = false)
        const val STOP_AFTER_MS = 5_000L
    }
}
