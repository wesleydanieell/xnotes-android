package com.xnotes.core.tools

/** Resolve the pointer's drop position within a pane to the nearest 3×3 region. */
internal fun toolbarDropTarget(x: Float, y: Float, width: Float, height: Float): ToolbarAnchor? {
    if (!x.isFinite() || !y.isFinite() || !width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) return null
    val column = when {
        x < width / 3f -> 0
        x > width * 2f / 3f -> 2
        else -> 1
    }
    val row = when {
        y < height / 3f -> 0
        y > height * 2f / 3f -> 2
        else -> 1
    }
    return ToolbarAnchor.entries[row * 3 + column]
}
