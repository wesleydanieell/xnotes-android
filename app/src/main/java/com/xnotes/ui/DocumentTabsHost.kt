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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.xnotes.core.tools.ToolbarDropPosition
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.core.workspace.NoteTabs
import com.xnotes.settings.DocumentTabsBarConfig
import com.xnotes.settings.DocumentTabsSizing
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
    var hoverTarget by remember { mutableStateOf<ToolbarDropPosition?>(null) }
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
        fun freeStrip(anchor: ToolbarAnchor, edge: ToolbarPosition): Rect =
            documentTabsFreeStrip(host, anchor, edge.vertical, 60f * px,
                obstacles, 8f * px, 84f * px, explicitEdge = edge)
        fun destination(strip: Rect, anchor: ToolbarAnchor, measured: Size): Offset =
            strip.topLeft + documentTabsOffset(strip.size, measured, anchor, emptyList(), 0f)

        fun expandedExtent(edge: ToolbarPosition, strip: Rect): Float {
            val available = if (config.floating) {
                (if (edge.vertical) strip.height else strip.width) / px
            } else if (edge.vertical) heightDp else widthDp
            val configured = config.expandedExtent((available - 32f).coerceAtLeast(0f))
            return if (edge.vertical && config.sizing == DocumentTabsSizing.DYNAMIC) {
                configured.coerceAtMost(240f)
            } else configured
        }
        val usableStrip = remember(config.anchor, config.position, obstacles, host, px) {
            freeStrip(config.anchor, config.position)
        }
        val contentExtent = expandedExtent(config.position, usableStrip).dp
        val stripLength = if (expanded) contentExtent.value else 210f
        val defaultSize = if (vertical) Size(44f * px, stripLength * px)
            else Size(stripLength * px, 44f * px)
        val measured = if (child == Size.Zero) defaultSize else child
        val currentOffset = if (config.floating) destination(usableStrip, config.anchor, measured) else Offset.Zero
        val parentPosition = if (config.floating) root.topLeft + currentOffset else dockedPosition
        fun configAt(target: ToolbarDropPosition): DocumentTabsBarConfig =
            config.movedTo(DocumentTabsBarConfig.edgeOf(target.anchor), target.alignment)
        fun projectedSize(edge: ToolbarPosition, strip: Rect): Size {
            val length = if (expanded) expandedExtent(edge, strip) * px
                else if (vertical) measured.height else measured.width
            val thickness = if (vertical) measured.width else measured.height
            return if (edge.vertical) Size(thickness, length) else Size(length, thickness)
        }
        // The shared gesture resolver encodes side edges with center anchors and
        // carries their alignment separately. Use its encoding for acknowledgement.
        val dragAnchor = if (vertical) DocumentTabsBarConfig.anchorOf(config.position) else config.anchor
        val dockAlignment = when (config.alignment()) {
            ToolbarDockAlignment.START -> if (vertical) Alignment.TopCenter else Alignment.CenterStart
            ToolbarDockAlignment.CENTER -> Alignment.Center
            ToolbarDockAlignment.END -> if (vertical) Alignment.BottomCenter else Alignment.CenterEnd
        }
        val navigation: @Composable (Modifier) -> Unit = { navModifier ->
            Box(navModifier
                .then(if (config.floating) Modifier else Modifier.wrapContentSize(dockAlignment))
                .testTag("document-tabs-bar")
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
                    id = "document-tabs-navigation", anchor = dragAnchor, movable = true,
                    paneBounds = root,
                    onMoveBar = { _, anchor, alignment ->
                        onConfigChange(configAt(ToolbarDropPosition(anchor, alignment)))
                    },
                    parentSlotInRoot = parentPosition,
                    currentAlignment = config.alignment(),
                    snapTargetInRoot = if (!config.floating) null else { target ->
                        val targetConfig = configAt(target)
                        val strip = freeStrip(targetConfig.anchor, targetConfig.position)
                        root.topLeft + destination(strip, targetConfig.anchor, projectedSize(targetConfig.position, strip))
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
                navigation(Modifier.sizeIn(
                    maxWidth = (usableStrip.width / px).dp,
                    maxHeight = (usableStrip.height / px).dp,
                ))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                if (config.position == ToolbarPosition.TOP) navigation(Modifier.fillMaxWidth().height(48.dp))
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    if (config.position == ToolbarPosition.LEFT)
                        navigation(Modifier.width(48.dp).fillMaxHeight())
                    Box(Modifier.weight(1f).fillMaxHeight()) { content() }
                    if (config.position == ToolbarPosition.RIGHT)
                        navigation(Modifier.width(48.dp).fillMaxHeight())
                }
                if (config.position == ToolbarPosition.BOTTOM) navigation(Modifier.fillMaxWidth().height(48.dp))
            }
        }
        if (hover != null) {
            val targetConfig = configAt(hover)
            if (targetConfig != config) {
                val strip = freeStrip(targetConfig.anchor, targetConfig.position)
                val targetSize = projectedSize(targetConfig.position, strip)
                val origin = if (config.floating) {
                    destination(strip, targetConfig.anchor, targetSize)
                } else {
                    val edgeOffset = documentTabsOffset(host, targetSize, targetConfig.anchor, emptyList(), 0f)
                    val inset = (48f * px - if (targetConfig.position.vertical) targetSize.width else targetSize.height) / 2f
                    edgeOffset + when (targetConfig.position) {
                        ToolbarPosition.TOP -> Offset(0f, inset)
                        ToolbarPosition.BOTTOM -> Offset(0f, -inset)
                        ToolbarPosition.LEFT -> Offset(inset, 0f)
                        ToolbarPosition.RIGHT -> Offset(-inset, 0f)
                    }
                }
                ToolbarDropIndicator(ToolbarDropSlot(origin.x / px, origin.y / px,
                    targetSize.width / px, targetSize.height / px), "document-tabs-navigation")
            }
        }
    }
}
