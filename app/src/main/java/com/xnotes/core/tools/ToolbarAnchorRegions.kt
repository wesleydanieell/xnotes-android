package com.xnotes.core.tools

/** A region in pane coordinates; units are supplied by the caller. */
internal data class ToolbarAnchorRegion(val x: Float, val y: Float, val width: Float, val height: Float)

/**
 * Divide an axis into thirds only when multiple anchor columns or rows are in use.
 * Anchors in the same cell share a stack; disjoint cells cannot overlap. The unused
 * middle third remains free when only opposite edges are occupied.
 */
internal fun toolbarAnchorRegions(
    anchors: Set<ToolbarAnchor>, width: Float, height: Float,
): Map<ToolbarAnchor, ToolbarAnchorRegion> {
    if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) return emptyMap()
    val columns = anchors.map { it.ordinal % 3 }.toSet()
    val rows = anchors.map { it.ordinal / 3 }.toSet()
    val splitX = columns.size > 1
    val splitY = rows.size > 1
    return anchors.associateWith { anchor ->
        val column = anchor.ordinal % 3
        val row = anchor.ordinal / 3
        val cellWidth = if (splitX) width / 3f else width
        val cellHeight = if (splitY) height / 3f else height
        ToolbarAnchorRegion(
            x = if (splitX) column * cellWidth else 0f,
            y = if (splitY) row * cellHeight else 0f,
            width = cellWidth,
            height = cellHeight,
        )
    }
}
