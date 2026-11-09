package com.xnotes.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.settings.DocumentTabsBarConfig
import com.xnotes.settings.ToolbarPosition

/** Float within the window, shifting inward past real toolbars rather than joining them. */
internal fun documentTabsOffset(host: Size, card: Size, anchor: ToolbarAnchor,
    obstacles: List<Rect>, gap: Float): Offset {
    val w = card.width.coerceAtMost(host.width).coerceAtLeast(0f)
    val h = card.height.coerceAtMost(host.height).coerceAtLeast(0f)
    val maxX = (host.width - w).coerceAtLeast(0f)
    val maxY = (host.height - h).coerceAtLeast(0f)
    val x = when (anchor) {
        ToolbarAnchor.TOP_LEFT, ToolbarAnchor.CENTER_LEFT, ToolbarAnchor.BOTTOM_LEFT -> gap
        ToolbarAnchor.TOP_RIGHT, ToolbarAnchor.CENTER_RIGHT, ToolbarAnchor.BOTTOM_RIGHT -> maxX - gap
        else -> maxX / 2f
    }.coerceIn(0f, maxX)
    val y = when (anchor) {
        ToolbarAnchor.TOP_LEFT, ToolbarAnchor.TOP_CENTER, ToolbarAnchor.TOP_RIGHT -> gap
        ToolbarAnchor.BOTTOM_LEFT, ToolbarAnchor.BOTTOM_CENTER, ToolbarAnchor.BOTTOM_RIGHT -> maxY - gap
        else -> maxY / 2f
    }.coerceIn(0f, maxY)
    val origin = Offset(x, y)
    val candidates = mutableListOf(origin)
    val visited = mutableSetOf<Pair<Int, Int>>()
    repeat((obstacles.size * 8 + 8).coerceAtMost(256)) {
        if (candidates.isEmpty()) return origin
        val index = candidates.indices.minByOrNull { (candidates[it] - origin).getDistanceSquared() } ?: return origin
        val current = candidates.removeAt(index)
        val overlaps = obstacles.filter { it.overlaps(Rect(current, Size(w, h))) }
        if (overlaps.isEmpty()) return current
        for (obstacle in overlaps) {
            val moves = listOf(Offset(obstacle.right + gap, current.y),
                Offset(obstacle.left - w - gap, current.y),
                Offset(current.x, obstacle.bottom + gap), Offset(current.x, obstacle.top - h - gap))
            for (next in moves) if (next.x in 0f..maxX && next.y in 0f..maxY &&
                visited.add(next.x.toInt() to next.y.toInt())) candidates.add(next)
        }
    }
    return origin
}

/** Longest usable strip near the requested edge. Swapping axes also handles side rails. */
internal fun documentTabsFreeStrip(host: Size, anchor: ToolbarAnchor, vertical: Boolean,
    crossExtent: Float, obstacles: List<Rect>, gap: Float, minimumExtent: Float): Rect {
    val main = if (vertical) host.height else host.width
    val cross = if (vertical) host.width else host.height
    val margin = gap.coerceAtMost(minOf(main, cross).coerceAtLeast(0f) / 2)
    val start = margin
    val end = (main - margin).coerceAtLeast(start)
    val thickness = crossExtent.coerceIn(0f, (cross - 2 * margin).coerceAtLeast(0f))
    val crossLimit = (cross - margin - thickness).coerceAtLeast(margin)
    val edge = DocumentTabsBarConfig.edgeOf(anchor)
    val desired = when (edge) {
        ToolbarPosition.TOP, ToolbarPosition.LEFT -> margin
        else -> crossLimit
    }
    fun crossStart(r: Rect) = if (vertical) r.left else r.top
    fun crossEnd(r: Rect) = if (vertical) r.right else r.bottom
    fun mainStart(r: Rect) = if (vertical) r.top else r.left
    fun mainEnd(r: Rect) = if (vertical) r.bottom else r.right
    fun rect(a: Float, b: Float, c: Float) = if (vertical) Rect(c, a, c + thickness, b)
        else Rect(a, c, b, c + thickness)
    val candidates = (listOf(desired, margin, crossLimit) + obstacles.flatMap {
        listOf(crossEnd(it) + gap, crossStart(it) - gap - thickness)
    }).filter { it in margin..crossLimit }.distinct().sortedBy { kotlin.math.abs(it - desired) }
    var fallback: Rect? = null
    for (c in candidates) {
        val blockers = obstacles.filter { crossStart(it) < c + thickness + gap && crossEnd(it) > c - gap }
            .map { (mainStart(it) - gap).coerceIn(start, end) to (mainEnd(it) + gap).coerceIn(start, end) }
            .sortedBy { it.first }
        val intervals = mutableListOf<Pair<Float, Float>>()
        var cursor = start
        for ((a, b) in blockers) { if (a > cursor) intervals.add(cursor to a); cursor = maxOf(cursor, b) }
        if (cursor < end) intervals.add(cursor to end)
        val mainAnchor = if (vertical) main / 2 else when (anchor) {
            ToolbarAnchor.TOP_LEFT, ToolbarAnchor.BOTTOM_LEFT -> start
            ToolbarAnchor.TOP_RIGHT, ToolbarAnchor.BOTTOM_RIGHT -> end
            else -> main / 2
        }
        val free = intervals.minWithOrNull(compareBy<Pair<Float, Float>> {
            kotlin.math.abs(mainAnchor - mainAnchor.coerceIn(it.first, it.second))
        }.thenByDescending { it.second - it.first }) ?: continue
        val result = rect(free.first, free.second, c)
        val extent = free.second - free.first
        val previous = fallback?.let { if (vertical) it.height else it.width } ?: -1f
        if (extent > previous) fallback = result
        if (extent >= minimumExtent.coerceAtMost(end - start)) return result
    }
    return fallback ?: rect(start, end, desired)
}

/** Clearance for the pane's top-center drag handle, using window-coordinate obstacles. */
internal fun paneGripClearance(pane: Rect, obstacles: List<Rect>, width: Float, height: Float, gap: Float): Float {
    if (pane.isEmpty) return gap
    val left = pane.center.x - width / 2f
    val right = left + width
    var top = pane.top + gap
    // Walk down through overlapping rows, independent of obstacle registration order.
    repeat(obstacles.size + 1) {
        val bottom = obstacles.filter { it.left < right && it.right > left &&
            it.top < top + height && it.bottom > top }.maxOfOrNull { it.bottom } ?: return top - pane.top
        val next = bottom + gap
        if (next + height > pane.bottom) return (pane.height - height).coerceAtLeast(0f)
        top = next
    }
    return top - pane.top
}
