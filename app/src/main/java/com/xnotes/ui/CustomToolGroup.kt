package com.xnotes.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xnotes.core.tools.Tool
import com.xnotes.core.tools.ToolbarGroupConfig
import com.xnotes.core.tools.ToolbarGroupDisplay
import com.xnotes.core.tools.ToolbarGroupIcon
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.ui.icons.XnotesIcons
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

/**
 * An ordinary folder on the toolbar: tap repeats the last member, double-tap opens its tools.
 * Expanded presentation remains available for users who want always-visible buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
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
        val active = armedTool == last.second
        var open by remember(group.id) { mutableStateOf(false) }
        val interaction = remember { MutableInteractionSource() }
        Box(Modifier.size(metrics.button), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(metrics.button)
                    .clip(CircleShape)
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { choose(last) },
                        onDoubleClick = { open = !open },
                        // Consume a hold so a movable bar cannot steal the folder's gesture.
                        onLongClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(metrics.circle).clip(CircleShape)
                        .background((if (active) palette.selectionBackground else palette.surface).toComposeColor()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        toolbarGroupCoverIcon(group.icon, last.first),
                        contentDescription = group.name,
                        tint = (if (active) palette.selectionForeground else palette.textDim).toComposeColor(),
                        modifier = Modifier.size(metrics.icon),
                    )
                }
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                modifier = Modifier.width(294.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(toolbarGroupCoverIcon(group.icon, last.first), null, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(group.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text("${choices.size} ferramentas", style = MaterialTheme.typography.labelSmall,
                            color = palette.textDim.toComposeColor())
                    }
                }
                HorizontalDivider(color = palette.border.toComposeColor())
                choices.chunked(3).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        row.forEach { choice ->
                            val selected = choice.second == armedTool
                            Column(
                                Modifier.weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background((if (selected) palette.selectionBackground else palette.surface).toComposeColor())
                                    .clickable { open = false; choose(choice) }
                                    .padding(horizontal = 4.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    itemIcon(choice.first),
                                    contentDescription = null,
                                    tint = (if (selected) palette.selectionForeground else palette.text).toComposeColor(),
                                    modifier = Modifier.size(24.dp),
                                )
                                Text(
                                    stringResource(choice.second.labelRes),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = (if (selected) palette.selectionForeground else palette.text).toComposeColor(),
                                )
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** A folder has its own persistent icon; changing tools doesn't unexpectedly repaint its cover. */
@Composable
internal fun toolbarGroupCoverIcon(icon: ToolbarGroupIcon, lastUsed: ToolbarItem): ImageVector = when (icon) {
    ToolbarGroupIcon.FOLDER -> XnotesIcons.folder
    ToolbarGroupIcon.LAYERS -> XnotesIcons.layers
    ToolbarGroupIcon.PEN -> itemIcon(ToolbarItem.PEN)
    ToolbarGroupIcon.SHAPE -> XnotesIcons.shape
    ToolbarGroupIcon.BOOKMARK -> XnotesIcons.bookmark
    ToolbarGroupIcon.LAST_USED -> itemIcon(lastUsed)
}
