package com.xnotes.core.workspace

/** Drop coordinates are expressed in window pixels; drag detection is UI-framework independent. */
sealed interface TabDrop {
    data class Reorder(val from: Int, val to: Int) : TabDrop
    data class Split(val onRight: Boolean) : TabDrop
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
): TabDrop? {
    if (fromIndex !in 0 until tabCount || slotWidth <= 0f || barWidth <= 0f) return null
    if (y >= barTop + barHeight + splitThreshold) {
        return if (tabCount >= 2) TabDrop.Split(onRight = x >= barLeft + barWidth / 2f) else null
    }
    if (y < barTop - splitThreshold) return null
    val contentX = x - barLeft + scrollOffset
    val to = (contentX / slotWidth).toInt().coerceIn(0, tabCount - 1)
    return if (to == fromIndex) null else TabDrop.Reorder(fromIndex, to)
}
