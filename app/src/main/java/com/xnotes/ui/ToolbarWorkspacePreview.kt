package com.xnotes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

/**
 * A deliberately small note-like stage for configuring layouts: it previews actual
 * visible tool order and each toolbar's docking/anchor. Tapping selects a bar;
 * holding and dragging uses exactly the same snap logic as the editor.
 */
@Composable
internal fun ToolbarWorkspacePreview(
    workspace: ToolbarWorkspace,
    look: ToolbarLook,
    selectedId: String,
    onSelectBar: (String) -> Unit,
    onMoveBar: (String, ToolbarAnchor) -> Unit,
) {
    val palette = LocalPalette.current
    var stageBounds by remember { mutableStateOf(Rect.Zero) }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(228.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFAFAFD))
            .border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(14.dp))
            .onGloballyPositioned { stageBounds = it.boundsInRoot() },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val inset = 14.dp.toPx()
            val rule = 30.dp.toPx()
            var y = 54.dp.toPx()
            while (y < size.height - inset) {
                drawLine(Color(0xFFE8EDF5), androidx.compose.ui.geometry.Offset(inset, y),
                    androidx.compose.ui.geometry.Offset(size.width - inset, y), 1.dp.toPx())
                y += rule
            }
        }
        workspace.bars.forEachIndexed { index, bar ->
            if (!bar.visible) return@forEachIndexed
            val floating = index != 0 || bar.geometry.floating || look.floating
            val anchor = if (index == 0 && !bar.geometry.floating) when (look.position) {
                ToolbarPosition.TOP -> ToolbarAnchor.TOP_CENTER
                ToolbarPosition.BOTTOM -> ToolbarAnchor.BOTTOM_CENTER
                ToolbarPosition.LEFT -> ToolbarAnchor.CENTER_LEFT
                ToolbarPosition.RIGHT -> ToolbarAnchor.CENTER_RIGHT
            } else bar.geometry.anchor
            val vertical = anchor == ToolbarAnchor.CENTER_LEFT || anchor == ToolbarAnchor.CENTER_RIGHT
            // A fixed bar has its own edge slot; floating bars are inset from the paper edge.
            Box(
                Modifier
                    .align(anchor.alignment())
                    .padding(if (floating) 9.dp else 1.dp),
            ) {
                ToolbarDragSurface(
                    id = bar.id,
                    anchor = anchor,
                    movable = bar.geometry.movable,
                    paneBounds = stageBounds,
                    onMoveBar = onMoveBar,
                ) {
                    PreviewBar(
                        bar = bar,
                        selected = bar.id == selectedId,
                        vertical = vertical,
                        onSelect = { onSelectBar(bar.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewBar(
    bar: ToolbarBar,
    selected: Boolean,
    vertical: Boolean,
    onSelect: () -> Unit,
) {
    val palette = LocalPalette.current
    val entries = bar.layout.visibleSections.flatMap { it.visibleEntries }.map { it.item }
    val shown = entries.take(if (vertical) 5 else 9)
    val extras = entries.size - shown.size
    val surface = Modifier
        .clip(RoundedCornerShape(9.dp))
        .background(palette.panel.toComposeColor())
        .border(
            if (selected) 2.dp else 1.dp,
            (if (selected) palette.accent else palette.border).toComposeColor(),
            RoundedCornerShape(9.dp),
        )
        .clickable(onClick = onSelect)
        .padding(horizontal = 6.dp, vertical = 5.dp)
    if (vertical) {
        Column(surface, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            for (item in shown) PreviewTool(item)
            if (extras > 0) Text("+$extras", fontSize = 9.sp, color = palette.textDim.toComposeColor())
        }
    } else {
        Row(surface, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (item in shown) PreviewTool(item)
            if (extras > 0) Text("+$extras", fontSize = 9.sp, color = palette.textDim.toComposeColor())
        }
    }
}

@Composable
private fun PreviewTool(item: ToolbarItem) {
    val palette = LocalPalette.current
    Box(Modifier.size(21.dp), contentAlignment = Alignment.Center) {
        if (item == ToolbarItem.COLORS) {
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                for (color in listOf(0xFF222222, 0xFF4986D4, 0xFFEF727A)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Color(color)))
                }
            }
        } else {
            Icon(itemIcon(item), contentDescription = null, modifier = Modifier.size(14.dp),
                tint = palette.text.toComposeColor())
        }
    }
}
