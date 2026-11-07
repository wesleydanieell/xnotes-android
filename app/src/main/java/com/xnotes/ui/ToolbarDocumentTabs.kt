package com.xnotes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.xnotes.R
import com.xnotes.core.workspace.NoteTab
import com.xnotes.core.workspace.NoteTabs
import com.xnotes.core.workspace.SplitSide
import com.xnotes.ui.icons.XnotesIcons

/** Navigation remains owned by the workspace; a bar only supplies a place to render it. */
internal data class ToolbarDocuments(
    val tabs: NoteTabs,
    val activeUri: String?,
    val expanded: Boolean,
    val onExpandedChange: (Boolean) -> Unit,
    val onSelect: (String) -> Unit,
    val onClose: (String) -> Unit,
    val onReorder: (Int, Int) -> Unit,
    val onSplit: (String, SplitSide) -> Unit,
    val dropArea: DocumentDropArea?,
)

internal val LocalToolbarDocuments = compositionLocalOf<ToolbarDocuments?> { null }
internal const val DOCUMENT_TABS_WIDTH_DP = 360f

/** Same animated switcher and tab gestures as standalone navigation, within a bounded bar slot.
 * Side rails open it beside the button so labels and split-drop coordinates stay upright. */
@Composable
internal fun ToolbarDocumentTabs() {
    val documents = LocalToolbarDocuments.current
    if (LocalBar.current.vertical) {
        var open by remember { mutableStateOf(false) }
        Box {
            ToolbarIcon(XnotesIcons.bookmark, stringResource(R.string.toolbar_document_tabs),
                enabled = documents?.tabs?.tabs?.isNotEmpty() == true) { open = !open }
            if (open && documents != null) {
                Popup(
                    alignment = if (LocalToolbarLook.current.position == com.xnotes.settings.ToolbarPosition.RIGHT)
                        Alignment.TopEnd else Alignment.TopStart,
                    onDismissRequest = { open = false },
                    properties = PopupProperties(focusable = false),
                ) {
                    DocumentsSwitcher(documents.copy(expanded = true, onExpandedChange = { if (!it) open = false }))
                }
            }
        }
    } else {
        // A fixed footprint keeps collapsing/expanding tabs from repacking unrelated bars.
        Box(Modifier.width(DOCUMENT_TABS_WIDTH_DP.dp).height(maxOf(42.dp, LocalBar.current.button)),
            contentAlignment = Alignment.CenterStart) {
            if (documents != null && documents.tabs.tabs.isNotEmpty()) DocumentsSwitcher(documents)
        }
    }
}

@Composable
private fun DocumentsSwitcher(documents: ToolbarDocuments) {
    FloatingDocumentTabs(
        tabs = documents.tabs,
        activeUri = documents.activeUri,
        expanded = documents.expanded,
        onExpandedChange = documents.onExpandedChange,
        onSelect = documents.onSelect,
        onClose = documents.onClose,
        onReorder = documents.onReorder,
        onSplit = documents.onSplit,
        dropArea = documents.dropArea,
        expandedWidth = DOCUMENT_TABS_WIDTH_DP.dp,
        embedded = !LocalBar.current.vertical,
    )
}

/** Noninteractive navigation fixture: preview clicks select the configurable element. */
@Composable
internal fun ToolbarDocumentTabsPreview(onSelect: () -> Unit) {
    if (LocalBar.current.vertical) {
        ToolbarIcon(XnotesIcons.bookmark, stringResource(R.string.toolbar_document_tabs), onClick = onSelect)
    } else {
        Box(Modifier.width(DOCUMENT_TABS_WIDTH_DP.dp).height(maxOf(42.dp, LocalBar.current.button))
            .clickable(onClick = onSelect)) {
            NoteTabBar(
                tabs = NoteTabs(listOf(NoteTab("preview.xnote", stringResource(R.string.toolbar_document_tabs)))),
                activeUri = "preview.xnote", onSelect = { onSelect() }, onClose = { onSelect() },
                onReorder = { _, _ -> }, onSplit = { _, _ -> }, floatingSurface = true,
            )
        }
    }
}

/** Suppress standalone navigation only while a configured tabs bar can actually be displayed. */
internal fun toolbarHasPlacedDocumentTabs(
    workspace: com.xnotes.core.tools.ToolbarWorkspace,
    look: com.xnotes.settings.ToolbarLook,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    colors: Int,
): Boolean {
    if (!workspace.hasVisibleDocumentTabs()) return false
    val docked = toolbarDockPlan(workspace, look, width, height)
    val floating = toolbarFloatingPlan(workspace, look,
        width - docked.leftInset.dp - docked.rightInset.dp,
        height - docked.topInset.dp - docked.bottomInset.dp, colors)
    return workspace.bars.withIndex().any { (index, bar) ->
        bar.visible && bar.layout.visibleSections.any { section ->
            section.visibleEntries.any { it.item == com.xnotes.core.tools.ToolbarItem.DOCUMENT_TABS }
        } && ((index == 0 && !bar.geometry.floating && !workspace.hasMultiDock(look)) ||
            docked.placement(bar.id) != null || floating.placement(bar.id) != null)
    }
}
