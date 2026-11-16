package com.xnotes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
import com.xnotes.core.tools.ToolbarDockPlacement
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarBarContent
import com.xnotes.core.tools.ToolbarGeometry
import com.xnotes.core.tools.ToolbarLayout
import com.xnotes.core.tools.ToolbarDropPosition
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.core.workspace.NoteTabs
import com.xnotes.settings.DocumentTabsBarConfig
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import kotlin.math.roundToInt

private const val DocumentTabsDockId = "document-tabs-navigation"
internal val LocalReorderToolbarWithTabs =
    compositionLocalOf<((ToolbarBar, ToolbarDockPlacement, Boolean, Offset) -> Boolean)?> { null }
internal val LocalFloatingDocumentTabs = compositionLocalOf<ToolbarBar?> { null }
internal val LocalDocumentTabsDockLayout = compositionLocalOf { false }
internal val LocalDocumentTabsDock = compositionLocalOf<ToolbarBar?> { null }

internal fun documentTabsToolbar(config: DocumentTabsBarConfig, floatingLength: Float? = null): ToolbarBar =
    ToolbarBar(
        id = DocumentTabsDockId,
        name = "",
        layout = ToolbarLayout(emptyList()),
        geometry = ToolbarGeometry(
            anchor = config.anchor,
            floating = config.floating,
            dockEdge = config.position.dockEdge(),
            dockAlignment = config.alignment(),
            dockLane = config.dockLane,
        ).withLengthDp(floatingLength?.roundToInt(), config.position.vertical)
            .withThicknessDp(if (config.floating) 44 else DocumentTabsDockExtent.value.toInt(),
                config.position.vertical),
        content = ToolbarBarContent.DocumentTabs(config.afterToolbarId),
    )

internal fun sharedDocumentTabsDock(
    config: DocumentTabsBarConfig?, workspace: ToolbarWorkspace, look: ToolbarLook,
    width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp,
): ToolbarBar? {
    if (config == null || config.floating) return null
    val existing = toolbarDockPlan(workspace, look, width, height)
    if (existing.placements.isEmpty()) return null
    val request = documentTabsToolbar(config)
    val shared = toolbarDockPlan(workspace, look, width, height, request)
    return request.takeIf {
        shared.placement(request.id) != null &&
            existing.placements.all { shared.placement(it.id) != null }
    }
}

