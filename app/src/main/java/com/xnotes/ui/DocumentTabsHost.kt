package com.xnotes.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.settings.DocumentTabsBarConfig
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import com.xnotes.settings.ToolbarSize
import com.xnotes.ui.icons.XnotesIcons
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlin.math.roundToInt

/** Navigation owns a separate surface. Docking reserves space outside the tools; floating avoids
 * their measured rectangles. Runtime and preview both use this exact host and surface. */
@Composable
internal fun DocumentTabsHost(
    documents: DocumentTabsState,
    config: DocumentTabsBarConfig,
    onMove: (ToolbarAnchor) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onWorkspaceBounds: (DocumentDropArea) -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (!enabled || documents.tabs.tabs.isEmpty()) { Box(modifier) { content() }; return }
    val tools = remember { ToolbarHitAreas() }
    val navigation = remember { ToolbarHitAreas() }
    var root by remember { mutableStateOf(Rect.Zero) }
    var window by remember { mutableStateOf(Rect.Zero) }
    var card by remember { mutableStateOf(Size.Zero) }
    var workspace by remember { mutableStateOf<DocumentDropArea?>(null) }
    val toolRects = tools.bounds()
    val observer = LocalDocumentTabsBoundsObserver.current
    val density = LocalDensity.current
    val state = documents.copy(dropArea = documents.dropArea ?: workspace)
    val edge = if (config.floating) DocumentTabsBarConfig.edgeOf(config.anchor) else config.position
    val vertical = edge == ToolbarPosition.LEFT || edge == ToolbarPosition.RIGHT
    val screen = androidx.compose.ui.platform.LocalConfiguration.current
    val hostSize = if (window.width > 0f) window.size else with(density) {
        Size(screen.screenWidthDp.dp.toPx(), screen.screenHeightDp.dp.toPx())
    }
    val relative = toolRects.map { it.translate(-window.topLeft) }
    val freeStrip = documentTabsFreeStrip(hostSize, config.anchor, vertical,
        with(density) { (if (vertical) 168.dp else 50.dp).toPx() }, relative,
        with(density) { 8.dp.toPx() }, with(density) { 80.dp.toPx() })
    val floatingExtent = with(density) {
        config.expandedExtent((if (vertical) freeStrip.height else freeStrip.width).toDp().value).dp
    }
    val renderBar: @Composable () -> Unit = {
        Box(Modifier.zIndex(20f).onGloballyPositioned { if (card != it.size.toSize()) card = it.size.toSize(); observer?.invoke(it.boundsInWindow()) }) {
            ToolbarDragSurface("document-navigation", config.anchor, true, root,
                { _, anchor -> onMove(anchor) }) {
                CompositionLocalProvider(LocalToolbarHitAreas provides navigation,
                    LocalGlobalToolHitAreas provides null) {
                    ToolbarHitArea {
                        DocumentTabsSurface(state, config.floating, edge, floatingExtent)
                    }
                }
            }
        }
    }
    CompositionLocalProvider(LocalGlobalToolHitAreas provides tools,
        LocalDocumentTabsHitAreas provides navigation) {
        Box(modifier.fillMaxSize().onGloballyPositioned {
            val r = it.boundsInRoot(); if (root != r) root = r
            val w = it.boundsInWindow(); if (window != w) window = w
        }) {
            Column(Modifier.fillMaxSize()) {
                if (!config.floating && edge == ToolbarPosition.TOP) renderBar()
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    if (!config.floating && edge == ToolbarPosition.LEFT) renderBar()
                    Box(Modifier.weight(1f).fillMaxHeight().onGloballyPositioned {
                        val next = DocumentDropArea(it.boundsInWindow().topLeft, it.size)
                        if (workspace != next) { workspace = next; onWorkspaceBounds(next) }
                    }) { content() }
                    if (!config.floating && edge == ToolbarPosition.RIGHT) renderBar()
                }
                if (!config.floating && edge == ToolbarPosition.BOTTOM) renderBar()
            }
            if (config.floating) {
                val offset = freeStrip.topLeft + documentTabsOffset(freeStrip.size, card, config.anchor, emptyList(), 0f)
                Box(Modifier.zIndex(20f).alpha(if (card == Size.Zero) 0f else 1f).offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                    .widthIn(max = with(density) { freeStrip.width.coerceAtLeast(1f).toDp() })
                    .heightIn(max = with(density) { freeStrip.height.coerceAtLeast(1f).toDp() })) {
                    renderBar()
                }
            }
        }
    }
}

