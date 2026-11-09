package com.xnotes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.xnotes.core.workspace.NoteTabs
import com.xnotes.core.workspace.SplitSide
import com.xnotes.settings.DocumentTabsBarConfig

/** One app-owned document session, shared by the navigation bar and its preview. */
internal data class DocumentTabsState(
    val tabs: NoteTabs,
    val selection: DocumentTabsSelectionState,
    val activeUri: String?,
    val expanded: Boolean,
    val onExpandedChange: (Boolean) -> Unit,
    val onSelect: (String) -> Unit,
    val onClose: (String) -> Unit,
    val onReorder: (Int, Int) -> Unit,
    val onSplit: (String, SplitSide) -> Unit,
    val dropArea: DocumentDropArea?,
    val splitControls: (@Composable () -> Unit)? = null,
    /** Per-pane tools use one fixed navigation row above the complete split. */
    val dockAboveSplit: Boolean = false,
)

internal val LocalDocumentTabsBoundsObserver = compositionLocalOf<((androidx.compose.ui.geometry.Rect) -> Unit)?> { null }

internal val LocalDocumentTabsState = compositionLocalOf<DocumentTabsState?> { null }
internal val LocalDocumentTabsConfig = compositionLocalOf { DocumentTabsBarConfig() }
internal const val DOCUMENT_TABS_WIDTH_DP = 360f
internal const val DOCUMENT_TABS_SIDE_WIDTH_DP = 160f
