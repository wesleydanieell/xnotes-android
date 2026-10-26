package com.xnotes.core.workspace

/** Drop coordinates are expressed in window pixels; drag detection is UI-framework independent. */
sealed interface TabDrop {
    data class Reorder(val from: Int, val to: Int) : TabDrop
    data class Split(val onRight: Boolean) : TabDrop
}

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
        return TabDrop.Split(onRight = x >= barLeft + barWidth / 2f)
    }
    if (y < barTop - splitThreshold) return null
    val contentX = x - barLeft + scrollOffset
    val to = (contentX / slotWidth).toInt().coerceIn(0, tabCount - 1)
    return if (to == fromIndex) null else TabDrop.Reorder(fromIndex, to)
}
