package com.xnotes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Slider
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xnotes.R
import com.xnotes.settings.DocumentTabsBarConfig
import com.xnotes.settings.DocumentTabsSizing

/** App navigation has positioning controls only; it is never an editable tool element. */
@Composable
internal fun DocumentTabsBarControls(config: DocumentTabsBarConfig, initiallyExpanded: Boolean,
    onConfig: (DocumentTabsBarConfig) -> Unit, onInitiallyExpanded: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Barra de abas", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PreferenceChoiceChip(stringResource(R.string.toolbar_floating), config.floating) { onConfig(config.withFloating(true)) }
            PreferenceChoiceChip(stringResource(R.string.toolbar_docked), !config.floating) { onConfig(config.withFloating(false)) }
        }
        ToolbarAnchorGrid(selected = if (config.floating) config.anchor else DocumentTabsBarConfig.anchorOf(config.position),
            docked = !config.floating, onSelect = { onConfig(config.movedTo(it)) })
        if (config.floating) {
            Text("Tamanho expandido", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreferenceChoiceChip("Dinâmico", config.sizing == DocumentTabsSizing.DYNAMIC) {
                    onConfig(config.copy(sizing = DocumentTabsSizing.DYNAMIC))
                }
                PreferenceChoiceChip("Manual", config.sizing == DocumentTabsSizing.MANUAL) {
                    onConfig(config.copy(sizing = DocumentTabsSizing.MANUAL))
                }
            }
            Text("Expande até o espaço livre antes da próxima barra.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreferenceChoiceChip("Mínimo", config.sizing == DocumentTabsSizing.MANUAL && config.manualExtentDp == config.minExtentDp) {
                    onConfig(config.copy(sizing = DocumentTabsSizing.MANUAL, manualExtentDp = config.minExtentDp))
                }
                PreferenceChoiceChip("360 dp", config.sizing == DocumentTabsSizing.MANUAL && config.manualExtentDp == 360) {
                    onConfig(config.copy(sizing = DocumentTabsSizing.MANUAL, manualExtentDp = 360))
                }
                PreferenceChoiceChip("Máximo", config.sizing == DocumentTabsSizing.DYNAMIC && config.maxExtentDp == 0) {
                    onConfig(config.copy(sizing = DocumentTabsSizing.DYNAMIC, maxExtentDp = 0))
                }
            }
            if (config.sizing == DocumentTabsSizing.MANUAL) {
                Text("Tamanho: ${config.manualExtentDp} dp", style = MaterialTheme.typography.bodySmall)
                Slider(value = config.manualExtentDp.toFloat().coerceIn(80f, 2000f),
                    onValueChange = { onConfig(config.copy(manualExtentDp = it.toInt())) }, valueRange = 80f..2000f)
            }
            Text("Limite mínimo: ${config.minExtentDp} dp", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (minimum in listOf(160, 240, 360)) PreferenceChoiceChip("$minimum dp", config.minExtentDp == minimum) {
                    onConfig(config.copy(minExtentDp = minimum))
                }
            }
            Text("Limite máximo", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (maximum in listOf(480, 720, 0)) PreferenceChoiceChip(if (maximum == 0) "Espaço livre" else "$maximum dp", config.maxExtentDp == maximum) {
                    onConfig(config.copy(maxExtentDp = maximum))
                }
            }
            Text("Os limites cedem quando a janela ou outra barra deixam menos espaço.", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(initiallyExpanded, onCheckedChange = onInitiallyExpanded)
            Text("Iniciar expandida", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
