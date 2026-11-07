package com.xnotes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import com.xnotes.ui.icons.XnotesIcons

internal val LocalUnavailableDockBars = compositionLocalOf { emptyList<ToolbarBar>() }

/** A constrained pane never silently loses tools; rejected bars remain usable in this menu. */
@Composable
internal fun ToolbarOverflowMenu(bars: List<ToolbarBar>, look: ToolbarLook, maxWidth: Dp,
    modifier: Modifier = Modifier, content: @Composable (ToolbarBar) -> Unit) {
    var open by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalGlobalToolHitAreas provides null) {
        ToolbarHitArea(modifier) {
            Box {
                FilledIconButton(onClick = { open = !open }, modifier = Modifier.size(40.dp)) {
                    Icon(XnotesIcons.more, "Outras barras (${bars.size})")
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false },
                    modifier = Modifier.widthIn(max = maxWidth.coerceAtLeast(40.dp))) {
                    Text("Barras sem espaço neste painel", modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.labelMedium)
                    // Popup bounds belong to another window; never register them as canvas exclusions.
                    CompositionLocalProvider(LocalToolbarHitAreas provides null,
                        LocalToolbarLook provides look.copy(position = ToolbarPosition.TOP, floating = false)) {
                        for (bar in bars) key(bar.id) {
                            Text(bar.name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall)
                            content(bar.copy(geometry = bar.geometry.copy(widthDp = null, heightDp = null)))
                        }
                    }
                }
            }
        }
    }
}
