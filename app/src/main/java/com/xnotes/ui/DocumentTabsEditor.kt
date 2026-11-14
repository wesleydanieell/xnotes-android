package com.xnotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.settings.DocumentTabsBarConfig
import com.xnotes.settings.DocumentTabsSizing
import com.xnotes.settings.ToolbarPosition

/** Independent document-navigation controls. No fake `document_tabs` tool item. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DocumentTabsEditor(
    config: DocumentTabsBarConfig,
    startupMode: String,
    onConfigChange: (DocumentTabsBarConfig) -> Unit,
    onModeChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Document navigation", style = MaterialTheme.typography.titleSmall)
        Text("Persistent tabs are a separate bar, compatible with XNote and XCanvas toolbars.",
            style = MaterialTheme.typography.bodySmall)
        Text("Placement", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolbarEditorChip("Floating", config.floating) { onConfigChange(config.withFloating(true)) }
            ToolbarEditorChip("Docked", !config.floating) { onConfigChange(config.withFloating(false)) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((edge, title) in listOf(
                ToolbarPosition.TOP to "Top", ToolbarPosition.BOTTOM to "Bottom",
                ToolbarPosition.LEFT to "Left", ToolbarPosition.RIGHT to "Right",
            )) {
                ToolbarEditorChip(title, config.position == edge) {
                    onConfigChange(config.movedTo(edge, com.xnotes.core.tools.ToolbarDockAlignment.CENTER))
                }
            }
        }
        if (config.floating) {
            Text("Alignment", style = MaterialTheme.typography.labelMedium)
            val anchors = when (config.position) {
                ToolbarPosition.TOP -> listOf(ToolbarAnchor.TOP_LEFT to "Start", ToolbarAnchor.TOP_CENTER to "Center", ToolbarAnchor.TOP_RIGHT to "End")
                ToolbarPosition.BOTTOM -> listOf(ToolbarAnchor.BOTTOM_LEFT to "Start", ToolbarAnchor.BOTTOM_CENTER to "Center", ToolbarAnchor.BOTTOM_RIGHT to "End")
                ToolbarPosition.LEFT -> listOf(ToolbarAnchor.TOP_LEFT to "Start", ToolbarAnchor.CENTER_LEFT to "Center", ToolbarAnchor.BOTTOM_LEFT to "End")
                ToolbarPosition.RIGHT -> listOf(ToolbarAnchor.TOP_RIGHT to "Start", ToolbarAnchor.CENTER_RIGHT to "Center", ToolbarAnchor.BOTTOM_RIGHT to "End")
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                anchors.forEach { (anchor, title) ->
                    ToolbarEditorChip(title, config.anchor == anchor) {
                        onConfigChange(config.copy(anchor = anchor))
                    }
                }
            }
        }
        HorizontalDivider()
        Text("Expanded size", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolbarEditorChip("Available space", config.sizing == DocumentTabsSizing.DYNAMIC) {
                onConfigChange(config.copy(sizing = DocumentTabsSizing.DYNAMIC))
            }
            ToolbarEditorChip("Custom", config.sizing == DocumentTabsSizing.MANUAL) {
                onConfigChange(config.copy(sizing = DocumentTabsSizing.MANUAL))
            }
        }
        if (config.sizing == DocumentTabsSizing.MANUAL) {
            Text("${config.manualExtentDp} dp", style = MaterialTheme.typography.bodySmall)
            Slider(config.manualExtentDp.coerceIn(160, 800).toFloat(),
                onValueChange = { onConfigChange(config.copy(manualExtentDp = it.toInt())) },
                valueRange = 160f..800f)
        }
        Text("Startup", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolbarEditorChip("Auto", startupMode == "auto") { onModeChange("auto") }
            ToolbarEditorChip("Collapsed", startupMode == "compact") { onModeChange("compact") }
            ToolbarEditorChip("Expanded", startupMode == "expanded") { onModeChange("expanded") }
        }
        Text("Hold the navigation bar to move it. Hold a tab to reorder it; the tab menu also offers split view.",
            style = MaterialTheme.typography.bodySmall)
    }
}
