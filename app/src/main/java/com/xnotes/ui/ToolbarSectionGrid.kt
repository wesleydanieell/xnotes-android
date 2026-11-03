package com.xnotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarEntry
import com.xnotes.core.tools.ToolbarLayout
import com.xnotes.core.tools.ToolbarSection

/** How many cells the thickest grouped section needs perpendicular to the bar's direction. */
internal fun toolbarCrossCells(layout: ToolbarLayout?): Int =
    layout?.visibleSections?.maxOfOrNull { it.crossCells }?.coerceIn(1, 2) ?: 1

/** Share the exact same grouped arrangement between note, canvas and preview.
 * Horizontal toolbars use columns of tools across 1–2 rows; side rails transpose the
 * same group into 1–2 columns across several rows. Linear sections remain unchanged. */
@Composable
internal fun ToolbarSectionGrid(
    section: ToolbarSection,
    cell: @Composable (ToolbarEntry) -> Unit,
) {
    val entries = section.visibleEntries
    if (section.gridColumns !in 1..3) {
        entries.forEach { cell(it) }
        return
    }

    val metrics = LocalBar.current
    val bands = entries.chunked(section.gridColumns)
    if (metrics.vertical) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            bands.forEach { band ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    band.forEach { entry ->
                        Box(Modifier.size(metrics.button), contentAlignment = Alignment.Center) {
                            cell(entry)
                        }
                    }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            bands.forEach { band ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    band.forEach { entry ->
                        Box(Modifier.size(metrics.button), contentAlignment = Alignment.Center) {
                            cell(entry)
                        }
                    }
                }
            }
        }
    }
}
