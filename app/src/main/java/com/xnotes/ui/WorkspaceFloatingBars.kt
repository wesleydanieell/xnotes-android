package com.xnotes.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarScale
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.core.tools.toolbarAnchorRegions
import com.xnotes.core.tools.toolbarDropTarget
import com.xnotes.core.tools.toolbarStackBarLimit
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
    onLongPress: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var drag by remember(id) { mutableStateOf(Offset.Zero) }
    var barBounds by remember(id) { mutableStateOf(Rect.Zero) }
    var dragStart by remember(id) { mutableStateOf(Offset.Zero) }
    val gesture = Modifier.pointerInput(id, anchor, paneBounds, movable) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val held = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
            dragStart = held.position
            drag = Offset.Zero
            var released = false
            try {
                while (true) {
                    // Claim movement before the nested toolbar scroll sees it. Before the hold,
                    // ordinary taps, scrolls and controls' own gestures keep their normal priority.
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == held.id } ?: break
                    if (change.isConsumed) break
                    val delta = change.positionChange()
                    if (!change.pressed) {
                        released = true
                        change.consume()
                        break
                    }
                    drag += delta
                    change.consume()
                }
                val moved = drag
                if (movable && released && moved.getDistance() >= 12f && barBounds != Rect.Zero && paneBounds != Rect.Zero) {
                    val drop = barBounds.topLeft + dragStart + moved - paneBounds.topLeft
                    toolbarDropTarget(drop.x, drop.y, paneBounds.width, paneBounds.height)?.let {
                        onMoveBar(id, it)
                    }
                } else if (released && moved.getDistance() < 12f) {
                    onLongPress()
                }
            } finally {
                drag = Offset.Zero
            }
        }
    }
    Box(
        Modifier
            .absoluteOffset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
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
    onUpdateBar: (ToolbarBar) -> Unit = {},
    onDuplicateBar: (ToolbarBar) -> Unit = {},
    onHideBar: (ToolbarBar) -> Unit = {},
    primaryContent: @Composable () -> Unit,
    content: @Composable (ToolbarBar) -> Unit,
) {
    var paneBounds by remember { mutableStateOf(Rect.Zero) }
    var contextBarId by remember { mutableStateOf<String?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { paneBounds = it.boundsInRoot() }) {
        val activeAnchors = workspace.bars.withIndex().filter { (index, bar) ->
            bar.visible && (index > 0 || bar.geometry.floating)
        }.map { it.value.geometry.anchor }.toSet()
        val regions = toolbarAnchorRegions(activeAnchors, maxWidth.value, maxHeight.value)
        for (anchor in ToolbarAnchor.entries) {
            val bars = workspace.bars.withIndex().filter { (index, bar) ->
                bar.visible && bar.geometry.anchor == anchor && (index > 0 || bar.geometry.floating)
            }
            if (bars.isEmpty()) continue
            val region = regions[anchor] ?: continue
            Box(Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset(region.x.dp, region.y.dp).size(region.width.dp, region.height.dp)) {
                // Keep a long vertical toolbar from consuming all space before later bars
                // are measured. The outer scroll exposes further bars if the stack is taller
                // than the pane; each bar retains its own bounded tool scroll.
                val availableHeight = (region.height.dp - 16.dp).coerceAtLeast(0.dp)
                val barMaxHeight = toolbarStackBarLimit(availableHeight.value, bars.size, 8f).dp
                ToolbarStackScroll(
                    maxHeight = availableHeight,
                    modifier = Modifier.align(anchor.alignment()).padding(8.dp),
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
                            Box(Modifier.heightIn(max = barMaxHeight)) {
                                ToolbarDragSurface(
                                    id = bar.id,
                                    anchor = anchor,
                                    movable = bar.geometry.movable,
                                    paneBounds = paneBounds,
                                    onMoveBar = onMoveBar,
                                    onLongPress = { contextBarId = bar.id },
                                ) {
                                    CompositionLocalProvider(
                                        LocalToolbarLook provides look,
                                        LocalToolbarGeometry provides bar.geometry.constrained(),
                                        LocalToolbarColorCount provides bar.colorCount,
                                    ) {
                                        if (index == 0) primaryContent() else content(bar)
                                    }
                                }
                                DropdownMenu(
                                    expanded = contextBarId == bar.id,
                                    onDismissRequest = { contextBarId = null },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (bar.geometry.movable) "Lock" else "Unlock") },
                                        onClick = {
                                            contextBarId = null
                                            onUpdateBar(bar.copy(geometry = bar.geometry.copy(movable = !bar.geometry.movable)))
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Duplicate") },
                                        onClick = { contextBarId = null; onDuplicateBar(bar) },
                                    )
                                    if (index > 0) {
                                        DropdownMenuItem(
                                            text = { Text("Hide") },
                                            onClick = { contextBarId = null; onHideBar(bar) },
                                        )
                                    }
                                }
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
