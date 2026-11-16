package com.xnotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
    val placement: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ToolbarSettingsLabel(stringResource(R.string.toolbar_config_position_and_alignment))
                ToolbarPositionMap(config.position.dockEdge(), config.alignment()) { edge, alignment ->
                    onConfigChange(config.movedTo(edge.toolbarPosition(), alignment))
                }
            }
        }
    }
    val floatingOptions: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Slider(config.manualExtentDp.coerceIn(160, 800).toFloat(),
                            onValueChange = { onConfigChange(config.copy(manualExtentDp = it.toInt())) },
                            modifier = Modifier.weight(1f),
                            valueRange = 160f..800f)
                        Text(stringResource(R.string.document_tabs_extent_dp, config.manualExtentDp),
                            modifier = Modifier.widthIn(min = 52.dp),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val twoColumns = config.floating && maxWidth >= 480.dp
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (twoColumns) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column(Modifier.width(240.dp)) { placement() }
                    Column(Modifier.weight(1f)) { floatingOptions() }
                }
            } else {
                placement()
                if (config.floating) floatingOptions()
            }
            Text(stringResource(R.string.document_tabs_gesture_hint),
                style = MaterialTheme.typography.bodySmall,
                color = LocalPalette.current.textDim.toComposeColor())
        }
    }
}
