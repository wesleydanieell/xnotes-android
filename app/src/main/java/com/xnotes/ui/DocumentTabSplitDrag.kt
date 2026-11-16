package com.xnotes.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.xnotes.settings.ToolbarPosition

internal val DocumentTabMoveArea = 24.dp

enum class DocumentTabSplitSide(val sideBySide: Boolean, val first: Boolean) {
    LEFT(true, true), RIGHT(true, false), TOP(false, true), BOTTOM(false, false);

    fun preview(workspace: Rect): Rect = when (this) {
        LEFT -> Rect(workspace.left, workspace.top, workspace.center.x, workspace.bottom)
        RIGHT -> Rect(workspace.center.x, workspace.top, workspace.right, workspace.bottom)
        TOP -> Rect(workspace.left, workspace.top, workspace.right, workspace.center.y)
        BOTTOM -> Rect(workspace.left, workspace.center.y, workspace.right, workspace.bottom)
    }
}

class DocumentTabSplitDrag internal constructor(
    private val workspace: Rect,
    private val strip: Rect,
    private val edge: ToolbarPosition,
    private val threshold: Float,
    internal val onPreview: (DocumentTabSplitSide?) -> Unit,
    internal val onDrop: (String, DocumentTabSplitSide) -> Unit,
) {
    internal fun canReorder(point: Offset): Boolean = point != Offset.Unspecified && strip.contains(point)

    internal fun target(point: Offset): DocumentTabSplitSide? {
        if (point == Offset.Unspecified || workspace.isEmpty || !workspace.contains(point)) return null
        val inward = when (edge) {
            ToolbarPosition.TOP -> point.y - strip.bottom
            ToolbarPosition.BOTTOM -> strip.top - point.y
            ToolbarPosition.LEFT -> point.x - strip.right
            ToolbarPosition.RIGHT -> strip.left - point.x
        }
        if (inward < threshold) return null

        val x = (point.x - workspace.left) / workspace.width
        val y = (point.y - workspace.top) / workspace.height
        return when (minOf(x, 1f - x, y, 1f - y)) {
            x -> DocumentTabSplitSide.LEFT
            1f - x -> DocumentTabSplitSide.RIGHT
            y -> DocumentTabSplitSide.TOP
            else -> DocumentTabSplitSide.BOTTOM
        }
    }
}
