package com.xnotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarScale
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import com.xnotes.settings.ToolbarSize

/**
 * First multi-bar renderer: the existing primary bar stays in ToolbarAround; additional bars
 * float over the current pane. Group bars sharing an anchor so they stack instead of covering
 * each other. This host never consumes ink outside the bars' bounds.
 */
@Composable
fun BoxScope.WorkspaceFloatingBars(
    workspace: ToolbarWorkspace,
    content: @Composable (ToolbarBar) -> Unit,
) {
    for (anchor in ToolbarAnchor.entries) {
        val bars = workspace.bars.drop(1).filter { it.visible && it.geometry.anchor == anchor }
        if (bars.isEmpty()) continue
        Column(
            modifier = Modifier.align(anchor.alignment()).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (bar in bars) {
                key(bar.id) {
                    val look = ToolbarLook(
                        position = anchor.toolbarPosition(),
                        size = when (bar.geometry.scale) {
                            ToolbarScale.COMPACT -> ToolbarSize.COMPACT
                            ToolbarScale.REGULAR -> ToolbarSize.REGULAR
                            ToolbarScale.COMFORTABLE -> ToolbarSize.COMFORTABLE
                        },
                        floating = true,
                    )
                    CompositionLocalProvider(LocalToolbarLook provides look) {
                        content(bar)
                    }
                }
            }
        }
    }
}

private fun ToolbarAnchor.alignment(): Alignment = when (this) {
    ToolbarAnchor.TOP_LEFT -> AbsoluteAlignment.TopLeft
    ToolbarAnchor.TOP_CENTER -> Alignment.TopCenter
    ToolbarAnchor.TOP_RIGHT -> AbsoluteAlignment.TopRight
    ToolbarAnchor.CENTER_LEFT -> AbsoluteAlignment.CenterLeft
    ToolbarAnchor.CENTER -> Alignment.Center
    ToolbarAnchor.CENTER_RIGHT -> AbsoluteAlignment.CenterRight
    ToolbarAnchor.BOTTOM_LEFT -> AbsoluteAlignment.BottomLeft
    ToolbarAnchor.BOTTOM_CENTER -> Alignment.BottomCenter
    ToolbarAnchor.BOTTOM_RIGHT -> AbsoluteAlignment.BottomRight
}

private fun ToolbarAnchor.toolbarPosition(): ToolbarPosition = when (this) {
    ToolbarAnchor.CENTER_LEFT -> ToolbarPosition.LEFT
    ToolbarAnchor.CENTER_RIGHT -> ToolbarPosition.RIGHT
    ToolbarAnchor.BOTTOM_LEFT, ToolbarAnchor.BOTTOM_CENTER, ToolbarAnchor.BOTTOM_RIGHT -> ToolbarPosition.BOTTOM
    else -> ToolbarPosition.TOP
}
