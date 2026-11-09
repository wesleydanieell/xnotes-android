package com.xnotes.ui

import com.xnotes.core.tools.*
import com.xnotes.settings.ToolbarLook

internal data class ToolbarBarMove(val profiles: ToolbarWorkspaces, val look: ToolbarLook)

/** Geometry-only change: never carries a captured Preferences snapshot through a drag. */
internal fun movedToolbarBar(profiles: ToolbarWorkspaces, look: ToolbarLook,
    context: ToolbarContext, id: String, anchor: ToolbarAnchor): ToolbarBarMove? {
    val workspace = if (context == ToolbarContext.NOTE || profiles.shared) profiles.note else profiles.canvas
    val bar = workspace.bars.firstOrNull { it.id == id && it.geometry.movable } ?: return null
    val isPrimary = workspace.bars.firstOrNull()?.id == id
    // An edge drop keeps a docked primary docked. Center/corner drops float it.
    val dockPosition = if (isPrimary && !bar.geometry.floating) when (anchor) {
        ToolbarAnchor.TOP_CENTER -> com.xnotes.settings.ToolbarPosition.TOP
        ToolbarAnchor.BOTTOM_CENTER -> com.xnotes.settings.ToolbarPosition.BOTTOM
        ToolbarAnchor.CENTER_LEFT -> com.xnotes.settings.ToolbarPosition.LEFT
        ToolbarAnchor.CENTER_RIGHT -> com.xnotes.settings.ToolbarPosition.RIGHT
        else -> null
    } else null
    val moved = bar.copy(geometry = bar.geometry.movedTo(anchor, floating = dockPosition == null))
    if (moved == bar && (bar.geometry.floating || dockPosition != null && look.position == dockPosition)) return null
    val changed = workspace.replaceBar(moved)
    val nextLook = if (dockPosition != null) look.copy(floating = false, position = dockPosition) else look
    return ToolbarBarMove(profiles.updateWorkspace(if (profiles.shared) ToolbarContext.NOTE else context) { changed }, nextLook)
}
