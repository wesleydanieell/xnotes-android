package com.xnotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.Tool
import com.xnotes.core.tools.ToolbarGroupConfig
import com.xnotes.core.tools.ToolbarGroupDisplay
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

/**
 * One group is one movable layout entry. Compact: tap activates last tool, hold opens choices.
 * Expanded: each tool gets its own normal-size control. All gestures are consumed by the child
 * before a parent floating toolbar can interpret a long-press as a move.
 */
@Composable
internal fun CustomToolGroup(
    config: ToolbarGroupConfig?,
    armedTool: Tool,
    onChooseTool: (Tool) -> Unit,
) {
    val group = config?.valid() ?: return
    val choices = group.tools.mapNotNull { item ->
        Tool.fromId(item.id)?.let { item to it }
    }
    if (choices.isEmpty()) return
    var lastUsedId by rememberSaveable(group.id) { mutableStateOf(choices.first().first.id) }
    LaunchedEffect(armedTool, group.tools) {
        if (choices.any { it.second == armedTool }) lastUsedId = armedTool.id
    }
    val last = choices.firstOrNull { it.first.id == lastUsedId } ?: choices.first()
    val vertical = LocalBar.current.vertical
    val choose: (Pair<ToolbarItem, Tool>) -> Unit = {
        lastUsedId = it.first.id
        onChooseTool(it.second)
    }
    if (group.display == ToolbarGroupDisplay.EXPANDED) {
        val buttons: @Composable () -> Unit = {
            choices.forEach { choice ->
                ToolbarIcon(
                    itemIcon(choice.first),
                    stringResource(choice.second.labelRes),
                    active = armedTool == choice.second,
                    glideKey = choice.second,
                    onClick = { choose(choice) },
                )
            }
        }
        if (vertical) Column(verticalArrangement = Arrangement.spacedBy(LocalBar.current.spacing)) { buttons() }
        else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LocalBar.current.spacing)) { buttons() }
    } else {
        val metrics = LocalBar.current
        val palette = LocalPalette.current
        val direction = LocalToolbarLook.current.position.penFanDirection()
        val haptics = LocalHapticFeedback.current
        val active = armedTool == last.second
        val centers = remember(group.id) { mutableStateMapOf<Int, Offset>() }
        var originOnScreen by remember { mutableStateOf(Offset.Zero) }
        var hover by remember { mutableIntStateOf(-1) }
        var open by remember { mutableStateOf(false) }
        val chooseLatest by rememberUpdatedState(choose)
        Box(Modifier.size(metrics.button), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(metrics.button)
                    .onGloballyPositioned { originOnScreen = it.positionOnScreen() }
                    .semantics {
                        role = Role.Button
                        contentDescription = "Grupo de ferramentas"
                        onClick { chooseLatest(last); true }
                    }
                    .pointerInput(group.id, group.tools) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume() // A group owns its hold, never the parent toolbar's drag.
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
                                true -> chooseLatest(last)
                                false -> Unit
                                null -> {
                                    hover = -1
                                    centers.clear()
                                    open = true
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    var tracking = true
                                    while (tracking) {
                                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                        if (change == null) tracking = false
                                        else {
                                            val target = penFanVisibleTarget(
                                                pointerOnScreen = originOnScreen + change.position,
                                                centersOnScreen = centers,
                                                count = choices.size,
                                                maxDistance = 35.dp.toPx(),
                                            )
                                            if (target != null && target != hover)
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            hover = target ?: -1
                                            if (!change.pressed) {
                                                if (target != null) chooseLatest(choices[target])
                                                tracking = false
                                            } else change.consume()
                                        }
                                    }
                                    open = false
                                    hover = -1
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(metrics.circle).clip(CircleShape)
                        .background((if (active) palette.selectionBackground else palette.surface).toComposeColor()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        itemIcon(last.first),
                        contentDescription = null,
                        tint = (if (active) palette.selectionForeground else palette.textDim).toComposeColor(),
                        modifier = Modifier.size(metrics.icon),
                    )
                }
            }
            if (open) {
                Popup(
                    alignment = when (direction) {
                        PenFanDirection.DOWN -> Alignment.TopCenter
                        PenFanDirection.UP -> Alignment.BottomCenter
                        PenFanDirection.RIGHT -> Alignment.CenterStart
                        PenFanDirection.LEFT -> Alignment.CenterEnd
                    },
                    onDismissRequest = { open = false },
                    properties = PopupProperties(focusable = false),
                ) {
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp))
                            .background(palette.menuBg.toComposeColor()).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        choices.chunked(4).forEachIndexed { rowIndex, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                row.forEachIndexed { columnIndex, choice ->
                                    val index = rowIndex * 4 + columnIndex
                                    Box(
                                        Modifier.size(44.dp)
                                            .onGloballyPositioned { coords ->
                                                centers[index] = coords.localToScreen(
                                                    Offset(coords.size.width / 2f, coords.size.height / 2f),
                                                )
                                            }
                                            .clip(CircleShape)
                                            .background((if (hover == index) palette.selectionBackground else palette.surface).toComposeColor()),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            itemIcon(choice.first),
                                            contentDescription = stringResource(choice.second.labelRes),
                                            tint = (if (hover == index) palette.selectionForeground else palette.text).toComposeColor(),
                                            modifier = Modifier.size(22.dp),
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
