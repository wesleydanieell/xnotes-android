package com.xnotes.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarScale
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import com.xnotes.settings.ToolbarSize
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
    paneBounds: Rect,
    onMoveBar: (String, ToolbarAnchor) -> Unit,
    content: @Composable () -> Unit,
) {
    var drag by remember(id) { mutableStateOf(Offset.Zero) }
    var barBounds by remember(id) { mutableStateOf(Rect.Zero) }
    val gesture = if (!movable) Modifier else Modifier.pointerInput(id, anchor, paneBounds) {
        detectDragGesturesAfterLongPress(
            onDragStart = { drag = Offset.Zero },
            onDrag = { change, distance ->
                change.consume()
                drag += distance
            },
            onDragEnd = {
                val moved = drag
                drag = Offset.Zero
                if (moved.getDistance() >= 12f && barBounds != Rect.Zero && paneBounds != Rect.Zero) {
                    val center = barBounds.center + moved - paneBounds.topLeft
                    val width = paneBounds.width
                    val height = paneBounds.height
                    val col = when {
                        center.x < width / 3f -> 0
                        center.x > width * 2f / 3f -> 2
                        else -> 1
                    }
                    val row = when {
                        center.y < height / 3f -> 0
                        center.y > height * 2f / 3f -> 2
                        else -> 1
                    }
                    onMoveBar(id, ToolbarAnchor.entries[row * 3 + col])
                }
            },
            onDragCancel = { drag = Offset.Zero },
        )
    }
    Box(
        Modifier
            .offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
            .onGloballyPositioned { if (drag == Offset.Zero) barBounds = it.boundsInRoot() }
            .then(gesture),
    ) {
        content()
    }
}

/** Secondary bars plus a relocated primary share exactly the same gesture and anchors. */
@Composable
fun BoxScope.WorkspaceFloatingBars(
    workspace: ToolbarWorkspace,
    onMoveBar: (id: String, anchor: ToolbarAnchor) -> Unit,
    primaryContent: @Composable () -> Unit,
    content: @Composable (ToolbarBar) -> Unit,
) {
    var paneBounds by remember { mutableStateOf(Rect.Zero) }
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { paneBounds = it.boundsInRoot() }) {
        for (anchor in ToolbarAnchor.entries) {
            val bars = workspace.bars.withIndex().filter { (index, bar) ->
                bar.visible && bar.geometry.anchor == anchor && (index > 0 || bar.geometry.floating)
            }
            if (bars.isEmpty()) continue
            Column(
                modifier = Modifier.align(anchor.alignment()).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for ((index, bar) in bars) {
                    key(bar.id) {
                        val look = ToolbarLook(
                            position = anchor.toolbarPosition(),
                            size = when (bar.geometry.scale) {
                                ToolbarScale.COMPACT -> ToolbarSize.COMPACT
                                ToolbarScale.REGULAR -> ToolbarSize.REGULAR
                                ToolbarScale.COMFORTABLE -> ToolbarSize.COMFORTABLE
                            },
                            floating = true,
                        )
                        ToolbarDragSurface(
                            id = bar.id,
                            anchor = anchor,
                            movable = bar.geometry.movable,
                            paneBounds = paneBounds,
                            onMoveBar = onMoveBar,
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
