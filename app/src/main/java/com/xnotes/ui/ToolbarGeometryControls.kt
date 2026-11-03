package com.xnotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarGeometry
import kotlin.math.roundToInt

/**
 * Size controls for the selected bar only. Null leaves the bar at its natural
 * size. Sliding any control creates an explicit override; Reset returns to auto.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ToolbarGeometryControls(
    geometry: ToolbarGeometry,
    onChange: (ToolbarGeometry) -> Unit,
) {
    val screen = LocalConfiguration.current
    Text("Dimensões e espaçamento", style = MaterialTheme.typography.titleSmall)
    Text(
        "Valores em dp. Automático mantém o tamanho padrão da barra; as mudanças também aparecem no preview.",
        style = MaterialTheme.typography.bodySmall,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToolbarMeasure("Largura", geometry.widthDp, screen.screenWidthDp.coerceIn(48, 1200), 48, 1200) {
            onChange(geometry.copy(widthDp = it))
        }
        ToolbarMeasure("Altura", geometry.heightDp, 56, 48, 800) {
            onChange(geometry.copy(heightDp = it))
        }
        ToolbarMeasure("Padding", geometry.paddingDp, 4, 0, 32) {
            onChange(geometry.copy(paddingDp = it))
        }
        ToolbarMeasure("Espaçamento", geometry.spacingDp, 2, 0, 32) {
            onChange(geometry.copy(spacingDp = it))
        }
        ToolbarMeasure("Elementos", geometry.elementSizeDp, 42, 28, 72) {
            onChange(geometry.copy(elementSizeDp = it))
        }
    }
}

@Composable
private fun ToolbarMeasure(
    title: String,
    value: Int?,
    defaultValue: Int,
    minimum: Int,
    maximum: Int,
    onChange: (Int?) -> Unit,
) {
    Column(Modifier.width(240.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$title: ${value?.let { "$it dp" } ?: "automático"}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            // Keep the button's space even in auto mode; otherwise the row shifts
            // when the first slider touch creates an override.
            TextButton(enabled = value != null, onClick = { onChange(null) }) {
                Text("Auto")
            }
        }
        Slider(
            value = (value ?: defaultValue).coerceIn(minimum, maximum).toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = minimum.toFloat()..maximum.toFloat(),
            modifier = Modifier.width(230.dp),
        )
    }
}
