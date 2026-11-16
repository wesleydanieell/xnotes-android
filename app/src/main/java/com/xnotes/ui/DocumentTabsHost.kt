package com.xnotes.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarDockPlan
import com.xnotes.core.tools.ToolbarDropPosition
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.core.workspace.NoteTabs
import com.xnotes.settings.DocumentTabsBarConfig
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import kotlin.math.roundToInt

private const val DocumentTabsDockId = "document-tabs-navigation"
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

internal data class DocumentTabsSplitToolbars(
    val first: ToolbarWorkspace,
    val second: ToolbarWorkspace,
    val sideBySide: Boolean,
    val ratio: Float,
) {
    fun obstacles(look: ToolbarLook, width: Float, height: Float, colorCount: Int, px: Float): List<Rect> {
        val firstWidth = if (sideBySide) width * ratio else width
        val firstHeight = if (sideBySide) height else height * ratio
        val secondWidth = if (sideBySide) width - firstWidth else width
        val secondHeight = if (sideBySide) height else height - firstHeight
        val secondOffset = if (sideBySide) Offset(firstWidth * px, 0f) else Offset(0f, firstHeight * px)
        return documentTabsToolObstacles(first, look, firstWidth, firstHeight, colorCount, px) +
            documentTabsToolObstacles(second, look, secondWidth, secondHeight, colorCount, px)
                .map { it.translate(secondOffset) }
    }
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
    splitToolbars: DocumentTabsSplitToolbars? = null,
    content: @Composable () -> Unit,
) {
    val showNavigation = enabled && tabs.tabs.isNotEmpty()
    val density = LocalDensity.current
    val vertical = config.position.vertical
    val showExpanded = expanded || !config.floating
    var root by remember { mutableStateOf(Rect.Zero) }
    var child by remember(vertical) { mutableStateOf(Size.Zero) }
    var hoverTarget by remember { mutableStateOf<ToolbarDropPosition?>(null) }
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds().onGloballyPositioned { coords ->
        val next = coords.boundsInRoot(); if (root != next) root = next
    }) {
        val widthDp = maxWidth.value
        val heightDp = maxHeight.value
        val px = with(density) { 1.dp.toPx() }
        val host = Size(widthDp * px, heightDp * px)
        val inset = when {
            !showNavigation -> 0.dp
            !config.floating -> DocumentTabsDockExtent
            else -> 0.dp
        }
        fun reserved(edge: ToolbarPosition) = if (config.position == edge) inset else 0.dp
        Box(Modifier.fillMaxSize().padding(PaddingValues.Absolute(
            left = reserved(ToolbarPosition.LEFT), top = reserved(ToolbarPosition.TOP),
            right = reserved(ToolbarPosition.RIGHT), bottom = reserved(ToolbarPosition.BOTTOM),
        ))) { content() }
        if (!showNavigation) return@BoxWithConstraints

        val obstacles = remember(workspace, look, widthDp, heightDp, colorCount, px, splitToolbars, config.floating) {
            if (!config.floating) emptyList()
            else splitToolbars?.obstacles(look, widthDp, heightDp, colorCount, px)
                ?: documentTabsToolObstacles(workspace, look, widthDp, heightDp, colorCount, px)
        }
        fun strip(target: DocumentTabsBarConfig) =
            documentTabsFloatingStrip(host, target.anchor, target.position, obstacles, px,
                keepCentered = splitToolbars != null)

        fun placement(target: DocumentTabsBarConfig, measuredLength: Float, thickness: Float): Rect {
            val edge = target.position
            val verticalTarget = edge.vertical
            if (!target.floating) {
                val depth = DocumentTabsDockExtent.value * px
                return when (edge) {
                    ToolbarPosition.TOP -> Rect(0f, 0f, host.width, minOf(depth, host.height))
                    ToolbarPosition.BOTTOM -> Rect(0f, (host.height - depth).coerceAtLeast(0f), host.width, host.height)
                    ToolbarPosition.LEFT -> Rect(0f, 0f, minOf(depth, host.width), host.height)
                    ToolbarPosition.RIGHT -> Rect((host.width - depth).coerceAtLeast(0f), 0f, host.width, host.height)
                }
            }
            val space = strip(target)
            val available = if (verticalTarget) space.height else space.width
            val length = if (showExpanded) target.expandedExtent((available / px - 32f).coerceAtLeast(0f)) * px
                else measuredLength
            val size = if (verticalTarget) Size(minOf(space.width, thickness), minOf(space.height, length))
                else Size(minOf(space.width, length), minOf(space.height, thickness))
            val origin = space.topLeft + documentTabsOffset(space.size, size, target.anchor, emptyList(), 0f)
            return Rect(origin, size)
        }
        val length = (if (vertical) child.height else child.width).takeIf { it > 0f } ?: 210f * px
        val thickness = (if (vertical) child.width else child.height).takeIf { it > 0f }
            ?: (if (showExpanded) 44f else 42f) * px
        val current = placement(config, length, thickness)
        fun destination(target: ToolbarDropPosition): Rect? {
            val next = config.movedTo(DocumentTabsBarConfig.edgeOf(target.anchor), target.alignment)
            val rect = placement(next, length, thickness)
            return rect.takeIf { !rect.isEmpty && obstacles.none(rect::overlaps) }
        }
        val dragAnchor = if (vertical) DocumentTabsBarConfig.anchorOf(config.position) else config.anchor
        val floatingStrip = if (config.floating) strip(config) else current
        val slotModifier = Modifier.offset {
            IntOffset(current.left.roundToInt(), current.top.roundToInt())
        }.zIndex(20f).then(
            if (config.floating) Modifier.sizeIn(
                maxWidth = (floatingStrip.width / px).dp,
                maxHeight = (floatingStrip.height / px).dp,
            ) else Modifier.width((current.width / px).dp).height((current.height / px).dp),
        )
        Box(slotModifier.testTag("document-tabs-bar").onGloballyPositioned { coordinates ->
            val size = Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat())
            if (child != size) child = size
        }) {
            ToolbarDragSurface(
                id = DocumentTabsDockId, anchor = dragAnchor, movable = true, paneBounds = root,
                onMoveBar = { _, anchor, alignment ->
                    onConfigChange(config.movedTo(DocumentTabsBarConfig.edgeOf(anchor), alignment))
                },
                parentSlotInRoot = root.topLeft + current.topLeft,
                fillParentSlot = !config.floating,
                currentAlignment = config.alignment(),
                snapTargetInRoot = { target -> destination(target)?.let { root.topLeft + it.topLeft } },
                onPlacementPreview = { hoverTarget = it },
                onSettleTarget = { hoverTarget = null },
            ) {
                FloatingDocumentTabs(
                    tabs = tabs, activeUri = activeUri, expanded = showExpanded,
                    onExpandedChange = onExpandedChange, onSelect = onSelect, onClose = onClose,
                    vertical = vertical,
                    expandedWidth = ((if (vertical) current.height else current.width) / px).dp,
                    modifier = if (config.floating) Modifier else Modifier.fillMaxSize(),
                    floatingSurface = config.floating, onReorder = onReorder, onSplit = onSplit,
                )
            }
        }
        val outlineColor = MaterialTheme.colorScheme.primary
        @Composable
        fun Outline(rect: Rect) {
            Box(Modifier.offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                .width((rect.width / px).dp).height((rect.height / px).dp).zIndex(21f)
                .border(1.5.dp, outlineColor, MaterialTheme.shapes.medium)
                .testTag("document-tabs-drop-outline"))
        }
        val hover = hoverTarget
        if (hover != null && hover != ToolbarDropPosition(dragAnchor, config.alignment())) {
            destination(hover)?.let { Outline(it) }
        }
    }
}