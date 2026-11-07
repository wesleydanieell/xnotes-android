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

/** Physical window bounds, independent of the strip's axis or dock edge. */
data class TabStripBounds(val left: Float, val top: Float, val width: Float, val height: Float)

data class TabStripGeometry(val bounds: TabStripBounds, val slotExtent: Float,
    val scrollOffset: Float, val vertical: Boolean = false)

/** Reordering follows the strip axis. Splitting works towards the document from either side of
 * the strip, including dragging up from a bottom bar and left from a right-hand rail. */
fun resolveTabStripDrop(fromIndex: Int, x: Float, y: Float, strip: TabStripGeometry,
    tabCount: Int, splitThreshold: Float, workspace: TabStripBounds? = null): TabDrop? {
    if (fromIndex !in 0 until tabCount || strip.slotExtent <= 0f) return null
    val b = strip.bounds
    val across = if (strip.vertical) x else y
    val crossStart = if (strip.vertical) b.left else b.top
    val crossSize = if (strip.vertical) b.width else b.height
    val offStrip = across < crossStart - splitThreshold || across >= crossStart + crossSize + splitThreshold
    if (offStrip && workspace != null && tabCount >= 2) {
        if (workspace.width <= 0f || workspace.height <= 0f ||
            x < workspace.left || x > workspace.left + workspace.width ||
            y < workspace.top || y > workspace.top + workspace.height) return null
        return paneDropSide(x - workspace.left, y - workspace.top,
            workspace.width, workspace.height)?.let(TabDrop::Split)
    }
    return resolveTabDrop(fromIndex,
        x = if (strip.vertical) y else x, y = across,
        barLeft = if (strip.vertical) b.top else b.left, barTop = crossStart,
        barWidth = if (strip.vertical) b.height else b.width, barHeight = crossSize,
        slotWidth = strip.slotExtent, scrollOffset = strip.scrollOffset,
        tabCount = tabCount, splitThreshold = splitThreshold)
}
