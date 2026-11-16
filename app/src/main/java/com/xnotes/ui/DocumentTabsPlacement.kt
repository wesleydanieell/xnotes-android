package com.xnotes.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarDockAlignment
import com.xnotes.settings.DocumentTabsBarConfig
import com.xnotes.settings.ToolbarPosition

/** Align a card within its selected free strip. */
internal fun documentTabsOffset(host: Size, card: Size, anchor: ToolbarAnchor): Offset {
    val x = (host.width - card.width).coerceAtLeast(0f)
    val y = (host.height - card.height).coerceAtLeast(0f)
    return Offset(
        when (anchor) {
            ToolbarAnchor.TOP_LEFT, ToolbarAnchor.CENTER_LEFT, ToolbarAnchor.BOTTOM_LEFT -> 0f
            ToolbarAnchor.TOP_RIGHT, ToolbarAnchor.CENTER_RIGHT, ToolbarAnchor.BOTTOM_RIGHT -> x
            else -> x / 2
        },
        when (anchor) {
            ToolbarAnchor.TOP_LEFT, ToolbarAnchor.TOP_CENTER, ToolbarAnchor.TOP_RIGHT -> 0f
            ToolbarAnchor.BOTTOM_LEFT, ToolbarAnchor.BOTTOM_CENTER, ToolbarAnchor.BOTTOM_RIGHT -> y
            else -> y / 2
        },
    )
}

/** Resolve free space along the requested edge. An occupied edge never shifts the
 * tab bar across the editor. The caller rejects overlapping interactive drops. */
internal fun documentTabsFloatingStrip(
    host: Size,
    anchor: ToolbarAnchor,
    edge: ToolbarPosition,
    obstacles: List<Rect>,
    px: Float,
    keepCentered: Boolean = false,
): Rect {
    val vertical = edge.vertical
    val main = if (vertical) host.height else host.width
    val cross = if (vertical) host.width else host.height
    val gap = 8f * px
    val start = gap.coerceAtMost(minOf(main, cross).coerceAtLeast(0f) / 2)
    val end = (main - start).coerceAtLeast(start)
    val depth = minOf(60f * px, (cross - 2 * start).coerceAtLeast(0f))
    val crossStart = when (edge) {
        ToolbarPosition.TOP, ToolbarPosition.LEFT -> start
        ToolbarPosition.BOTTOM, ToolbarPosition.RIGHT -> (cross - start - depth).coerceAtLeast(start)
    }
    fun strip(a: Float, b: Float) = if (vertical) Rect(crossStart, a, crossStart + depth, b)
        else Rect(a, crossStart, b, crossStart + depth)
    val edgeStrip = strip(start, end)
    if (obstacles.isEmpty()) return edgeStrip

    val alignment = DocumentTabsBarConfig(position = edge, anchor = anchor).alignment()
    val mainAnchor = when (alignment) {
        ToolbarDockAlignment.START -> start
        ToolbarDockAlignment.CENTER -> main / 2
        ToolbarDockAlignment.END -> end
    }
    val blockers = obstacles.mapNotNull { obstacle ->
        val crossLow = if (vertical) obstacle.left else obstacle.top
        val crossHigh = if (vertical) obstacle.right else obstacle.bottom
        if (crossLow >= crossStart + depth + gap || crossHigh <= crossStart - gap) null
        else {
            val a = if (vertical) obstacle.top else obstacle.left
            val b = if (vertical) obstacle.bottom else obstacle.right
            (a - gap).coerceIn(start, end) to (b + gap).coerceIn(start, end)
        }
    }.sortedBy { it.first }

    val intervals = mutableListOf<Pair<Float, Float>>()
    var cursor = start
    for ((a, b) in blockers) {
        if (a > cursor) intervals.add(cursor to a)
        cursor = maxOf(cursor, b)
    }
    if (cursor < end) intervals.add(cursor to end)
    val free = intervals.minWithOrNull(compareBy<Pair<Float, Float>> {
        kotlin.math.abs(mainAnchor - mainAnchor.coerceIn(it.first, it.second))
    }.thenByDescending { it.second - it.first }) ?: return edgeStrip

    val (from, to) = if (keepCentered && alignment == ToolbarDockAlignment.CENTER) {
        val room = minOf(mainAnchor - free.first, free.second - mainAnchor)
        if (room <= 0) return edgeStrip
        mainAnchor - room to mainAnchor + room
    } else free
    return if (to - from >= minOf(132f * px, end - start)) strip(from, to) else edgeStrip
}
