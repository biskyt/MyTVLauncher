package com.biskyt.mytvlauncher.core.watchnext

/**
 * The part of a TV channel's logo that is not transparent (the owner, 2026-11-15), from [left] and [top] to [right] and
 * [bottom] (exclusive), so that the logo's transparent border can be cut off.
 */
data class LogoLook(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    companion object {
        /** A pixel with less alpha than this (of 255) counts as transparent. */
        private const val SEEN_ALPHA = 24

        /**
         * Looks at a picture's ARGB [pixels] ([width] by [height], row by row). Null for a picture that is all
         * transparent.
         */
        fun of(pixels: IntArray, width: Int, height: Int): LogoLook? {
            var left = width
            var top = height
            var right = -1
            var bottom = -1
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (pixels[y * width + x] ushr 24 < SEEN_ALPHA) continue
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
            if (right < 0) return null
            return LogoLook(left, top, right + 1, bottom + 1)
        }
    }
}
