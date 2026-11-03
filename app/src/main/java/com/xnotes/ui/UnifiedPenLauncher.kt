package com.xnotes.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.res.stringResource
import com.xnotes.R
import com.xnotes.core.tools.Tool
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.core.tools.ToolbarToolGroup
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Pinterest-style radial shortcut: a tap arms the last pen, a second tap opens the full
 * Samsung Notes-inspired settings panel. Long-press shows a compact circular fan; drag and
 * lift chooses the hovered pen. Lifting without a choice ALWAYS dismisses the fan.
 */
@Composable
internal fun UnifiedPenLauncher(
    host: ToolPopupHost,
    armedTool: Tool,
    ink: Color,
    onChooseTool: (Tool) -> Unit,
    onSelectColor: (Int) -> Unit,
) {
    val group = remember { ToolbarToolGroup.defaultWritingGroup() }
    val direction = LocalToolbarLook.current.position.penFanDirection()
    val palette = LocalPalette.current
    val metrics = LocalBar.current
    val haptics = LocalHapticFeedback.current
    var lastId by rememberSaveable { mutableStateOf(ToolbarItem.PEN.id) }
    val lastItem = group.items.firstOrNull { it.id == lastId } ?: ToolbarItem.PEN
    var fanOpen by remember { mutableStateOf(false) }
    var fanMounted by remember { mutableStateOf(false) }
    var detailsOpen by remember { mutableStateOf(false) }
    var tappedOnce by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableIntStateOf(-1) }

    val fanProgress = remember { Animatable(0f) }
    LaunchedEffect(fanOpen) {
        if (fanOpen) {
            fanMounted = true
            fanProgress.snapTo(0f)
            fanProgress.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            )
        } else if (fanMounted) {
            fanProgress.animateTo(0f, tween(durationMillis = 125))
            fanMounted = false
        }
    }

    val choose by rememberUpdatedState<(ToolbarItem) -> Unit> { item ->
        val tool = Tool.fromId(item.id)
        if (tool != null && item in group.items) {
            lastId = item.id
            fanOpen = false
            hoveredIndex = -1
            onChooseTool(tool)
        }
    }
    // A separate pen button updates the shortcut too, but not the second-tap state.
    LaunchedEffect(armedTool) {
        val activeItem = group.items.firstOrNull { it.id == armedTool.id }
        if (activeItem == null) {
            tappedOnce = false
        } else if (activeItem.id != lastId) {
            lastId = activeItem.id
            tappedOnce = false
        }
    }
    val tap by rememberUpdatedState {
        if (tappedOnce && armedTool.id == lastItem.id) {
            detailsOpen = true
            tappedOnce = false
        } else {
            choose(lastItem)
            tappedOnce = true
        }
    }

    val active = armedTool.id == lastItem.id
    val label = stringResource(R.string.toolbar_unified_pen)
    Box(Modifier.size(metrics.button), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(metrics.button)
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) palette.selectionBackground.toComposeColor() else Color.Transparent)
                .semantics {
                    role = Role.Button
                    contentDescription = label
                    onClick { tap(); true }
                }
                .pointerInput(direction, group.items) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume() // The pen owns its hold; the whole toolbar must not move.
                        val quickRelease = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            var result: Boolean? = null
                            while (result == null) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                result = when {
                                    change == null -> false
                                    !change.pressed -> true
                                    change.isConsumed -> false
                                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop -> false
                                    else -> null
                                }
                            }
                            result
                        }
                        when (quickRelease) {
                            true -> tap()
                            false -> Unit
                            null -> {
                                fanOpen = true
                                detailsOpen = false
                                tappedOnce = false
                                hoveredIndex = -1
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val minForward = 24.dp.toPx()
                                var tracking = true
                                while (tracking) {
                                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                    if (change == null) {
                                        tracking = false
                                    } else {
                                        val target = penFanTarget(
                                            displacement = change.position - down.position,
                                            direction = direction,
                                            count = group.items.size,
                                            minForward = minForward,
                                        )
                                        val next = target ?: -1
                                        if (next != hoveredIndex && next >= 0) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        hoveredIndex = next
                                        if (!change.pressed) {
                                            target?.let { choose(group.items[it]) }
                                            tracking = false
                                        } else {
                                            change.consume()
                                        }
                                    }
                                }
                                // No sticky fan: a release at the origin also closes it.
                                fanOpen = false
                                hoveredIndex = -1
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            PenSilhouette(lastItem, ink, Modifier.width(21.dp).height(31.dp))
            Canvas(Modifier.align(Alignment.BottomEnd).padding(1.dp).size(7.dp)) {
                drawCircle(ink, radius = size.minDimension / 2f)
            }
        }
        if (fanMounted) {
            Popup(
                alignment = when (direction) {
                    PenFanDirection.DOWN -> Alignment.TopCenter
                    PenFanDirection.UP -> Alignment.BottomCenter
                    PenFanDirection.RIGHT -> Alignment.CenterStart
                    PenFanDirection.LEFT -> Alignment.CenterEnd
                },
                onDismissRequest = { fanOpen = false },
                properties = PopupProperties(focusable = false),
            ) {
                PenFanPalette(
                    items = group.items,
                    direction = direction,
                    selected = hoveredIndex,
                    active = lastItem,
                    ink = ink,
                    progress = fanProgress.value,
                )
            }
        }
        if (detailsOpen) {
            UnifiedPenDetails(
                host = host,
                items = group.items,
                current = lastItem,
                ink = ink,
                onChoose = { choose(it) },
                onSelectColor = onSelectColor,
                onDismiss = { detailsOpen = false },
            )
        }
    }
}

