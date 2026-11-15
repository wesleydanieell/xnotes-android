package com.xnotes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarDockAlignment
import com.xnotes.core.tools.ToolbarDockPlan
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.core.workspace.NoteTabs
import com.xnotes.settings.DocumentTabsBarConfig
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import kotlin.math.roundToInt

/** Navigation is not a tool or a ToolbarEntry. It occupies an independent overlay/dock,
 * and the existing tool layout planner is its source of collision obstacles. */
internal fun documentTabsToolObstacles(
    workspace: ToolbarWorkspace, look: ToolbarLook,
    screenWidthDp: Float, screenHeightDp: Float, colorCount: Int, scale: Float,
): List<Rect> {
    if (screenWidthDp <= 0f || screenHeightDp <= 0f || scale <= 0f) return emptyList()
    val dock = toolbarDockPlan(workspace, look, screenWidthDp.dp, screenHeightDp.dp)
    val floating = toolbarFloatingPlan(workspace, look,
        (screenWidthDp - dock.leftInset - dock.rightInset).coerceAtLeast(0f).dp,
        (screenHeightDp - dock.topInset - dock.bottomInset).coerceAtLeast(0f).dp, colorCount)
    fun ToolbarDockPlan.rects(dx: Float, dy: Float) = placements.map { placement ->
        Rect((placement.x + dx) * scale, (placement.y + dy) * scale,
            (placement.x + placement.width + dx) * scale,
            (placement.y + placement.height + dy) * scale)
    }
    return dock.rects(0f, 0f) + floating.rects(dock.leftInset, dock.topInset)
}

/** Independent document navigation, sharing geometry conventions with advanced toolbars.
 * Floating keeps the canvas extent unchanged; docked reserves real layout space. */
