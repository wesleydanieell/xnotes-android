package com.xnotes.core.workspace

/** Which half of the workspace a pane grip is dropped in. Outside cancels the drop. */
fun paneDropSide(x: Float, y: Float, width: Float, height: Float): SplitSide? {
    if (width <= 0f || height <= 0f) return null
    val horizontal = x / width
    val vertical = y / height
    if (horizontal !in 0f..1f || vertical !in 0f..1f) return null
    val nearest = minOf(horizontal, 1f - horizontal, vertical, 1f - vertical)
    return when (nearest) {
        horizontal -> SplitSide.LEFT
        1f - horizontal -> SplitSide.RIGHT
        vertical -> SplitSide.TOP
        else -> SplitSide.BOTTOM
    }
}

/** True when changing the pane's half or the split orientation is necessary. */
fun panePlacementChanges(
    sourcePrimary: Boolean,
    currentlyHorizontal: Boolean,
    currentlySwapped: Boolean,
    destination: SplitSide,
): Boolean {
    val nextHorizontal = destination == SplitSide.LEFT || destination == SplitSide.RIGHT
    val nextFirst = destination == SplitSide.LEFT || destination == SplitSide.TOP
    val nextSwapped = if (sourcePrimary) !nextFirst else nextFirst
    return currentlyHorizontal != nextHorizontal || currentlySwapped != nextSwapped
}
