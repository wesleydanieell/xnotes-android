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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.toSize
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
import com.xnotes.core.workspace.resolveTabDrop
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
    onReorder: (Int, Int) -> Unit,
    onSplit: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
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
    BoxWithConstraints(modifier.fillMaxWidth().onGloballyPositioned { barOrigin[0] = it.positionInWindow() }) {
        val visibleSlots = visibleTabSlots(maxWidth.value, tabs.tabs.size)
        val tabWidth = maxWidth / visibleSlots
        val animatedWidth by animateDpAsState(tabWidth, tween(240), label = "tabWidth")
        val activeIndex = tabs.tabs.indexOfFirst { it.uri == activeUri }
        val density = LocalDensity.current
        val scrollState = rememberScrollState()
        val barWidthPx = with(density) { maxWidth.toPx() }
        val slotWidthPx = with(density) { animatedWidth.toPx() }
        val barHeightPx = with(density) { 42.dp.toPx() }
        val splitThresholdPx = with(density) { 48.dp.toPx() }
        val (highlightXDp, highlightWidthDp) = tabHighlightBounds(animatedWidth.value, activeIndex)
        val targetX = with(density) { highlightXDp.dp.toPx() }
        val targetWidth = with(density) { highlightWidthDp.dp.toPx() }
        val highlightMotion = spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)
        val highlightX by animateFloatAsState(targetX, highlightMotion, label = "tabHighlightX")
        val highlightWidth by animateFloatAsState(targetWidth, highlightMotion, label = "tabHighlightWidth")
    Row(
        Modifier.fillMaxWidth()
            .background(palette.bg.toComposeColor())
            .horizontalScroll(scrollState)
            .height(42.dp)
            .drawBehind {
                if (activeIndex >= 0 && highlightWidth > 0f) {
                    val inset = 2.dp.toPx()
                    drawRoundRect(
                        highlight,
                        topLeft = Offset(highlightX, inset),
                        size = Size(highlightWidth, size.height - inset * 2),
                        cornerRadius = CornerRadius((size.height - inset * 2) / 2),
                    )
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.tabs.forEach { tab ->
            key(tab.uri) {
            val tabTint by animateColorAsState(
                targetValue = if (tab.uri == activeUri) palette.selectionForeground.toComposeColor()
                    else palette.textDim.toComposeColor(),
                animationSpec = tween(300),
                label = "tabTint",
            )
            Row(
                Modifier.width(animatedWidth)
                    .padding(horizontal = 2.dp)
                    .graphicsLayer { alpha = if (draggingUri == tab.uri) 0.55f else 1f }
                    .clip(RoundedCornerShape(8.dp)),
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
                                    when (val drop = resolveTabDrop(
                                        fromIndex = tabs.tabs.indexOfFirst { it.uri == tab.uri },
                                        x = position.x, y = position.y,
                                        barLeft = barOrigin[0].x, barTop = barOrigin[0].y,
                                        barWidth = barWidthPx, barHeight = barHeightPx,
                                        slotWidth = slotWidthPx, scrollOffset = scrollState.value.toFloat(),
                                        tabCount = tabs.tabs.size, splitThreshold = splitThresholdPx,
                                    )) {
                                        is TabDrop.Reorder -> reorderNow(drop.from, drop.to)
                                        is TabDrop.Split -> splitNow(tab.uri, drop.onRight)
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
    // An untouchable popup follows the pointer beyond the strip into either editor pane.
    // It never steals the gesture from the tab that started the drag.
    draggingUri?.let { uri ->
        val tab = tabs.tabs.firstOrNull { it.uri == uri }
        if (tab != null) {
            val overEditor = dragPoint.y >= barOrigin[0].y + barHeightPx + splitThresholdPx
            val destination = when {
                !overEditor -> "Reorder tab"
                dragPoint.x < barOrigin[0].x + barWidthPx / 2 -> "Split to left"
                else -> "Split to right"
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
                        .border(1.dp, palette.selectionBackground.toComposeColor(), RoundedCornerShape(12.dp))
                        .background(palette.bg.toComposeColor(), RoundedCornerShape(12.dp))
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
