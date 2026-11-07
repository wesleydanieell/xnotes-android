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
