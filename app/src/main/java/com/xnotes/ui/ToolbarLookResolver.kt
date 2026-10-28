package com.xnotes.ui

import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarScale
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import com.xnotes.settings.ToolbarSize

/**
 * Projects a persisted bar geometry into the legacy look consumed by the common renderer.
 *
 * The old renderer has one [ToolbarLook], while workspaces now have one geometry per primary
 * bar. Keeping this projection pure lets note and canvas panes render independently and preserves
 * the legacy look as a fallback when a workspace has no primary bar yet.
 */
internal fun toolbarLookFor(base: ToolbarLook, primary: ToolbarBar?): ToolbarLook {
    val geometry = primary?.geometry ?: return base
    return base.copy(
        position = geometry.anchor.toolbarPosition(),
        size = geometry.scale.toolbarSize(),
        floating = geometry.floating,
    )
}

internal fun ToolbarAnchor.toolbarPosition(): ToolbarPosition = when (this) {
    ToolbarAnchor.CENTER_LEFT -> ToolbarPosition.LEFT
    ToolbarAnchor.CENTER_RIGHT -> ToolbarPosition.RIGHT
    ToolbarAnchor.BOTTOM_LEFT,
    ToolbarAnchor.BOTTOM_CENTER,
    ToolbarAnchor.BOTTOM_RIGHT,
    -> ToolbarPosition.BOTTOM
    else -> ToolbarPosition.TOP
}

internal fun ToolbarScale.toolbarSize(): ToolbarSize = when (this) {
    ToolbarScale.COMPACT -> ToolbarSize.COMPACT
    ToolbarScale.REGULAR -> ToolbarSize.REGULAR
    ToolbarScale.COMFORTABLE -> ToolbarSize.COMFORTABLE
}
