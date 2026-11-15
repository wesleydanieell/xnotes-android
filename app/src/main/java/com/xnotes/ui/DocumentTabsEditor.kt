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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xnotes.R
import com.xnotes.settings.DocumentTabsBarConfig
import com.xnotes.settings.DocumentTabsSizing

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
        Text(stringResource(R.string.document_tabs_title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(R.string.document_tabs_description),
            style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.toolbar_config_placement), style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolbarEditorChip(stringResource(R.string.toolbar_floating), config.floating) { onConfigChange(config.withFloating(true)) }
            ToolbarEditorChip(stringResource(R.string.toolbar_docked), !config.floating) { onConfigChange(config.withFloating(false)) }
        }
        Text(stringResource(R.string.toolbar_config_position_and_alignment),
            style = MaterialTheme.typography.labelMedium)
        ToolbarPositionMap(config.position.dockEdge(), config.alignment()) { edge, alignment ->
            onConfigChange(config.movedTo(edge.toolbarPosition(), alignment))
        }
        HorizontalDivider()
        Text(stringResource(R.string.document_tabs_expanded_size), style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolbarEditorChip(stringResource(R.string.document_tabs_automatic_size), config.sizing == DocumentTabsSizing.DYNAMIC) {
                onConfigChange(config.copy(sizing = DocumentTabsSizing.DYNAMIC))
            }
            ToolbarEditorChip(stringResource(R.string.document_tabs_manual_size), config.sizing == DocumentTabsSizing.MANUAL) {
                onConfigChange(config.copy(sizing = DocumentTabsSizing.MANUAL))
            }
        }
        if (config.sizing == DocumentTabsSizing.MANUAL) {
            Text(stringResource(R.string.document_tabs_extent_dp, config.manualExtentDp), style = MaterialTheme.typography.bodySmall)
            Slider(config.manualExtentDp.coerceIn(160, 800).toFloat(),
                onValueChange = { onConfigChange(config.copy(manualExtentDp = it.toInt())) },
                valueRange = 160f..800f)
        }
        Text(stringResource(R.string.document_tabs_startup), style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToolbarEditorChip(stringResource(R.string.document_tabs_auto), startupMode == "auto") { onModeChange("auto") }
            ToolbarEditorChip(stringResource(R.string.document_tabs_compact), startupMode == "compact") { onModeChange("compact") }
            ToolbarEditorChip(stringResource(R.string.document_tabs_expanded), startupMode == "expanded") { onModeChange("expanded") }
        }
        Text(stringResource(R.string.document_tabs_gesture_hint),
            style = MaterialTheme.typography.bodySmall)
    }
}