internal val DocumentTabsDockExtent = 48.dp

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
    shareDockLanes: Boolean = false,
    content: @Composable () -> Unit,
) {
    val showNavigation = enabled && tabs.tabs.isNotEmpty()
    val density = LocalDensity.current
    val vertical = config.position.vertical
    val showExpanded = expanded || !config.floating
    var navigationBounds by remember { mutableStateOf(Rect.Zero) }
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
        val sharedDock = remember(shareDockLanes, showNavigation, config, workspace, look, maxWidth, maxHeight) {
            if (shareDockLanes && showNavigation) sharedDocumentTabsDock(config, workspace, look, maxWidth, maxHeight) else null
        }
        val dockSlot = sharedDock?.let {
            toolbarDockPlan(workspace, look, maxWidth, maxHeight, it).placement(it.id)
        }
        val floatingStage = remember(workspace, look, maxWidth, maxHeight) {
            toolbarDockPlan(workspace, look, maxWidth, maxHeight)
        }
        val floatingWidth = (widthDp - floatingStage.leftInset - floatingStage.rightInset).coerceAtLeast(0f).dp
        val floatingHeight = (heightDp - floatingStage.topInset - floatingStage.bottomInset).coerceAtLeast(0f).dp
        val floatingBase = remember(workspace, look, floatingWidth, floatingHeight, colorCount) {
            toolbarFloatingPlan(workspace, look, floatingWidth, floatingHeight, colorCount)
        }
        fun floatingRequest(target: DocumentTabsBarConfig): ToolbarBar {
            val space = (if (target.position.vertical) floatingHeight else floatingWidth).value
            val currentLength = if (vertical) child.height / px else child.width / px
            val length = if (showExpanded) target.expandedExtent((space - 48f).coerceAtLeast(0f))
                else currentLength.takeIf { it > 0f }?.coerceAtLeast(96f) ?: 210f
            return documentTabsToolbar(target, length)
        }
        fun floatingPlan(request: ToolbarBar) =
            toolbarFloatingPlan(workspace, look, floatingWidth, floatingHeight, colorCount, request)
        val candidate = if (showNavigation && config.floating && shareDockLanes &&
            floatingBase.placements.isNotEmpty()) floatingRequest(config) else null
        fun fitsFloatingPlan(request: ToolbarBar, plan: ToolbarDockPlan): Boolean {
            val slot = plan.placement(request.id) ?: return false
            val edge = slot.edge.toolbarPosition()
            val mainLength = if (edge.vertical) slot.height else slot.width
            val viewportLength = if (edge.vertical) heightDp else widthDp
            val gap = when (edge) {
                ToolbarPosition.TOP -> floatingStage.topInset + slot.y
                ToolbarPosition.BOTTOM -> heightDp - floatingStage.topInset - slot.y - slot.height
                ToolbarPosition.LEFT -> floatingStage.leftInset + slot.x
                ToolbarPosition.RIGHT -> widthDp - floatingStage.leftInset - slot.x - slot.width
            }
            val viewportDepth = if (edge.vertical) widthDp else heightDp
            return mainLength >= minOf(96f, (viewportLength - 48f).coerceAtLeast(0f)) &&
                gap < viewportDepth / 2f &&
                floatingBase.placements.all { plan.placement(it.id) != null }
        }
        val sharedFloating = candidate?.takeIf { fitsFloatingPlan(it, floatingPlan(it)) }
        val floatingSlot = sharedFloating?.let { floatingPlan(it).placement(it.id) }
        fun floatingOrigin(x: Float, y: Float) =
            Offset((x + floatingStage.leftInset) * px, (y + floatingStage.topInset) * px)
        // The editor stays in one composition slot as the dock changes edges
        // or switches between the shared plan and the dedicated inset.
        val reserveDock = showNavigation && !config.floating && dockSlot == null
        fun reserved(edge: ToolbarPosition) =
            if (reserveDock && config.position == edge) DocumentTabsDockExtent else 0.dp
        val padding = PaddingValues.Absolute(
            left = reserved(ToolbarPosition.LEFT), top = reserved(ToolbarPosition.TOP),
            right = reserved(ToolbarPosition.RIGHT), bottom = reserved(ToolbarPosition.BOTTOM),
        )
        fun persistOrder(next: DocumentTabsBarConfig): Boolean {
            if (next == config) return false
            val fits = if (config.floating) {
                val request = floatingRequest(next)
                fitsFloatingPlan(request, floatingPlan(request))
            } else sharedDocumentTabsDock(next, workspace, look, maxWidth, maxHeight) != null
            if (fits) {
                hoverTarget = null
                onConfigChange(next)
            }
            return fits
        }
        val reorderWithToolbar = floatingSlot ?: dockSlot
        fun reorderToolbar(bar: ToolbarBar, placement: ToolbarDockPlacement, floating: Boolean, pointer: Offset): Boolean {
            if (reorderWithToolbar == null || floating != config.floating ||
                placement.edge != reorderWithToolbar.edge || !navigationBounds.contains(pointer)) return false
            val next = config.copy(dockLane = bar.geometry.dockLane,
                afterToolbarId = if (config.afterToolbarId == bar.id) null else bar.id)
            return persistOrder(next)
        }
        val reorderParticipant: ((ToolbarBar, ToolbarDockPlacement, Boolean, Offset) -> Boolean)? =
            if (reorderWithToolbar == null) null else { bar, placement, floating, pointer ->
                reorderToolbar(bar, placement, floating, pointer)
            }
        CompositionLocalProvider(
            LocalDocumentTabsDock provides sharedDock,
            LocalFloatingDocumentTabs provides sharedFloating,
            LocalReorderToolbarWithTabs provides reorderParticipant,
            LocalDocumentTabsDockLayout provides (showNavigation && !config.floating),
        ) {
            Box(Modifier.fillMaxSize().padding(padding)) { content() }
        }
        if (!showNavigation) return@BoxWithConstraints

        val obstacles = remember(workspace, look, widthDp, heightDp, colorCount, px) {
            documentTabsToolObstacles(workspace, look, widthDp, heightDp, colorCount, px)
        }
        fun freeStrip(anchor: ToolbarAnchor, edge: ToolbarPosition): Rect =
            documentTabsFloatingStrip(host, anchor, edge, obstacles, px)
        fun destination(strip: Rect, anchor: ToolbarAnchor, measured: Size): Offset =
            strip.topLeft + documentTabsOffset(strip.size, measured, anchor, emptyList(), 0f)

        fun expandedExtent(edge: ToolbarPosition, strip: Rect): Float {
            if (!config.floating) return if (edge.vertical) heightDp else widthDp
            val available = (if (edge.vertical) strip.height else strip.width) / px
            return config.expandedExtent((available - 32f).coerceAtLeast(0f))
        }
        val usableStrip = remember(config.anchor, config.position, obstacles, host, px) {
            freeStrip(config.anchor, config.position)
        }
        val contentExtent = ((dockSlot ?: floatingSlot)?.let { if (vertical) it.height else it.width }
            ?: expandedExtent(config.position, usableStrip)).dp
        val stripLength = if (showExpanded) contentExtent.value else 210f
        val defaultSize = if (vertical) Size(44f * px, stripLength * px)
            else Size(stripLength * px, 44f * px)
        val measured = if (child == Size.Zero) defaultSize else child
        val currentOffset = when {
            floatingSlot != null -> floatingOrigin(floatingSlot.x, floatingSlot.y)
            config.floating -> destination(usableStrip, config.anchor, measured)
            else -> Offset.Zero
        }
        val parentPosition = if (config.floating) root.topLeft + currentOffset else dockedPosition
        fun configAt(target: ToolbarDropPosition): DocumentTabsBarConfig =
            config.movedTo(DocumentTabsBarConfig.edgeOf(target.anchor), target.alignment)
        fun projectedSize(edge: ToolbarPosition, strip: Rect): Size {
            val length = if (showExpanded) expandedExtent(edge, strip) * px
                else if (vertical) measured.height else measured.width
            val thickness = if (!config.floating) DocumentTabsDockExtent.value * px
                else if (vertical) measured.width else measured.height
            return if (edge.vertical) Size(thickness, length) else Size(length, thickness)
        }
        // The shared gesture resolver encodes side edges with center anchors and
        // carries their alignment separately. Use its encoding for acknowledgement.
        val dragAnchor = if (vertical) DocumentTabsBarConfig.anchorOf(config.position) else config.anchor
        val navigation: @Composable (Modifier) -> Unit = { navModifier ->
            Box(navModifier
                .testTag("document-tabs-bar")
                .onGloballyPositioned { coordinates ->
                    navigationBounds = coordinates.boundsInRoot()
                    val size = coordinates.size
                    val next = Size(size.width.toFloat(), size.height.toFloat())
                    if (child != next) child = next
                    if (!config.floating) {
                        val origin = coordinates.boundsInRoot().topLeft
                        if (dockedPosition != origin) dockedPosition = origin
                    }
                }) {
                ToolbarDragSurface(
                    id = DocumentTabsDockId, anchor = dragAnchor, movable = true,
                    paneBounds = root,
                    onMoveBar = { _, anchor, alignment ->
                        onConfigChange(configAt(ToolbarDropPosition(anchor, alignment)))
                    },
                    parentSlotInRoot = parentPosition,
                    fillParentSlot = !config.floating,
                    currentAlignment = config.alignment(),
                    snapTargetInRoot = if (!config.floating) null else { target ->
                        val targetConfig = configAt(target)
                        val slot = sharedFloating?.let {
                            val request = floatingRequest(targetConfig)
                            val plan = floatingPlan(request)
                            plan.placement(request.id)?.takeIf { fitsFloatingPlan(request, plan) }
                        }
                        val strip = freeStrip(targetConfig.anchor, targetConfig.position)
                        root.topLeft + if (slot != null) floatingOrigin(slot.x, slot.y)
                        else destination(strip, targetConfig.anchor, projectedSize(targetConfig.position, strip))
                    },
                    onDrop = { pointer ->
                        val tabsSlot = reorderWithToolbar
                        val plan = when {
                            sharedFloating != null -> floatingPlan(sharedFloating)
                            sharedDock != null -> toolbarDockPlan(workspace, look, widthDp.dp, heightDp.dp, sharedDock)
                            else -> null
                        }
                        val target = plan?.placements?.firstOrNull { placement ->
                            val origin = if (config.floating) floatingOrigin(placement.x, placement.y)
                                else Offset(placement.x * px, placement.y * px)
                            placement.id != DocumentTabsDockId && placement.edge == tabsSlot?.edge &&
                                Rect(root.topLeft + origin, Size(placement.width * px, placement.height * px)).contains(pointer)
                        }
                        val bar = target?.let { slot -> workspace.bars.firstOrNull { it.id == slot.id } }
                        bar != null && persistOrder(config.copy(dockLane = bar.geometry.dockLane,
                            afterToolbarId = if (config.afterToolbarId == bar.id) null else bar.id))
                    },
                    onPlacementPreview = { hoverTarget = it },
                    onSettleTarget = { if (it == null) hoverTarget = null },
                ) {
                    // Drag by holding the navigation surface itself. A separate
                    // grip takes room from tabs, unlike the other toolbars.
                    FloatingDocumentTabs(
                        tabs = tabs, activeUri = activeUri, expanded = showExpanded,
                        onExpandedChange = onExpandedChange, onSelect = onSelect,
                        onClose = onClose, vertical = vertical,
                        expandedWidth = contentExtent,
                        modifier = if (config.floating) Modifier else Modifier.fillMaxSize(),
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
            Box(Modifier.offset {
                IntOffset(currentOffset.x.roundToInt(), currentOffset.y.roundToInt())
            }.zIndex(20f).wrapContentSize().testTag("document-tabs-floating-slot")) {
                navigation(Modifier.sizeIn(
                    maxWidth = floatingSlot?.width?.dp ?: (usableStrip.width / px).dp,
                    maxHeight = floatingSlot?.height?.dp ?: (usableStrip.height / px).dp,
                ))
            }
        } else if (dockSlot != null) {
            navigation(Modifier.offset(x = dockSlot.x.dp, y = dockSlot.y.dp).zIndex(20f)
                .width(dockSlot.width.dp).height(dockSlot.height.dp))
        } else {
            val width = if (vertical) DocumentTabsDockExtent else widthDp.dp
            val height = if (vertical) heightDp.dp else DocumentTabsDockExtent
            val x = if (config.position == ToolbarPosition.RIGHT) widthDp.dp - width else 0.dp
            val y = if (config.position == ToolbarPosition.BOTTOM) heightDp.dp - height else 0.dp
            navigation(Modifier.offset(x = x, y = y).zIndex(20f).width(width).height(height))
        }
        if (hover != null) {
            val targetConfig = configAt(hover)
            if (targetConfig != config) {
                val strip = freeStrip(targetConfig.anchor, targetConfig.position)
                val targetSize = projectedSize(targetConfig.position, strip)
                val sharedTarget = if (dockSlot == null) null else {
                    val request = documentTabsToolbar(targetConfig)
                    toolbarDockPlan(workspace, look, maxWidth, maxHeight, request).placement(request.id)
                }
                val floatingTarget = sharedFloating?.let {
                    val request = floatingRequest(targetConfig)
                    floatingPlan(request).placement(request.id)
                }
                val origin = if (floatingTarget != null) {
                    floatingOrigin(floatingTarget.x, floatingTarget.y)
                } else if (sharedTarget != null) {
                    Offset(sharedTarget.x * px, sharedTarget.y * px)
                } else if (config.floating) {
                    destination(strip, targetConfig.anchor, targetSize)
                } else {
                    documentTabsOffset(host, targetSize, targetConfig.anchor, emptyList(), 0f)
                }
                ToolbarDropIndicator(ToolbarDropSlot(origin.x / px, origin.y / px,
                    (floatingTarget ?: sharedTarget)?.width ?: targetSize.width / px,
                    (floatingTarget ?: sharedTarget)?.height ?: targetSize.height / px), DocumentTabsDockId)
            }
        }
    }
}
