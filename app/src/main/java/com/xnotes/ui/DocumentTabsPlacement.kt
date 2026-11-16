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

/** Prefer the requested edge and clear only as much toolbar depth as needed. */
internal fun documentTabsFloatingStrip(
    host: Size,
    anchor: ToolbarAnchor,
    edge: ToolbarPosition,
    obstacles: List<Rect>,
    px: Float,
    keepCentered: Boolean = false,
    allowInset: Boolean = true,
): Rect {
    val vertical = edge.vertical
    val main = if (vertical) host.height else host.width
    val cross = if (vertical) host.width else host.height
    val gap = 8f * px
    val start = gap.coerceAtMost(minOf(main, cross).coerceAtLeast(0f) / 2)
    val end = (main - start).coerceAtLeast(start)
    val depth = minOf(60f * px, (cross - 2 * start).coerceAtLeast(0f))
    val reverse = edge == ToolbarPosition.BOTTOM || edge == ToolbarPosition.RIGHT
    fun strip(a: Float, b: Float, inset: Float): Rect {
        val c = if (reverse) cross - inset - depth else inset
        return if (vertical) Rect(c, a, c + depth, b) else Rect(a, c, b, c + depth)
    }

    val edgeStrip = strip(start, end, start)
    if (obstacles.isEmpty()) return edgeStrip

    val alignment = DocumentTabsBarConfig(position = edge, anchor = anchor).alignment()
    val mainAnchor = when (alignment) {
        ToolbarDockAlignment.START -> start
        ToolbarDockAlignment.CENTER -> main / 2
        ToolbarDockAlignment.END -> end
    }
    val centered = keepCentered && alignment == ToolbarDockAlignment.CENTER
    val minimum = minOf(132f * px, end - start)
    val insets = if (allowInset) {
        (listOf(start) + obstacles.map { obstacle ->
            val near = if (vertical) obstacle.left else obstacle.top
            val far = if (vertical) obstacle.right else obstacle.bottom
            (if (reverse) cross - near else far) + gap
        }.filter { it > start && it < cross / 2 && it + depth <= cross - start }).distinct().sorted()
    } else listOf(start)

    for (inset in insets) {
        val edgeBand = strip(start, end, inset)
        val blockers = obstacles.asSequence().filter { it.inflate(gap).overlaps(edgeBand) }
            .map { obstacle ->
                val a = if (vertical) obstacle.top else obstacle.left
                val b = if (vertical) obstacle.bottom else obstacle.right
                (a - gap).coerceIn(start, end) to (b + gap).coerceIn(start, end)
            }.sortedBy { it.first }
        val intervals = mutableListOf<Pair<Float, Float>>()
        var cursor = start
        for ((a, b) in blockers) {
            if (a > cursor) intervals.add(cursor to a)
            cursor = maxOf(cursor, b)
        }
        if (cursor < end) intervals.add(cursor to end)
        val free = intervals.asSequence().map { (a, b) ->
            if (centered) {
                val room = minOf(mainAnchor - a, b - mainAnchor)
                mainAnchor - room to mainAnchor + room
            } else a to b
        }.filter { (a, b) -> b - a >= minimum }.minWithOrNull(
            compareBy<Pair<Float, Float>> {
                kotlin.math.abs(mainAnchor - mainAnchor.coerceIn(it.first, it.second))
            }.thenByDescending { it.second - it.first },
        )
        if (free != null) return strip(free.first, free.second, inset)
    }

    // Keep navigation accessible when the viewport is too crowded to fit another surface.
    return edgeStrip
}
