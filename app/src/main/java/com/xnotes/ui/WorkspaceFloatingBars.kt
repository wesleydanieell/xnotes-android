package com.xnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarAnchorResolver
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarScale
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import com.xnotes.settings.ToolbarSize
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlin.math.roundToInt

/**
 * Hold anywhere on a toolbar's container to pick it up. Ordinary taps still reach
 * the tool buttons; the drawing canvas receives events outside the bars unchanged.
 * A completed move snaps to a safe 3x3 anchor and is persisted by the caller.
 */
@Composable
fun ToolbarDragSurface(
    id: String,
    anchor: ToolbarAnchor,
    movable: Boolean,
    floating: Boolean,
    paneBounds: Rect,
    workspace: ToolbarWorkspace,
    onMoveBar: (String, ToolbarAnchor) -> Unit,
    previewLook: ToolbarLook? = null,
    colorCount: Int = 5,
    content: @Composable () -> Unit,
) {
    var drag by remember(id) { mutableStateOf(Offset.Zero) }
    var dragging by remember(id) { mutableStateOf(false) }
    var barBounds by remember(id) { mutableStateOf(Rect.Zero) }
    val gesture = if (!movable) Modifier else Modifier.pointerInput(id, anchor, paneBounds) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                drag = Offset.Zero
                dragging = true
            },
            onDrag = { change, distance ->
                change.consume()
                drag += distance
            },
            onDragEnd = {
                val moved = drag
                drag = Offset.Zero
                dragging = false
                if (moved.getDistance() >= 12f && barBounds != Rect.Zero && paneBounds != Rect.Zero) {
                    val center = barBounds.center + moved - paneBounds.topLeft
                    ToolbarAnchorResolver.nearest(
                        center.x, center.y, paneBounds.width, paneBounds.height,
                    )?.let { target ->
                        if (target != anchor) onMoveBar(id, target)
                    }
                }
            },
            onDragCancel = {
                drag = Offset.Zero
                dragging = false
            },
        )
    }
    val palette = LocalPalette.current
    val dropTarget = if (dragging && drag.getDistance() >= 12f &&
        barBounds != Rect.Zero && paneBounds != Rect.Zero) {
        val center = barBounds.center + drag - paneBounds.topLeft
        ToolbarAnchorResolver.nearest(center.x, center.y, paneBounds.width, paneBounds.height)
            ?.takeIf { floating || it == ToolbarAnchor.TOP_CENTER || it == ToolbarAnchor.BOTTOM_CENTER ||
                it == ToolbarAnchor.CENTER_LEFT || it == ToolbarAnchor.CENTER_RIGHT }
    } else null
    // Preview the *actual* packed destination: orientation, automatic dimensions,
    // lanes and other bars all come from the same planners as the runtime layout.
    val density = LocalDensity.current.density
    val placement = dropTarget?.let { target ->
        toolbarDragDestination(
            workspace, previewLook ?: LocalToolbarLook.current, id, target,
            (paneBounds.width / density).dp, (paneBounds.height / density).dp, colorCount,
        )
    }
    val destinationOffset = placement?.let {
        Offset(
            paneBounds.left + it.x * density - barBounds.left,
            paneBounds.top + it.y * density - barBounds.top,
        )
    }
    Box(
        Modifier
            // Keep the origin slot fixed while the actual bar follows the pointer.
            // A themed placeholder masks the vacated dock area in both notes and previews.
            .zIndex(if (dragging) 10f else 0f)
            .onGloballyPositioned { barBounds = it.boundsInRoot() }
            .then(gesture),
    ) {
        if (destinationOffset != null) {
            Box(
                Modifier.offset {
                    IntOffset(destinationOffset.x.roundToInt(), destinationOffset.y.roundToInt())
                }.width(placement.width.dp).height(placement.height.dp)
                    .background(palette.accent.toComposeColor().copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                    .border(2.dp, palette.accent.toComposeColor(), RoundedCornerShape(12.dp)),
            )
        }
        if (dragging) {
            Box(
                Modifier.matchParentSize()
                    .background(palette.panel.toComposeColor(), RoundedCornerShape(12.dp))
                    .border(1.dp, palette.accent.toComposeColor().copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
            )
        }
        Box(Modifier.offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }) {
            content()
        }
    }
}

/** Floating bars auto-pack along screen edges and overflow to free lanes, never covering each other. */
@Composable
fun BoxScope.WorkspaceFloatingBars(
    workspace: ToolbarWorkspace,
    onMoveBar: (id: String, anchor: ToolbarAnchor) -> Unit,
    primaryContent: @Composable () -> Unit,
    content: @Composable (ToolbarBar) -> Unit,
    colorCount: Int = 5,
) {
    var paneBounds by remember { mutableStateOf(Rect.Zero) }
    val baseLook = LocalToolbarLook.current
    val showPrimary = workspace.hasMultiDock(baseLook)
    BoxWithConstraints(Modifier.fillMaxSize().zIndex(10f).onGloballyPositioned { paneBounds = it.boundsInRoot() }) {
        val plan = toolbarFloatingPlan(workspace, baseLook, maxWidth, maxHeight, colorCount)
        for ((index, bar) in workspace.bars.withIndex()) {
            val placement = plan.placement(bar.id) ?: continue
            // Legacy floating primary is already rendered by ToolbarAround.
            if (index == 0 && !bar.geometry.floating && !showPrimary) continue
            key(bar.id) {
                val look = ToolbarLook(
                    position = placement.edge.toolbarPosition(),
                    size = toolbarBarSize(bar, index, baseLook),
                    floating = true,
                )
                Box(Modifier.offset(x = placement.x.dp, y = placement.y.dp)
                    .width(placement.width.dp).height(placement.height.dp)) {
                    ToolbarDragSurface(
                        id = bar.id,
                        anchor = bar.geometry.anchor,
                        movable = bar.geometry.movable,
                        floating = true,
                        paneBounds = paneBounds,
                        workspace = workspace,
                        onMoveBar = onMoveBar,
                        colorCount = colorCount,
                    ) {
                        CompositionLocalProvider(LocalToolbarLook provides look) {
                            if (index == 0) primaryContent() else content(bar)
                        }
                    }
                }
            }
        }
    }
}

internal fun ToolbarAnchor.alignment(): Alignment = when (this) {
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

internal fun ToolbarAnchor.toolbarPosition(): ToolbarPosition = when (this) {
    ToolbarAnchor.CENTER_LEFT -> ToolbarPosition.LEFT
    ToolbarAnchor.CENTER_RIGHT -> ToolbarPosition.RIGHT
    ToolbarAnchor.BOTTOM_LEFT, ToolbarAnchor.BOTTOM_CENTER, ToolbarAnchor.BOTTOM_RIGHT -> ToolbarPosition.BOTTOM
    else -> ToolbarPosition.TOP
}
