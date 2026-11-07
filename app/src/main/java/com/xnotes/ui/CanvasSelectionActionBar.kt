package com.xnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlin.math.roundToInt

/** One measured, scrollable action surface shared by note/canvas and both forks. */
@Composable
internal fun CanvasSelectionActionBar(selection: com.xnotes.core.geometry.Rect,
    content: @Composable RowScope.() -> Unit) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    val tools = LocalGlobalToolHitAreas.current?.bounds().orEmpty()
    val navigation = LocalDocumentTabsHitAreas.current?.bounds().orEmpty()
    var origin by remember { mutableStateOf(Offset.Zero) }
    var measured by remember { mutableStateOf(IntSize.Zero) }
    BoxWithConstraints(Modifier.fillMaxSize().zIndex(30f).onGloballyPositioned {
        val next = it.positionInWindow(); if (origin != next) origin = next
    }) {
        val viewport = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val selected = Rect(selection.left.toFloat(), selection.top.toFloat(), selection.right.toFloat(), selection.bottom.toFloat())
        val desired = if (measured == IntSize.Zero) with(density) { Size(400.dp.toPx(), 46.dp.toPx()) }
            else Size(measured.width.toFloat(), measured.height.toFloat())
        val placed = placeCanvasActionBar(selected, viewport, desired,
            (tools + navigation).map { it.translate(-origin) }, with(density) { 8.dp.toPx() })
        // Protect only the actual menu from native pen forwarding, not the transparent host.
        // Selection actions must not become obstacles that move the document navigation bar.
        CompositionLocalProvider(LocalGlobalToolHitAreas provides null,
            LocalTipSide provides if (placed.bottom <= selected.top) TipSide.ABOVE else TipSide.BELOW) {
            ToolbarHitArea(Modifier.offset { IntOffset(placed.left.roundToInt(), placed.top.roundToInt()) }
                .widthIn(max = (maxWidth - 16.dp).coerceAtLeast(0.dp))) {
                Row(Modifier.onSizeChanged { if (measured != it) measured = it }
                    .clip(MaterialTheme.shapes.medium).background(palette.menuBg.toComposeColor())
                    .border(1.dp, palette.border.toComposeColor(), MaterialTheme.shapes.medium)
                    .horizontalScroll(rememberScrollState()), content = content)
            }
        }
    }
}
