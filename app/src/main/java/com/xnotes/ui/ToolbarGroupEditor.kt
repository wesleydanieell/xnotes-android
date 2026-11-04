package com.xnotes.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarGroupConfig
import com.xnotes.core.tools.ToolbarGroupDisplay
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.ui.icons.XnotesIcons
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor
import kotlin.math.abs

/** Target available for moving a member directly between custom groups. */
internal data class GroupTarget(val section: Int, val entry: Int, val id: String, val name: String)

/** Inspector for one custom group; stable member order is saved with the bar layout. */
@Composable
internal fun ToolbarGroupEditor(
    config: ToolbarGroupConfig,
    allowed: Set<ToolbarItem>,
    otherGroups: List<GroupTarget>,
    onUpdate: (ToolbarGroupConfig) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onExtract: (Int) -> Unit,
    onTransfer: (Int, GroupTarget) -> Unit,
) {
    val palette = LocalPalette.current
    var addOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Grupo de ferramentas", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            Text("${config.tools.size} ferramentas", color = palette.textDim.toComposeColor(),
                style = MaterialTheme.typography.labelSmall)
        }
        OutlinedTextField(
            value = config.name,
            onValueChange = { onUpdate(config.copy(name = it)) },
            label = { Text("Nome do grupo") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = config.display == ToolbarGroupDisplay.COMPACT,
                onClick = { onUpdate(config.copy(display = ToolbarGroupDisplay.COMPACT)) },
                label = { Text("Compacto") },
            )
            FilterChip(
                selected = config.display == ToolbarGroupDisplay.EXPANDED,
                onClick = { onUpdate(config.copy(display = ToolbarGroupDisplay.EXPANDED)) },
                label = { Text("Expandido") },
            )
        }
        Text("Arraste pelo ícone para reordenar. Segure o grupo compacto e deslize até a ferramenta desejada.",
            color = palette.textDim.toComposeColor(), style = MaterialTheme.typography.bodySmall)
        config.tools.forEachIndexed { index, item ->
            val reorder by rememberUpdatedState(onReorder)
            val currentIndex by rememberUpdatedState(index)
            val threshold = with(LocalDensity.current) { 36.dp.toPx() }
            var dragged by remember(item) { mutableFloatStateOf(0f) }
            var transfersOpen by remember(item) { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    XnotesIcons.menu, "Segurar e arrastar",
                    tint = palette.textDim.toComposeColor(),
                    modifier = Modifier.size(36.dp).pointerInput(item) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { dragged = 0f },
                            onDrag = { change, delta ->
                                change.consume()
                                dragged += delta.y
                                if (abs(dragged) > threshold) {
                                    val target = (currentIndex + if (dragged > 0) 1 else -1)
                                        .coerceIn(0, config.tools.lastIndex)
                                    if (target != currentIndex) reorder(currentIndex, target)
                                    dragged = 0f
                                }
                            },
                            onDragEnd = { dragged = 0f },
                            onDragCancel = { dragged = 0f },
                        )
                    }.padding(10.dp),
                )
                Icon(itemIcon(item), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(item.labelRes), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                androidx.compose.foundation.layout.Box {
                    IconButton(onClick = { transfersOpen = true }) {
                        Icon(XnotesIcons.more, "Opções da ferramenta", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = transfersOpen, onDismissRequest = { transfersOpen = false }) {
                        otherGroups.forEach { target ->
                            DropdownMenuItem(text = { Text("Mover para ${target.name} · seção ${target.section + 1}") }, onClick = {
                                transfersOpen = false
                                onTransfer(index, target)
                            }, enabled = config.tools.size > 1)
                        }
                        DropdownMenuItem(text = { Text("Retirar para a barra") }, onClick = {
                            transfersOpen = false; onExtract(index)
                        }, enabled = config.tools.size > 1)
                        DropdownMenuItem(text = { Text("Remover do grupo") }, onClick = {
                            transfersOpen = false
                            onUpdate(config.copy(tools = config.tools.filterIndexed { i, _ -> i != index }))
                        }, enabled = config.tools.size > 1)
                    }
                }
            }
        }
        androidx.compose.foundation.layout.Box {
            TextButton(onClick = { query = ""; addOpen = true }) {
                Icon(XnotesIcons.plus, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Adicionar ferramenta")
            }
            DropdownMenu(expanded = addOpen, onDismissRequest = { addOpen = false }) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("Buscar ferramenta") },
                    modifier = Modifier.width(290.dp).padding(horizontal = 8.dp),
                )
                Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                    ToolbarGroupConfig.GROUPABLE_TOOLS.filter { it in allowed && it !in config.tools }
                        .filter { it.id.contains(query, true) || stringResource(it.labelRes).contains(query, true) }
                        .forEach { tool ->
                            DropdownMenuItem(
                                text = { Text(stringResource(tool.labelRes)) },
                                leadingIcon = { Icon(itemIcon(tool), null, modifier = Modifier.size(18.dp)) },
                                onClick = { addOpen = false; onUpdate(config.copy(tools = config.tools + tool)) },
                            )
                        }
                }
            }
        }
    }
}
