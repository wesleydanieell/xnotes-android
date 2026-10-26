package com.xnotes.core.workspace

/** Drop coordinates are expressed in window pixels; drag detection is UI-framework independent. */
enum class SplitSide { LEFT, RIGHT, TOP, BOTTOM }

sealed interface TabDrop {
    data class Reorder(val from: Int, val to: Int) : TabDrop
    data class Split(val side: SplitSide) : TabDrop
}

/** Boundary in the original strip that corresponds to the target after removal. */
fun insertionBoundary(from: Int, to: Int): Int = if (to > from) to + 1 else to

fun resolveTabDrop(
    fromIndex: Int,
    x: Float,
    y: Float,
    barLeft: Float,
    barTop: Float,
    barWidth: Float,
    barHeight: Float,
    slotWidth: Float,
    scrollOffset: Float,
    tabCount: Int,
    splitThreshold: Float,
    workspaceBottom: Float = barTop + barHeight + barWidth,
): TabDrop? {
    if (fromIndex !in 0 until tabCount || slotWidth <= 0f || barWidth <= 0f) return null
    if (y >= barTop + barHeight + splitThreshold) {
        if (tabCount < 2) return null
        // Four intentional drop zones: near either side edge, or near the top/bottom of
        // the editor area. The centre remains a left/right target for familiar gestures.
        val horizontal = ((x - barLeft) / barWidth).coerceIn(0f, 1f)
        val areaTop = barTop + barHeight
        val vertical = ((y - areaTop) / (workspaceBottom - areaTop).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val side = when {
            horizontal <= 0.2f -> SplitSide.LEFT
            horizontal >= 0.8f -> SplitSide.RIGHT
            vertical <= 0.33f -> SplitSide.TOP
            vertical >= 0.67f -> SplitSide.BOTTOM
            horizontal < 0.5f -> SplitSide.LEFT
            else -> SplitSide.RIGHT
        }
        return TabDrop.Split(side)
    }
    if (y < barTop - splitThreshold) return null
    val contentX = x - barLeft + scrollOffset
    val to = (contentX / slotWidth).toInt().coerceIn(0, tabCount - 1)
    return if (to == fromIndex) null else TabDrop.Reorder(fromIndex, to)
}
