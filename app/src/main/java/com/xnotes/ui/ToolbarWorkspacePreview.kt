package com.xnotes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.core.model.Rgba
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.core.tools.ToolbarLayout
import com.xnotes.core.tools.ToolbarScale
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import com.xnotes.settings.ToolbarSize
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

/**
 * A note-like stage with 1:1 dp toolbar controls. Oversized bars scroll like the real
 * ToolbarFrame rather than shrinking their icons or silently omitting tools.
 * This is still a non-executable preview: tools are represented, not dispatched.
 */
@Composable
internal fun ToolbarWorkspacePreview(
    workspace: ToolbarWorkspace,
    look: ToolbarLook,
    selectedId: String,
    colors: List<Rgba>,
    onSelectBar: (String) -> Unit,
    onMoveBar: (String, ToolbarAnchor) -> Unit,
) {
    val palette = LocalPalette.current
    var stageBounds by remember { mutableStateOf(Rect.Zero) }
    val primary = workspace.bars.firstOrNull()?.takeIf { it.visible }
    val docked = primary != null && !primary.geometry.floating && !look.floating
    val dockSize = look.size
    val dockDepth = if (docked) barMetrics(dockSize).thickness else 0.dp
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE7EAF0))
            .border(1.dp, palette.border.toComposeColor(), RoundedCornerShape(14.dp))
            .onGloballyPositioned { stageBounds = it.boundsInRoot() },
    ) {
        val stageWidth = maxWidth
        val stageHeight = maxHeight
        Canvas(Modifier.fillMaxSize()) {
            val depth = dockDepth.toPx()
            val left = if (docked && look.position == ToolbarPosition.LEFT) depth else 0f
            val top = if (docked && look.position == ToolbarPosition.TOP) depth else 0f
            val right = if (docked && look.position == ToolbarPosition.RIGHT) depth else 0f
            val bottom = if (docked && look.position == ToolbarPosition.BOTTOM) depth else 0f
            val paperWidth = (size.width - left - right).coerceAtLeast(0f)
            val paperHeight = (size.height - top - bottom).coerceAtLeast(0f)
            drawRect(
                Color(0xFFFAFAFD),
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(paperWidth, paperHeight),
            )
            val inset = 14.dp.toPx()
            val rule = 30.dp.toPx()
            var y = top + 54.dp.toPx()
            while (y < size.height - bottom - inset) {
                drawLine(
                    Color(0xFFE8EDF5),
                    androidx.compose.ui.geometry.Offset(left + inset, y),
                    androidx.compose.ui.geometry.Offset(size.width - right - inset, y),
                    1.dp.toPx(),
                )
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
            Box(
                Modifier
                    .align(anchor.alignment())
                    .padding(if (floating) 8.dp else 1.dp)
                    .then(
                        if (!floating && vertical) Modifier.fillMaxHeight()
                        else if (!floating) Modifier.fillMaxWidth()
                        else Modifier,
                    ),
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
                        effectiveSize = if (index == 0 && !bar.geometry.floating) look.size else when (bar.geometry.scale) {
                            ToolbarScale.COMPACT -> ToolbarSize.COMPACT
                            ToolbarScale.REGULAR -> ToolbarSize.REGULAR
                            ToolbarScale.COMFORTABLE -> ToolbarSize.COMFORTABLE
                        },
                        vertical = vertical,
                        floating = floating,
                        availableWidth = stageWidth - 20.dp,
                        availableHeight = stageHeight - 20.dp,
                        colors = colors,
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
    effectiveSize: ToolbarSize,
    vertical: Boolean,
    floating: Boolean,
    availableWidth: Dp,
    availableHeight: Dp,
    colors: List<Rgba>,
    onSelect: () -> Unit,
) {
    val palette = LocalPalette.current
    val metrics = barMetrics(effectiveSize).copy(vertical = vertical)
    val shape = RoundedCornerShape(if (floating) 24.dp else 0.dp)
    val surface = Modifier
        .then(if (floating) Modifier.shadow(6.dp, shape) else Modifier)
        .clip(shape)
        .background(palette.panel.toComposeColor())
        .border(
            if (selected) 2.dp else if (floating) 1.dp else 0.dp,
            (if (selected) palette.accent else palette.border).toComposeColor(),
            shape,
        )
        .clickable(onClick = onSelect)

    if (vertical) {
        Column(
            surface
                .heightIn(max = availableHeight)
                .then(if (!floating) Modifier.fillMaxHeight() else Modifier)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PreviewSections(bar.layout, metrics, colors, vertical = true)
        }
    } else {
        Row(
            surface
                .widthIn(max = availableWidth)
                .then(if (!floating) Modifier.fillMaxWidth() else Modifier)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PreviewSections(bar.layout, metrics, colors, vertical = false)
        }
    }
}

@Composable
private fun PreviewSections(
    layout: ToolbarLayout,
    metrics: BarMetrics,
    colors: List<Rgba>,
    vertical: Boolean,
) {
    // A flat sequence is used here because the actual strip draws separators between sections.
    layout.visibleSections.forEachIndexed { sectionIndex, section ->
        if (sectionIndex > 0) {
            val palette = LocalPalette.current
            Box(
                Modifier
                    .padding(if (vertical) 4.dp else 3.dp)
                    .then(
                        if (vertical) Modifier.size(metrics.rule, 1.dp)
                        else Modifier.size(1.dp, metrics.rule),
                    )
                    .background(palette.border.toComposeColor()),
            )
        }
        section.visibleEntries.forEach { entry ->
            PreviewTool(entry.item, metrics, colors, vertical)
        }
    }
}

@Composable
private fun PreviewTool(item: ToolbarItem, metrics: BarMetrics, colors: List<Rgba>, vertical: Boolean) {
    val palette = LocalPalette.current
    if (item == ToolbarItem.COLORS) {
        if (vertical) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                colors.forEach { color ->
                    Box(Modifier.size(metrics.swatch).clip(CircleShape).background(color.toComposeColor()))
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                colors.forEach { color ->
                    Box(Modifier.size(metrics.swatch).clip(CircleShape).background(color.toComposeColor()))
                }
            }
        }
    } else {
        // Composite items occupy multiple controls in both real toolbars.
        val count = when (item) {
            ToolbarItem.PAGE_NAV, ToolbarItem.ZOOM -> 3
            else -> 1
        }
        val cells: @Composable () -> Unit = {
            repeat(count) { index ->
                Box(Modifier.size(metrics.button), contentAlignment = Alignment.Center) {
                    if (item == ToolbarItem.TITLE) {
                        Text("Note", maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontSize = metrics.label.value.sp, color = palette.textDim.toComposeColor())
                    } else {
                        Icon(
                            itemIcon(item), contentDescription = null,
                            modifier = Modifier.size(metrics.icon),
                            tint = palette.text.toComposeColor(),
                        )
                    }
                }
            }
        }
        if (vertical) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { cells() }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) { cells() }
        }
    }
}
