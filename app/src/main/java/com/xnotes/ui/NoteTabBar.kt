package com.xnotes.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import android.view.WindowManager
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.xnotes.core.workspace.NoteTabs
import com.xnotes.core.workspace.TabDrop
import com.xnotes.core.workspace.SplitSide
import com.xnotes.core.workspace.resolveTabDrop
import com.xnotes.core.workspace.insertionBoundary
import com.xnotes.core.workspace.paneDropSide
import androidx.compose.ui.platform.LocalConfiguration
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

/** Window-space bounds of the actual editor workspace, independent of toolbar/pane widths. */
data class DocumentDropArea(val origin: Offset, val size: IntSize)

/**
 * Independent workspace bar. It depends on tab metadata and actions, not on the page toolbar,
 * canvas, or Editor. Standalone navigation and customizable toolbar elements mount this component.
 */
@Composable
fun NoteTabBar(
    tabs: NoteTabs,
    activeUri: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onSplit: (String, SplitSide) -> Unit,
    modifier: Modifier = Modifier,
    floatingSurface: Boolean = false,
    dropArea: DocumentDropArea? = null,
    selection: DocumentTabsSelectionState? = null,
    vertical: Boolean = false,
) {
    val palette = LocalPalette.current
    // All tabs are equal-width, so the indicator can be positioned arithmetically.
    // Avoid feeding onGloballyPositioned measurements into Compose state each animation frame.
    val highlight = palette.selectionBackground.toComposeColor()
    val barOrigin = remember { arrayOf(Offset.Zero) }
    val tabOrigins = remember { HashMap<String, Offset>() }
    var draggingUri by remember { mutableStateOf<String?>(null) }
    var dragPoint by remember { mutableStateOf(Offset.Zero) }
    val reorderNow by rememberUpdatedState(onReorder)
    val splitNow by rememberUpdatedState(onSplit)
    BoxWithConstraints(modifier.then(if (vertical) Modifier.fillMaxHeight() else Modifier.fillMaxWidth()).onGloballyPositioned { barOrigin[0] = it.positionInWindow() }) {
        val axisLength = if (vertical) maxHeight else maxWidth
        val visibleSlots = visibleTabSlots(if (vertical) axisLength.value * 144f / 42f else axisLength.value, tabs.tabs.size)
        val tabWidth = if (vertical) 42.dp else axisLength / visibleSlots
        val animatedWidth by animateDpAsState(tabWidth, tween(240), label = "tabWidth")
        val activeIndex = tabs.tabs.indexOfFirst { it.uri == activeUri }
        val density = LocalDensity.current
        val scrollState = rememberScrollState()
        val barWidthPx = with(density) { axisLength.toPx() }
        val slotWidthPx = with(density) { animatedWidth.toPx() }
        val barHeightPx = with(density) { if (vertical) maxWidth.toPx() else 42.dp.toPx() }
        val splitThresholdPx = with(density) { 48.dp.toPx() }
        // The pointer detector may outlive width/scroll animations. Keep its drop resolver current
        // so releasing at either end uses the same geometry as the visible insertion preview.
        val resolveDrop: (String, Offset) -> TabDrop? = { uri, point ->
            com.xnotes.core.workspace.resolveTabStripDrop(
                fromIndex = tabs.tabs.indexOfFirst { it.uri == uri }, x = point.x, y = point.y,
                strip = com.xnotes.core.workspace.TabStripGeometry(
                    com.xnotes.core.workspace.TabStripBounds(barOrigin[0].x, barOrigin[0].y,
                        if (vertical) barHeightPx else barWidthPx, if (vertical) barWidthPx else barHeightPx),
                    slotWidthPx, scrollState.value.toFloat(), vertical),
                tabCount = tabs.tabs.size, splitThreshold = splitThresholdPx,
                workspace = dropArea?.let { com.xnotes.core.workspace.TabStripBounds(
                    it.origin.x, it.origin.y, it.size.width.toFloat(), it.size.height.toFloat()) },
            )
        }
        val currentResolveDrop by rememberUpdatedState(resolveDrop)
        val previewDrop = draggingUri?.let { uri -> resolveDrop(uri, dragPoint) }
        val insertionX = (previewDrop as? TabDrop.Reorder)?.let {
            insertionBoundary(it.from, it.to) * slotWidthPx
        }
        val (highlightXDp, highlightWidthDp) = tabHighlightBounds(animatedWidth.value, activeIndex)
        val targetX = with(density) { highlightXDp.dp.toPx() }
        val targetWidth = with(density) { highlightWidthDp.dp.toPx() }
        val highlightMotion = spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)
        val localHighlightX by animateFloatAsState(targetX, highlightMotion, label = "tabHighlightX")
        val highlightX = selection?.let {
            with(density) { (it.position.value * animatedWidth.value + 2f).dp.toPx() }
        } ?: localHighlightX
        val highlightWidth by animateFloatAsState(targetWidth, highlightMotion, label = "tabHighlightWidth")
    DocumentTabStrip(
        vertical = vertical,
        modifier = Modifier.fillMaxWidth()
            .background((if (floatingSurface) palette.panel else palette.bg).toComposeColor())
            .then(if (vertical) Modifier.fillMaxHeight().verticalScroll(scrollState)
                else Modifier.horizontalScroll(scrollState).height(42.dp))
            .drawBehind {
                if (activeIndex >= 0 && highlightWidth > 0f) {
                    val inset = 2.dp.toPx()
                    drawRoundRect(
                        highlight,
                        topLeft = if (vertical) Offset(inset, highlightX) else Offset(highlightX, inset),
                        size = if (vertical) Size(size.width - inset * 2, highlightWidth)
                            else Size(highlightWidth, size.height - inset * 2),
                        cornerRadius = CornerRadius((size.height - inset * 2) / 2),
                    )
                }
                insertionX?.let { x ->
                    val indicatorWidth = 4.dp.toPx()
                    drawRoundRect(
                        color = highlight,
                        topLeft = if (vertical) Offset(3.dp.toPx(), x - indicatorWidth / 2)
                            else Offset(x - indicatorWidth / 2, 3.dp.toPx()),
                        size = if (vertical) Size(size.width - 6.dp.toPx(), indicatorWidth)
                            else Size(indicatorWidth, size.height - 6.dp.toPx()),
                        cornerRadius = CornerRadius(indicatorWidth / 2),
                    )
                }
            },
    ) {
        tabs.tabs.forEach { tab ->
            key(tab.uri) {
            DisposableEffect(tab.uri) {
                onDispose { tabOrigins.remove(tab.uri) }
            }
            val localTabTint by animateColorAsState(
                targetValue = if (tab.uri == activeUri) palette.selectionForeground.toComposeColor()
                    else palette.textDim.toComposeColor(),
                animationSpec = tween(300),
                label = "tabTint",
            )
            val tabTint = selection?.let {
                androidx.compose.ui.graphics.lerp(palette.textDim.toComposeColor(),
                    palette.selectionForeground.toComposeColor(), it.weight(tab.uri))
            } ?: localTabTint
            Row(
                Modifier.then(if (vertical) Modifier.fillMaxWidth().height(animatedWidth) else Modifier.width(animatedWidth))
                    .padding(horizontal = 2.dp)
                    .graphicsLayer { alpha = if (draggingUri == tab.uri) 0.55f else 1f }
                    .clip(MaterialTheme.shapes.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f).height(40.dp)
                        .onGloballyPositioned { tabOrigins[tab.uri] = it.positionInWindow() }
                        .pointerInput(tab.uri, visibleSlots) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { local ->
                                    draggingUri = tab.uri
                                    dragPoint = (tabOrigins[tab.uri] ?: Offset.Zero) + local
                                },
                                onDrag = { change, delta ->
                                    change.consume()
                                    dragPoint += delta
                                },
                                onDragEnd = {
                                    val position = dragPoint
                                    draggingUri = null
                                    when (val drop = currentResolveDrop(tab.uri, position)) {
                                        is TabDrop.Reorder -> reorderNow(drop.from, drop.to)
                                        is TabDrop.Split -> splitNow(tab.uri, drop.side)
                                        null -> Unit
                                    }
                                },
                                onDragCancel = { draggingUri = null },
                            )
                        }
                        .clickable(
                            interactionSource = remember(tab.uri) { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(tab.uri) },
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val isCanvas = tab.uri.substringBefore('?').endsWith(".xcanvas", ignoreCase = true) ||
                        tab.title.endsWith(".xcanvas", ignoreCase = true)
                    Icon(
                        imageVector = if (isCanvas) Icons.Outlined.Gesture else Icons.AutoMirrored.Outlined.Article,
                        contentDescription = if (isCanvas) "Canvas" else "Note",
                        tint = tabTint,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        tab.title.replace(Regex("(?i)\\.(xnote|xcanvas)$"), ""),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        color = tabTint,
                        modifier = Modifier.weight(1f).padding(start = 6.dp),
                    )
                }
                TextButton(onClick = { onClose(tab.uri) }, modifier = Modifier.width(36.dp)) {
                    Text("×", fontSize = 16.sp, color = tabTint)
                }
            }
            }
        }
    }
    // Keep a single preview window alive throughout the entire drag. Recreating/repositioning
    // separate Popup windows on each edge change could miss frames during fast transitions.
    if (draggingUri != null) {
        val side = (previewDrop as? TabDrop.Split)?.side
        val screen = LocalConfiguration.current
        val screenWidth = screen.screenWidthDp.dp
        val screenHeight = screen.screenHeightDp.dp
        val previewLeft = with(density) { (dropArea?.origin?.x ?: barOrigin[0].x).toDp() }
        val previewTop = with(density) { (dropArea?.origin?.y ?: (barOrigin[0].y + barHeightPx)).toDp() }
        val previewWidth = dropArea?.let { with(density) { it.size.width.toDp() } } ?: maxWidth
        val previewHeight = dropArea?.let { with(density) { it.size.height.toDp() } }
            ?: (screenHeight - previewTop - 8.dp).coerceAtLeast(180.dp)
        val bounds = splitPreviewRect(side ?: SplitSide.LEFT, previewWidth, previewHeight)
        val targetX = previewLeft + bounds.x
        val targetY = previewTop + bounds.y
        val targetWidth = bounds.width
        val targetHeight = bounds.height
        val x by animateDpAsState(targetX, tween(130), label = "splitPreviewX")
        val y by animateDpAsState(targetY, tween(130), label = "splitPreviewY")
        val w by animateDpAsState(targetWidth, tween(130), label = "splitPreviewWidth")
        val h by animateDpAsState(targetHeight, tween(130), label = "splitPreviewHeight")
        val fixedPosition = remember {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect, windowSize: IntSize,
                    layoutDirection: LayoutDirection, popupContentSize: IntSize,
                ) = IntOffset.Zero
            }
        }
        Popup(popupPositionProvider = fixedPosition, properties = TAB_DRAG_POPUP) {
            Box(Modifier.width(screenWidth).height(screenHeight)) {
                if (side != null) {
                    Box(
                        Modifier.offset(x, y).width(w).height(h)
                            .background(highlight.copy(alpha = 0.13f), MaterialTheme.shapes.medium)
                            .border(2.dp, highlight, MaterialTheme.shapes.medium),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(when (side) {
                            SplitSide.LEFT -> "Left pane"
                            SplitSide.RIGHT -> "Right pane"
                            SplitSide.TOP -> "Top pane"
                            SplitSide.BOTTOM -> "Bottom pane"
                        }, color = highlight, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
    // An untouchable popup follows the pointer beyond the strip into either editor pane.
    // It never steals the gesture from the tab that started the drag.
    draggingUri?.let { uri ->
        val tab = tabs.tabs.firstOrNull { it.uri == uri }
        if (tab != null) {
            val destination = when (val drop = previewDrop) {
                is TabDrop.Split -> "Split to ${drop.side.name.lowercase()}"
                is TabDrop.Reorder -> "Move to position ${drop.to + 1}"
                null -> "Drag to reorder"
            }
            val popupPosition = remember(dragPoint) {
                object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize,
                    ): IntOffset = IntOffset(
                        (dragPoint.x + 20f).roundToInt().coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
                        (dragPoint.y + 20f).roundToInt().coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
                    )
                }
            }
            Popup(popupPositionProvider = popupPosition, properties = TAB_DRAG_POPUP) {
                Column(
                    Modifier.widthIn(min = 120.dp, max = 230.dp)
                        .border(1.dp, palette.selectionBackground.toComposeColor(), MaterialTheme.shapes.medium)
                        .background(palette.bg.toComposeColor(), MaterialTheme.shapes.medium)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val canvas = tab.title.endsWith(".xcanvas", true) || tab.uri.endsWith(".xcanvas", true)
                        Icon(if (canvas) Icons.Outlined.Gesture else Icons.AutoMirrored.Outlined.Article,
                            contentDescription = null, tint = palette.text.toComposeColor(), modifier = Modifier.size(18.dp))
                        Text(tab.title.replace(Regex("(?i)\\.(xnote|xcanvas)$"), ""),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = palette.text.toComposeColor(), modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(destination, fontSize = 11.sp,
                        color = palette.textDim.toComposeColor(), modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
    }
}

private val TAB_DRAG_POPUP = PopupProperties(
    flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
)

/** Equal-width tabs until five fit, but never below 144 dp per visible tab. */
internal fun visibleTabSlots(widthDp: Float, tabCount: Int): Int =
    minOf(tabCount.coerceAtLeast(1), 5, (widthDp / 144f).toInt().coerceAtLeast(1))

/** Stable indicator geometry independent of animation frames and removed-tab measurements. */
internal fun tabHighlightBounds(tabWidthDp: Float, activeIndex: Int): Pair<Float, Float> =
    (activeIndex.coerceAtLeast(0) * tabWidthDp + 2f) to (tabWidthDp - 4f).coerceAtLeast(0f)

@Composable
private fun DocumentTabStrip(modifier: Modifier, vertical: Boolean, content: @Composable () -> Unit) {
    if (vertical) Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) { content() }
    else Row(modifier, verticalAlignment = Alignment.CenterVertically) { content() }
}
