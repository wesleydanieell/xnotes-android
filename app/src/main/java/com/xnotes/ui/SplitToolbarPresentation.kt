package com.xnotes.ui

import com.xnotes.core.tools.ToolbarItem
import com.xnotes.core.tools.ToolbarScale
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarSize

internal data class SplitToolbarPresentation(val workspace: ToolbarWorkspace, val look: ToolbarLook)

/** Display-only adaptation. Resizing/focusing a pane must never rewrite saved toolbar presets. */
internal fun splitToolbarPresentation(workspace: ToolbarWorkspace, look: ToolbarLook,
    widthDp: Float, heightDp: Float, adaptive: Boolean): SplitToolbarPresentation {
    if (!adaptive || minOf(widthDp, heightDp) >= 600f) return SplitToolbarPresentation(workspace, look)
    val bars = workspace.bars.map { bar ->
        bar.copy(geometry = bar.geometry.copy(scale = ToolbarScale.COMPACT,
            widthDp = bar.geometry.widthDp?.coerceAtMost(widthDp.toInt().coerceAtLeast(1)),
            heightDp = bar.geometry.heightDp?.coerceAtMost(heightDp.toInt().coerceAtLeast(1)),
            elementSizeDp = bar.geometry.elementSizeDp?.coerceAtMost(36),
            paddingDp = bar.geometry.paddingDp?.coerceAtMost(4), spacingDp = bar.geometry.spacingDp?.coerceAtMost(2)),
            layout = bar.layout.copy(sections = bar.layout.sections.map { section ->
                section.copy(entries = section.entries.map { entry ->
                    if (entry.item == ToolbarItem.SPACER) entry else entry.copy(sizeDp = entry.sizeDp?.coerceAtMost(36))
                })
            }))
    }
    return SplitToolbarPresentation(workspace.copy(bars = bars), look.copy(size = ToolbarSize.COMPACT))
}
