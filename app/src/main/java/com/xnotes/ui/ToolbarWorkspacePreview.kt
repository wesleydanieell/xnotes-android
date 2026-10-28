package com.xnotes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
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
import com.xnotes.core.tools.toolbarAnchorRegions
import com.xnotes.core.tools.resolveToolbarSize
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
        val placedBars = workspace.bars.mapIndexedNotNull { index, bar ->
            if (!bar.visible) return@mapIndexedNotNull null
            val floating = index != 0 || bar.geometry.floating || look.floating
            val anchor = if (index == 0 && !bar.geometry.floating) when (look.position) {
                ToolbarPosition.TOP -> ToolbarAnchor.TOP_CENTER
                ToolbarPosition.BOTTOM -> ToolbarAnchor.BOTTOM_CENTER
                ToolbarPosition.LEFT -> ToolbarAnchor.CENTER_LEFT
                ToolbarPosition.RIGHT -> ToolbarAnchor.CENTER_RIGHT
            } else bar.geometry.anchor
            Triple(bar, anchor, floating)
        }
        val regions = toolbarAnchorRegions(placedBars.map { it.second }.toSet(), maxWidth.value, maxHeight.value)
        for (anchor in ToolbarAnchor.entries) {
            val bars = placedBars.filter { it.second == anchor }
            if (bars.isEmpty()) continue
            val region = regions[anchor] ?: continue
            Box(Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset(region.x.dp, region.y.dp).size(region.width.dp, region.height.dp)) {
                // Like the live workspace, bars sharing an anchor occupy a stable vertical stack.
                // The stage is bounded; scrolling keeps every preview selectable when it overflows.
                ToolbarStackScroll(
                    maxHeight = (region.height.dp - 2.dp).coerceAtLeast(0.dp),
                    modifier = Modifier.align(anchor.alignment()).padding(1.dp),
                    spacing = 4.dp,
                ) {
                    for ((bar, _, floating) in bars) {
                        Box(Modifier.padding(if (floating) 4.dp else 0.dp)) {
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
                                    vertical = anchor == ToolbarAnchor.CENTER_LEFT || anchor == ToolbarAnchor.CENTER_RIGHT,
                                    onSelect = { onSelectBar(bar.id) },
                                )
                            }
                        }
                    }
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
    val resolved = resolveToolbarSize(bar.geometry)
    val appearance = bar.geometry.appearance.constrained()
    val shape = RoundedCornerShape(appearance.cornerRadiusDp.dp)
    val surfaceColor = (appearance.background?.scaleAlpha(appearance.opacity.toDouble()) ?: palette.panel).toComposeColor()
    val surface = Modifier
        .then(resolved.widthDp?.let { Modifier.width(it.dp) } ?: Modifier)
        .then(resolved.heightDp?.let { Modifier.height(it.dp) } ?: Modifier)
        .clip(shape)
        .alpha(appearance.opacity)
        .background(surfaceColor)
        .border(
            if (selected) 2.dp else if (appearance.borderEnabled) appearance.borderWidthDp.dp else 0.dp,
            (if (selected) palette.accent else (appearance.borderColor ?: palette.border)).toComposeColor(),
            shape,
        )
        .clickable(onClick = onSelect)
        .padding(horizontal = appearance.paddingHorizontalDp.dp, vertical = appearance.paddingVerticalDp.dp)
    if (vertical) {
        Column(surface, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            for (item in shown) PreviewTool(item)
            if (extras > 0) Text("+$extras", fontSize = 9.sp, color = palette.textDim.toComposeColor())
        }
    } else {
        Row(surface.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
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
