package com.xnotes.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.R
import kotlin.math.roundToInt
import androidx.compose.ui.text.style.TextOverflow
import com.xnotes.core.workspace.NoteTabs
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

/**
 * Independent workspace bar. It depends on tab metadata and actions, not on the page toolbar,
 * canvas, or Editor. A future customizable toolbar host can mount or replace this component.
 */
@Composable
fun NoteTabBar(
    tabs: NoteTabs,
    activeUri: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    modifier: Modifier = Modifier,
    floatingSurface: Boolean = false,
    rotated: Boolean = false,
    onReorder: ((Int, Int) -> Unit)? = null,
    onSplit: ((String) -> Unit)? = null,
    splitDrag: DocumentTabSplitDrag? = null,
) {
    val palette = LocalPalette.current
    val highlight = palette.selectionBackground.toComposeColor()
    val density = LocalDensity.current
    val splitDragNow by rememberUpdatedState(splitDrag)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val visibleSlots = visibleTabSlots(maxWidth.value, tabs.tabs.size)
        val slotWidth = maxWidth / visibleSlots
        val animatedWidth by animateDpAsState(slotWidth, tween(240), label = "tabWidth")
        val axisPixels = with(density) { animatedWidth.toPx() }
        val activeIndex = tabs.tabs.indexOfFirst { it.uri == activeUri }
        val scrollState = rememberScrollState()
        val viewportPixels = with(density) { maxWidth.toPx() }
        LaunchedEffect(activeIndex, axisPixels, viewportPixels, scrollState.maxValue) {
            if (activeIndex >= 0 && axisPixels > 0f && viewportPixels > 0f) {
                val start = activeIndex * axisPixels
                val end = start + axisPixels
                val visibleStart = scrollState.value.toFloat()
                val visibleEnd = visibleStart + viewportPixels
                val requested = when {
                    start < visibleStart -> start
                    end > visibleEnd -> end - viewportPixels
                    else -> visibleStart
                }
                scrollState.animateScrollTo(requested.roundToInt().coerceIn(0, scrollState.maxValue))
            }
        }
        val highlightMotion = spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)
        val target = if (activeIndex < 0) 0f else activeIndex * axisPixels + with(density) { 2.dp.toPx() }
        val highlightOffset by animateFloatAsState(target, highlightMotion, label = "tabHighlightOffset")
        val highlightExtent by animateFloatAsState((axisPixels - with(density) { 4.dp.toPx() }).coerceAtLeast(0f),
            highlightMotion, label = "tabHighlightExtent")
        val highlightShape = MaterialTheme.shapes.large
        val base = Modifier
            .background((if (floatingSurface) palette.panel else palette.bg).toComposeColor())
            .horizontalScroll(scrollState)
            .drawBehind {
                if (activeIndex >= 0 && highlightExtent > 0f) {
                    val inset = 2.dp.toPx()
                    val highlightSize = Size(highlightExtent, (size.height - inset * 2).coerceAtLeast(0f))
                    val radius = (highlightShape.createOutline(highlightSize, layoutDirection, this)
                        as? Outline.Rounded)?.roundRect?.topLeftCornerRadius ?: CornerRadius.Zero
                    drawRoundRect(
                        highlight,
                        topLeft = Offset(highlightOffset, inset),
                        size = highlightSize,
                        cornerRadius = radius,
                    )
                }
            }
        val tabCells: @Composable () -> Unit = {
            tabs.tabs.forEachIndexed { from, tab ->
                key(tab.uri) {
                    val tabTint by animateColorAsState(
                        if (tab.uri == activeUri) palette.selectionForeground.toComposeColor()
                        else palette.textDim.toComposeColor(), tween(300), label = "tabTint",
                    )
                    var menuExpanded by remember(tab.uri) { mutableStateOf(false) }
                    var dragDistance by remember(tab.uri) { mutableFloatStateOf(0f) }
                    val coordinates = remember(tab.uri) { arrayOfNulls<LayoutCoordinates>(1) }
                    val reorderGesture = if (onReorder == null && splitDrag == null) Modifier else Modifier.pointerInput(
                        tab.uri, tabs.tabs.size, axisPixels, rotated,
                    ) {
                        var pointer = Offset.Unspecified
                        fun reset() {
                            dragDistance = 0f
                            splitDragNow?.onPreview?.invoke(null)
                        }
                        detectDragGesturesAfterLongPress(
                            onDragStart = { local ->
                                dragDistance = 0f
                                pointer = coordinates[0]?.takeIf { it.isAttached }
                                    ?.localToRoot(local) ?: Offset.Unspecified
                            },
                            onDragEnd = {
                                val split = splitDragNow
                                val target = split?.target(pointer)
                                if (target != null) split.onDrop(tab.uri, target)
                                else if (split == null || split.canReorder(pointer)) {
                                    val shift = (dragDistance / axisPixels.coerceAtLeast(1f)).roundToInt()
                                    val to = (from + shift).coerceIn(0, tabs.tabs.lastIndex)
                                    if (to != from) onReorder?.invoke(from, to)
                                }
                                reset()
                            },
                            onDragCancel = ::reset,
                            onDrag = { change, delta ->
                                change.consume()
                                pointer += if (rotated) Offset(-delta.y, delta.x) else delta
                                val split = splitDragNow
                                val target = split?.target(pointer)
                                split?.onPreview?.invoke(target)
                                if (target == null) dragDistance += delta.x
                            },
                        )
                    }
                    Row(
                        Modifier.width(animatedWidth).height(42.dp)
                            .graphicsLayer { translationX = dragDistance }
                            .padding(horizontal = 2.dp).clip(MaterialTheme.shapes.small)
                            .testTag("document-tab-${tab.uri}")
                            .then(if (splitDrag == null) Modifier else Modifier.onGloballyPositioned { coordinates[0] = it })
                            .then(reorderGesture),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier.weight(1f).height(40.dp)
                                .clickable(
                                    interactionSource = remember(tab.uri) { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onSelect(tab.uri) },
                                ).padding(start = 8.dp, end = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val canvas = tab.uri.substringBefore('?').endsWith(".xcanvas", true) ||
                                tab.title.endsWith(".xcanvas", true)
                            Icon(if (canvas) Icons.Outlined.Gesture else Icons.AutoMirrored.Outlined.Article,
                                contentDescription = if (canvas) stringResource(R.string.document_tabs_canvas) else stringResource(R.string.document_tabs_note),
                                tint = tabTint, modifier = Modifier.size(16.dp))
                            Text(tab.title.replace(Regex("(?i)\\.(xnote|xcanvas)$"), ""),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp, color = tabTint,
                                modifier = Modifier.weight(1f).padding(start = 5.dp))
                        }
                        if (onSplit != null) {
                            Box {
                                TextButton(onClick = { menuExpanded = true }, modifier = Modifier.width(28.dp)) {
                                    Text("⋮", fontSize = 16.sp, color = tabTint)
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.document_tabs_open_split)) }, onClick = {
                                        menuExpanded = false; onSplit(tab.uri)
                                    })
                                    if (onReorder != null && from > 0) {
                                        DropdownMenuItem(text = { Text(stringResource(if (rotated) R.string.document_tabs_move_up else R.string.document_tabs_move_left)) }, onClick = {
                                            menuExpanded = false; onReorder(from, from - 1)
                                        })
                                    }
                                    if (onReorder != null && from < tabs.tabs.lastIndex) {
                                        DropdownMenuItem(text = { Text(stringResource(if (rotated) R.string.document_tabs_move_down else R.string.document_tabs_move_right)) }, onClick = {
                                            menuExpanded = false; onReorder(from, from + 1)
                                        })
                                    }
                                }
                            }
                        }
                        TextButton(onClick = { onClose(tab.uri) }, modifier = Modifier.width(32.dp)) {
                            Text("×", fontSize = 16.sp, color = tabTint)
                        }
                    }
                }
            }
        }
        Row(base.height(42.dp), verticalAlignment = Alignment.CenterVertically) { tabCells() }
    }
}

/** Equal-width tabs until five fit, but never below 144 dp per visible tab. */
internal fun visibleTabSlots(widthDp: Float, tabCount: Int): Int =
    minOf(tabCount.coerceAtLeast(1), 5, (widthDp / 144f).toInt().coerceAtLeast(1))

/** Stable indicator geometry independent of animation frames and removed-tab measurements. */
internal fun tabHighlightBounds(tabWidthDp: Float, activeIndex: Int): Pair<Float, Float> =
    (activeIndex.coerceAtLeast(0) * tabWidthDp + 2f) to (tabWidthDp - 4f).coerceAtLeast(0f)