@Composable
private fun DocumentTabsSurface(state: DocumentTabsState, floating: Boolean, edge: ToolbarPosition,
    floatingExtent: androidx.compose.ui.unit.Dp) {
    var controlsSize by remember { mutableStateOf(Size.Zero) }
    val controlsExtent = if (state.splitControls != null) with(LocalDensity.current) {
        (if (edge == ToolbarPosition.LEFT || edge == ToolbarPosition.RIGHT) controlsSize.height else controlsSize.width)
            .toDp().coerceAtLeast(40.dp)
    } else 0.dp
    val splitControls: @Composable () -> Unit = {
        state.splitControls?.let { controls ->
            Box(Modifier.onGloballyPositioned { controlsSize = it.size.toSize() }) { controls() }
        }
    }
    val palette = LocalPalette.current
    val vertical = edge == ToolbarPosition.LEFT || edge == ToolbarPosition.RIGHT
    val shape = if (floating) MaterialTheme.shapes.extraLarge else RectangleShape
    val surface = Modifier.then(if (floating) Modifier.shadow(5.dp, shape) else Modifier)
        .clip(shape).background(palette.panel.toComposeColor())
        .then(if (floating) Modifier.border(1.dp, palette.border.toComposeColor(), shape) else Modifier)
    val sideWidth by animateDpAsState(
        (if (state.expanded) DOCUMENT_TABS_SIDE_WIDTH_DP else 42f).dp + 8.dp,
        spring(stiffness = Spring.StiffnessMediumLow), label = "documentNavigationWidth")
    val grip: @Composable () -> Unit = {
        Icon(XnotesIcons.grip, "Mover barra de abas", Modifier.size(24.dp).padding(4.dp),
            tint = palette.textDim.toComposeColor())
    }
    CompositionLocalProvider(LocalToolbarLook provides ToolbarLook(edge, ToolbarSize.REGULAR, floating),
        LocalBar provides barMetrics(ToolbarSize.REGULAR).copy(vertical = vertical)) {
        if (vertical) {
            Column(surface.then(if (floating) Modifier.widthIn(max = DOCUMENT_TABS_SIDE_WIDTH_DP.dp + 8.dp)
                else Modifier.width(sideWidth).fillMaxHeight()).padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                grip()
                splitControls()
                DocumentTabsSwitcher(state, if (floating) Modifier else Modifier.weight(1f), vertical = true,
                    expandedWidth = DOCUMENT_TABS_WIDTH_DP.dp,
                    expandedHeight = if (floating) (floatingExtent - 32.dp - controlsExtent).coerceAtLeast(36.dp) else null)
            }
        } else {
            Row(surface.then(if (floating) Modifier else Modifier.fillMaxWidth()).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                grip()
                splitControls()
                DocumentTabsSwitcher(state, if (floating) Modifier else Modifier.weight(1f), vertical = false,
                    expandedWidth = if (floating) (floatingExtent - 32.dp - controlsExtent).coerceAtLeast(36.dp) else
                        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp)
            }
        }
    }
}

@Composable
private fun DocumentTabsSwitcher(state: DocumentTabsState, modifier: Modifier, vertical: Boolean,
    expandedWidth: androidx.compose.ui.unit.Dp, expandedHeight: androidx.compose.ui.unit.Dp? = null) {
    FloatingDocumentTabs(state.tabs, state.activeUri, state.expanded, state.onExpandedChange,
        state.onSelect, state.onClose, state.onReorder, state.onSplit,
        dropArea = state.dropArea, modifier = modifier, expandedWidth = expandedWidth,
        embedded = true, selection = state.selection, vertical = vertical, expandedHeight = expandedHeight)
}
