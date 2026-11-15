package com.biskyt.mytvlauncher.diag

/**
 * An address as the report shows it: every query value and any user name or password replaced by "…", so that a
 * server token (such as Plex's X-Plex-Token) never reaches the report. The scheme, host, port, path and query names
 * stay, because they show where a picture comes from.
 */
fun redacted(address: String): String {
    val uri = address.replace(USER_INFO, "$1…@")
    val q = uri.indexOf('?')
    if (q < 0) return uri
    val fragment = uri.indexOf('#', q).let { if (it < 0) uri.length else it }
    val query = uri.substring(q + 1, fragment).split('&').joinToString("&") { part ->
        val eq = part.indexOf('=')
        if (eq < 0) part else part.substring(0, eq) + "=…"
    }
    return uri.substring(0, q + 1) + query
}

private val USER_INFO = Regex("^([a-zA-Z][a-zA-Z0-9+.-]*://)[^/@?#]*@")

/** Any text (an error message, a stack trace) with each address in it shown as [redacted] shows it. */
fun redactedText(text: String): String = ADDRESS.replace(text) { redacted(it.value) }

private val ADDRESS = Regex("[a-zA-Z][a-zA-Z0-9+.-]*://[^\\s\"']+")
