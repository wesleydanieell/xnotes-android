package com.xnotes.core.tools

/**
 * Pure snap-grid decision shared by the real toolbar surface and the configuration preview.
 * Coordinates are relative to the safe pane, never to the entire screen.
 */
object ToolbarAnchorResolver {
    fun nearest(
        centerX: Float,
        centerY: Float,
        paneWidth: Float,
        paneHeight: Float,
    ): ToolbarAnchor? {
        if (!centerX.isFinite() || !centerY.isFinite() ||
            !paneWidth.isFinite() || !paneHeight.isFinite() ||
            paneWidth <= 0f || paneHeight <= 0f
        ) return null

        val x = centerX.coerceIn(0f, paneWidth)
        val y = centerY.coerceIn(0f, paneHeight)
        val column = when {
            x < paneWidth / 3f -> 0
            x > paneWidth * 2f / 3f -> 2
            else -> 1
        }
        val row = when {
            y < paneHeight / 3f -> 0
            y > paneHeight * 2f / 3f -> 2
            else -> 1
        }
        return ToolbarAnchor.entries[row * 3 + column]
    }
}
