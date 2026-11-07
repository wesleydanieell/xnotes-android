package com.xnotes.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/** Selection actions stay inside their pane and prefer free space above/below the selection. */
internal fun placeCanvasActionBar(selection: Rect, viewport: Size, requested: Size,
    obstacles: List<Rect>, gap: Float): Rect {
    val margin = gap.coerceAtLeast(0f).coerceAtMost(minOf(viewport.width, viewport.height).coerceAtLeast(0f) / 2)
    val width = requested.width.coerceIn(0f, (viewport.width - 2 * margin).coerceAtLeast(0f))
    val height = requested.height.coerceIn(0f, (viewport.height - 2 * margin).coerceAtLeast(0f))
    val maxX = (viewport.width - margin - width).coerceAtLeast(margin)
    val maxY = (viewport.height - margin - height).coerceAtLeast(margin)
    fun x(value: Float) = value.coerceIn(margin, maxX)
    fun y(value: Float) = value.coerceIn(margin, maxY)
    val above = selection.top - gap - height
    val preferred = Offset(x(selection.center.x - width / 2), y(if (above >= margin) above else selection.bottom + gap))
    val occupied = obstacles.filter { it.overlaps(Rect(Offset.Zero, viewport)) }
    val xs = (listOf(preferred.x, margin, maxX) + occupied.flatMap { listOf(x(it.left - gap - width), x(it.right + gap)) }).distinct()
    val ys = (listOf(preferred.y, y(above), y(selection.bottom + gap), margin, maxY) + occupied.flatMap {
        listOf(y(it.top - gap - height), y(it.bottom + gap))
    }).distinct()
    val candidates = xs.flatMap { a -> ys.map { b -> Rect(Offset(a, b), Size(width, height)) } }
        .sortedBy { (it.topLeft - preferred).getDistanceSquared() }
    return candidates.firstOrNull { r -> !r.overlaps(selection) && occupied.none { it.overlaps(r) } }
        ?: candidates.firstOrNull { r -> occupied.none { it.overlaps(r) } }
        ?: Rect(preferred, Size(width, height))
}
