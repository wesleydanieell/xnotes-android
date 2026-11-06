package com.xnotes.ui

import androidx.compose.ui.unit.Dp
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarTransferDraft
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.settings.ToolbarLook

/**
 * Find a legal destination for a copied toolbar. This is separate from the
 * settings UI so copy previews and actual copies use the same validated bar.
 * The source bar and destination workspace are never mutated.
 */
internal fun planCopiedToolbar(
    source: ToolbarBar,
    sourceIsPrimary: Boolean,
    destination: ToolbarWorkspace,
    draft: ToolbarTransferDraft,
    look: ToolbarLook,
    stageWidth: Dp,
    stageHeight: Dp,
    colorCount: Int,
): ToolbarTransferDraft? {
    if (draft.retainedCount == 0) return null
    // Primary bars inherit docking from the global look; copied bars do not.
    val geometry = if (sourceIsPrimary) {
        if (source.geometry.floating || look.floating)
            source.geometry.copy(floating = true, dockEdge = null)
        else source.geometry.copy(
            dockEdge = source.geometry.dockEdge ?: look.position.dockEdge(),
            floating = false,
        )
    } else source.geometry
    val candidates = listOf(geometry) + ToolbarAnchor.entries.filter { it != ToolbarAnchor.CENTER }
        .map { geometry.copy(floating = true, dockEdge = null, anchor = it, dockLane = 0) }
    for (position in candidates) {
        val candidate = destination.addBar(draft.bar.copy(geometry = position))
        if (toolbarPlacementFits(candidate, look, stageWidth, stageHeight, colorCount)) {
            return draft.copy(bar = draft.bar.copy(geometry = position))
        }
    }
    return null
}