@Composable
internal fun DocumentTabsHost(
    tabs: NoteTabs,
    activeUri: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onSplit: (String) -> Unit,
    config: DocumentTabsBarConfig,
    onConfigChange: (DocumentTabsBarConfig) -> Unit,
    workspace: ToolbarWorkspace,
    look: ToolbarLook,
    colorCount: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (!enabled || tabs.tabs.isEmpty()) { Box(modifier) { content() }; return }
    val density = LocalDensity.current
    val vertical = config.position.vertical
    var root by remember { mutableStateOf(Rect.Zero) }
    var child by remember { mutableStateOf(Size.Zero) }
    var dockedPosition by remember { mutableStateOf<Offset?>(null) }
    var hoverTarget by remember { mutableStateOf<com.xnotes.core.tools.ToolbarDropPosition?>(null) }
    // Gesture callback updates this without invalidating layout on every stylus move.
    val lastPointer = remember { mutableStateOf(Offset.Unspecified) }
    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { coords ->
        val next = coords.boundsInRoot(); if (root != next) root = next
    }) {
        val widthDp = maxWidth.value
        val heightDp = maxHeight.value
        val px = with(density) { 1.dp.toPx() }
        val host = Size(widthDp * px, heightDp * px)
        val obstacles = remember(workspace, look, widthDp, heightDp, colorCount, px) {
            documentTabsToolObstacles(workspace, look, widthDp, heightDp, colorCount, px)
        }
        fun destination(anchor: ToolbarAnchor, measured: Size,
            edge: ToolbarPosition = config.position): Offset {
            val portrait = edge.vertical
            val strip = documentTabsFreeStrip(host, anchor, portrait,
                if (portrait) 232f * px else 60f * px, obstacles, 8f * px, 84f * px,
                explicitEdge = edge)
            return strip.topLeft + documentTabsOffset(strip.size, measured, anchor, emptyList(), 0f)
        }
        val usableStrip = remember(config.anchor, config.position, obstacles, host, px) {
            documentTabsFreeStrip(host, config.anchor, vertical,
                if (vertical) 232f * px else 60f * px,
                obstacles, 8f * px, 84f * px, explicitEdge = config.position)
        }
        // Expanded navigation must fit the free strip *beside* the toolbars,
        // not the full screen width. Docked bars reserve their own edge separately.
        val maximumExpandedLength = (if (vertical) usableStrip.height else usableStrip.width) / px
        val contentExtent = config.expandedExtent((maximumExpandedLength - 32f).coerceAtLeast(0f)).dp
        val defaultSize = Size((if (vertical) if (expanded) 200 else 48 else if (expanded) 360 else 210) * px,
            (if (vertical) if (expanded) 260 else 48 else 48) * px)
        val measured = if (child == Size.Zero) defaultSize else child
        val currentOffset = if (config.floating) destination(config.anchor, measured) else Offset.Zero
        val parentPosition = if (config.floating) root.topLeft + currentOffset else dockedPosition
        val navigation: @Composable (Modifier) -> Unit = { navModifier ->
            Box(navModifier.testTag("document-tabs-bar")
                .onGloballyPositioned { coordinates ->
                    val size = coordinates.size
                    val next = Size(size.width.toFloat(), size.height.toFloat())
                    if (child != next) child = next
                    if (!config.floating) {
                        val origin = coordinates.boundsInRoot().topLeft
                        if (dockedPosition != origin) dockedPosition = origin
                    }
                }) {
                ToolbarDragSurface(
                    id = "document-tabs-navigation", anchor = config.anchor, movable = true,
                    paneBounds = root,
                    onMoveBar = { _, anchor, alignment ->
                        val edge = documentTabsEdgeAtDrop(anchor, lastPointer.value, root)
                        onConfigChange(config.movedTo(edge, alignment))
                    },
                    onDragPosition = { lastPointer.value = it },
                    hasPlacementChanged = { target ->
                        config.movedTo(documentTabsEdgeAtDrop(target.anchor, lastPointer.value, root),
                            target.alignment) != config
                    },
                    parentSlotInRoot = parentPosition,
                    currentAlignment = config.alignment(),
                    snapTargetInRoot = if (!config.floating) null else { target ->
                        root.topLeft + destination(target.anchor, measured,
                            documentTabsEdgeAtDrop(target.anchor, lastPointer.value, root))
                    },
                    onPlacementPreview = { hoverTarget = it },
                    onSettleTarget = { if (it == null) hoverTarget = null },
                ) {
                    // Drag by holding the navigation surface itself. A separate
                    // grip takes room from tabs, unlike the other toolbars.
                    FloatingDocumentTabs(
                        tabs = tabs, activeUri = activeUri, expanded = expanded,
                        onExpandedChange = onExpandedChange, onSelect = onSelect,
                        onClose = onClose, vertical = vertical,
                        expandedWidth = contentExtent,
                        expandedHeight = contentExtent,
                        floatingSurface = config.floating,
                        onReorder = onReorder, onSplit = onSplit,
                    )
                }
            }
        }
        // Show the same destination affordance as advanced toolbars without
        // storing a fictitious ToolbarBar in the editable tool collection.
        val hover = hoverTarget
        if (config.floating) {
            content()
            Box(Modifier.offset {
                IntOffset(currentOffset.x.roundToInt(), currentOffset.y.roundToInt())
            }.zIndex(20f).wrapContentSize().testTag("document-tabs-floating-slot")) {
                navigation(Modifier.wrapContentSize())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                if (config.position == ToolbarPosition.TOP) navigation(Modifier.fillMaxWidth().height(54.dp))
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    if (config.position == ToolbarPosition.LEFT)
                        navigation(Modifier.width(if (expanded) 220.dp else 48.dp).fillMaxHeight())
                    Box(Modifier.weight(1f).fillMaxHeight()) { content() }
                    if (config.position == ToolbarPosition.RIGHT)
                        navigation(Modifier.width(if (expanded) 220.dp else 48.dp).fillMaxHeight())
                }
                if (config.position == ToolbarPosition.BOTTOM) navigation(Modifier.fillMaxWidth().height(54.dp))
            }
        }
        if (hover != null && (hover.anchor != config.anchor ||
            hover.alignment != config.alignment())) {
            val requested = documentTabsEdgeAtDrop(hover.anchor, lastPointer.value, root)
            val origin = destination(hover.anchor, measured, requested)
            ToolbarDropIndicator(ToolbarDropSlot(origin.x / px, origin.y / px,
                measured.width / px, measured.height / px), "document-tabs-navigation")
        }
    }
}
