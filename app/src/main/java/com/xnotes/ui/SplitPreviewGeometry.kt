package com.xnotes.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xnotes.core.workspace.SplitSide

/** Destination region in a shared two-pane workspace, independent of drag source. */
internal data class SplitPreviewRect(val x: Dp, val y: Dp, val width: Dp, val height: Dp)

/**
 * Both tab dragging and pane-grip dragging preview the same destination slot.
 * A top toolbar is present in each pane, so vertical previews exclude it within
 * their own half, not across the whole workspace.
 */
internal fun splitPreviewRect(
    side: SplitSide,
    width: Dp,
    height: Dp,
    topClearance: Dp = 0.dp,
): SplitPreviewRect {
    val halfW = width / 2
    val halfH = height / 2
    return when (side) {
        SplitSide.LEFT -> SplitPreviewRect(0.dp, topClearance, halfW, (height - topClearance).coerceAtLeast(0.dp))
        SplitSide.RIGHT -> SplitPreviewRect(halfW, topClearance, halfW, (height - topClearance).coerceAtLeast(0.dp))
        SplitSide.TOP -> SplitPreviewRect(0.dp, topClearance, width, (halfH - topClearance).coerceAtLeast(0.dp))
        SplitSide.BOTTOM -> SplitPreviewRect(0.dp, halfH + topClearance, width, (halfH - topClearance).coerceAtLeast(0.dp))
    }
}
