package com.xnotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xnotes.R
import com.xnotes.settings.DocumentTabsBarConfig
import com.xnotes.settings.DocumentTabsSizing
import com.xnotes.ui.theme.LocalPalette
import com.xnotes.ui.theme.toComposeColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DocumentTabsEditor(
    config: DocumentTabsBarConfig,
    startupMode: String,
    onConfigChange: (DocumentTabsBarConfig) -> Unit,
    onModeChange: (String) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val twoColumns = maxWidth >= 460.dp
        val groupWidth = if (twoColumns) (maxWidth - 13.dp) / 2 else maxWidth
        val mapWidth = if (twoColumns) 240.dp else maxWidth
        val sizeWidth = if (twoColumns) maxWidth - mapWidth - 13.dp else maxWidth
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.width(groupWidth), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolbarSettingsLabel(stringResource(R.string.toolbar_config_placement))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PreferenceChoiceChip(stringResource(R.string.toolbar_docked), !config.floating) {
                            onConfigChange(config.withFloating(false))
                        }
                        PreferenceChoiceChip(stringResource(R.string.toolbar_floating), config.floating) {
                            onConfigChange(config.withFloating(true))
                        }
                    }
                }
                Column(Modifier.width(groupWidth), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolbarSettingsLabel(stringResource(R.string.document_tabs_startup))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PreferenceChoiceChip(stringResource(R.string.document_tabs_auto), startupMode == "auto") {
                            onModeChange("auto")
                        }
                        PreferenceChoiceChip(stringResource(R.string.document_tabs_compact), startupMode == "compact") {
                            onModeChange("compact")
                        }
                        PreferenceChoiceChip(stringResource(R.string.document_tabs_expanded), startupMode == "expanded") {
                            onModeChange("expanded")
                        }
                    }
                }
            }
            HorizontalDivider()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.width(mapWidth), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolbarSettingsLabel(stringResource(R.string.toolbar_config_position_and_alignment))
                    ToolbarPositionMap(config.position.dockEdge(), config.alignment()) { edge, alignment ->
                        onConfigChange(config.movedTo(edge.toolbarPosition(), alignment))
                    }
                }
                Column(Modifier.width(sizeWidth), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToolbarSettingsLabel(stringResource(R.string.document_tabs_expanded_size))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PreferenceChoiceChip(stringResource(R.string.document_tabs_automatic_size),
                            config.sizing == DocumentTabsSizing.DYNAMIC) {
                            onConfigChange(config.copy(sizing = DocumentTabsSizing.DYNAMIC))
                        }
                        PreferenceChoiceChip(stringResource(R.string.document_tabs_manual_size),
                            config.sizing == DocumentTabsSizing.MANUAL) {
                            onConfigChange(config.copy(sizing = DocumentTabsSizing.MANUAL))
                        }
                    }
                    if (config.sizing == DocumentTabsSizing.MANUAL) {
                        Text(stringResource(R.string.document_tabs_extent_dp, config.manualExtentDp),
                            style = MaterialTheme.typography.bodySmall)
                        Slider(config.manualExtentDp.coerceIn(160, 800).toFloat(),
                            onValueChange = { onConfigChange(config.copy(manualExtentDp = it.toInt())) },
                            valueRange = 160f..800f)
                    }
                }
            }
            Text(stringResource(R.string.document_tabs_gesture_hint),
                style = MaterialTheme.typography.bodySmall,
                color = LocalPalette.current.textDim.toComposeColor())
        }
    }
}
