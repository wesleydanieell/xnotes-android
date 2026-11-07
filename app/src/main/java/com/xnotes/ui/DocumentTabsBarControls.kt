package com.xnotes.ui

import androidx.compose.foundation.layout.*
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(initiallyExpanded, onCheckedChange = onInitiallyExpanded)
            Text("Iniciar expandida", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
