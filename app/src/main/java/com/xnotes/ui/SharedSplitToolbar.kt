package com.xnotes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xnotes.R
import com.xnotes.core.tools.*
import com.xnotes.settings.ToolbarLook
import com.xnotes.ui.icons.XnotesIcons
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

/** Only the host draws the bar. Each surface reserves the part that actually overlaps it. */
internal class SharedToolbarOcclusion {
    var bar by mutableStateOf(Rect.Zero)

    fun insets(pane: Rect, look: ToolbarLook): ToolbarPaneInsets =
        if (!look.floating) ToolbarPaneInsets() else sharedToolbarInsets(
            ToolbarPaneBounds(bar.left.toDouble(), bar.top.toDouble(), bar.right.toDouble(), bar.bottom.toDouble()),
            ToolbarPaneBounds(pane.left.toDouble(), pane.top.toDouble(), pane.right.toDouble(), pane.bottom.toDouble()),
            look.position.id,
        )
}

/** Preserve the chosen interface's order, but never send a page command to a hidden note. */
@Composable
internal fun UnavailableSharedToolbarItem(item: ToolbarItem) {
    val description = stringResource(R.string.split_tool_unavailable, stringResource(item.labelRes))
    Box(Modifier.clearAndSetSemantics { contentDescription = description; disabled() }) {
        ToolbarIcon(itemIcon(item), description, enabled = false) {}
    }
}

/** Split tools live in the existing navigation surface, with no mandatory extra header. */
@Composable
internal fun SplitToolbarTabsControl(app: Editor) {
    val palette = LocalPalette.current
    val preferences = remember(app.prefsVersion) { app.preferences }
    val mixed = app.inSplit && app.canvasOpen != app.secondary?.canvasOpen
    var open by remember { mutableStateOf(false) }
    Box {
        if (!LocalBar.current.vertical && mixed && preferences.sharedSplitToolbars) {
            TextButton(onClick = { open = true }, modifier = Modifier.height(40.dp)) {
                Icon(XnotesIcons.sliders, stringResource(R.string.split_settings),
                    tint = palette.textDim.toComposeColor(), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (preferences.mixedSplitToolbar == SplitToolbarInterface.CANVAS) "xcanvas" else "xnote")
            }
        } else {
            IconButton(onClick = { open = true }, modifier = Modifier.size(40.dp)) {
                Icon(XnotesIcons.sliders, stringResource(R.string.split_settings),
                    tint = palette.textDim.toComposeColor(), modifier = Modifier.size(20.dp))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Column(Modifier.width(300.dp).padding(12.dp)) {
                Text(stringResource(R.string.split_settings), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(12.dp))
                SplitToolbarSettings(preferences, mixed) { app.applyHomePreferences(it) }
            }
        }
    }
}