@Composable
private fun PenFanPalette(
    items: List<ToolbarItem>,
    direction: PenFanDirection,
    selected: Int,
    active: ToolbarItem,
    ink: Color,
    progress: Float,
) {
    val horizontal = direction == PenFanDirection.DOWN || direction == PenFanDirection.UP
    val width = if (horizontal) 260.dp else 185.dp
    val height = if (horizontal) 185.dp else 260.dp
    val originX = if (horizontal) width / 2 else if (direction == PenFanDirection.RIGHT) 35.dp else width - 35.dp
    val originY = if (!horizontal) height / 2 else if (direction == PenFanDirection.DOWN) 35.dp else height - 35.dp
    val palette = LocalPalette.current
    val surface = palette.panel.toComposeColor()
    val stroke = palette.border.toComposeColor()

    Box(Modifier.width(width).height(height)) {
        items.forEachIndexed { index, item ->
            // 110 dp radius provides clear spacing, even when the hovered circle grows.
            val displacement = penFanOffset(index, items.size, direction, 110f)
            val reveal = progress.coerceIn(0f, 1f)
            val x = originX + (displacement.x * reveal).dp - 23.dp
            val y = originY + (displacement.y * reveal).dp - 23.dp
            val hovered = index == selected
            val scale by animateFloatAsState(
                targetValue = if (hovered) 1.17f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "pen-fan-hover",
            )
            Box(
                Modifier
                    .offset(x = x, y = y)
                    .size(46.dp)
                    .graphicsLayer {
                        alpha = reveal
                        scaleX = (0.65f + 0.35f * reveal) * scale
                        scaleY = (0.65f + 0.35f * reveal) * scale
                    }
                    .clip(CircleShape)
                    .background(if (hovered) palette.selectionBackground.toComposeColor() else surface)
                    .border(
                        if (hovered) 2.5.dp else 1.2.dp,
                        if (hovered || item == active) ink else stroke,
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                PenSilhouette(item, ink, Modifier.width(20.dp).height(34.dp))
            }
        }
        if (selected in items.indices) {
            val name = stringResource(items[selected].labelRes)
            Box(Modifier.align(Alignment.Center).padding(4.dp), contentAlignment = Alignment.Center) {
                Text(name, color = palette.text.toComposeColor(), fontSize = 12.sp)
            }
        }
    }
}

/** A stable, vector-based nib shared by the compact fan, detailed panel, and editor preview. */
@Composable
internal fun PenSilhouette(item: ToolbarItem, ink: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val graphite = Color(0xFF343942)
        val steel = Color(0xFFD8DDE4)
        val silver = Color(0xFF919BA8)
        val beige = Color(0xFFF4CD96)
        val highlighter = item == ToolbarItem.HIGHLIGHTER
        val pencil = item == ToolbarItem.SPEED
        val brush = item == ToolbarItem.TAPER
        val fountain = item == ToolbarItem.PEN || item == ToolbarItem.CALLIGRAPHY
        val l = if (highlighter) w * .2f else w * .28f
        val r = if (highlighter) w * .8f else w * .72f
        val bodyTop = if (brush) h * .42f else h * .34f
        // Keep every illustrated pen vertical; type differences are in the nib, not rotation.
        rotate(0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        if (highlighter) Color(0xFFBFC7D2) else if (pencil) beige else Color(0xFF222832),
                        if (highlighter) Color(0xFFF3F5F8) else if (pencil) Color(0xFFFFE0A6) else Color(0xFF555E6A),
                        if (highlighter) silver else if (pencil) Color(0xFFD29B60) else graphite,
                    ),
                ),
                topLeft = Offset(l, bodyTop),
                size = Size(r - l, h * .58f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .07f),
            )
            drawLine(
                color = ink.copy(alpha = if (highlighter) .8f else 1f),
                start = Offset(l + w * .025f, h * .71f),
                end = Offset(r - w * .025f, h * .71f),
                strokeWidth = h * .105f,
                cap = StrokeCap.Round,
            )
            drawLine(Color.White.copy(alpha = .35f), Offset(l + w * .07f, h * .43f), Offset(l + w * .07f, h * .64f), w * .035f)
            val tip = Path().apply {
                if (highlighter) {
                    moveTo(l, bodyTop); lineTo(r, bodyTop); lineTo(r - w * .10f, h * .10f); lineTo(l, h * .17f)
                } else if (pencil) {
                    moveTo(l, bodyTop); lineTo(r, bodyTop); lineTo(w * .5f, h * .08f)
                } else if (brush) {
                    moveTo(l, bodyTop); lineTo(r, bodyTop); lineTo(w * .59f, h * .02f); lineTo(w * .49f, h * .13f)
                } else if (fountain) {
                    moveTo(l, bodyTop); lineTo(r, bodyTop); lineTo(w * .51f, h * .03f)
                } else {
                    moveTo(l, bodyTop); lineTo(r, bodyTop); lineTo(w * .5f, h * .09f)
                }
                close()
            }
            drawPath(
                tip,
                when {
                    highlighter -> ink.copy(alpha = .87f)
                    pencil -> beige
                    brush -> silver
                    fountain -> steel
                    else -> silver
                },
            )
            if (fountain) {
                drawLine(graphite, Offset(w * .51f, h * .05f), Offset(w * .50f, h * .27f), w * .025f)
                drawCircle(ink, radius = w * .035f, center = Offset(w * .5f, h * .24f))
                if (item == ToolbarItem.CALLIGRAPHY) {
                    drawLine(ink, Offset(w * .37f, h * .20f), Offset(w * .62f, h * .11f), w * .07f)
                }
            }
            if (pencil) {
                drawPath(Path().apply {
                    moveTo(w * .50f, h * .075f); lineTo(w * .43f, h * .16f); lineTo(w * .57f, h * .16f); close()
                }, graphite)
            }
            if (brush) {
                drawLine(ink, Offset(w * .5f, h * .055f), Offset(w * .5f, h * .23f), w * .04f, cap = StrokeCap.Round)
            }
            if (item == ToolbarItem.DASHED) {
                for (i in 0..2) {
                    drawCircle(ink, radius = w * .035f, center = Offset(w * .5f, h * (.41f + i * .075f)))
                }
            }
        }
    }
}

@Composable
internal fun UnifiedPenPreview(ink: Color, onSelect: () -> Unit) {
    val metrics = LocalBar.current
    Box(
        Modifier.size(metrics.button)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        PenSilhouette(ToolbarItem.PEN, ink, Modifier.width(21.dp).height(31.dp))
        Canvas(Modifier.align(Alignment.BottomEnd).padding(1.dp).size(7.dp)) {
            drawCircle(ink, radius = size.minDimension / 2f)
        }
    }
}
