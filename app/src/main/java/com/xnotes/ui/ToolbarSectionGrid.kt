package com.xnotes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarEntry
import com.xnotes.core.tools.ToolbarLayout
import com.xnotes.core.tools.ToolbarSection

/** Maximum thickness measured perpendicular to a bar. */
internal fun toolbarCrossCells(layout: ToolbarLayout?): Int =
    layout?.visibleSections?.maxOfOrNull { it.crossCells }?.coerceIn(1, 3) ?: 1

/** Identical layout in notes, canvas and settings preview.
 * Sections hold 1–3 independent lanes. Sidebars transpose rows into columns.
 * Composite controls retain their natural dimensions. */
@Composable
internal fun ToolbarSectionGrid(
    section: ToolbarSection,
    cell: @Composable (ToolbarEntry) -> Unit,
) {
    if (section.lanes == 1) {
        section.visibleEntries.forEach { cell(it) }
        return
    }
    if (LocalBar.current.vertical) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(section.lanes) { lane ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    section.entriesInLane(lane).forEach { cell(it) }
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(section.lanes) { lane ->
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    section.entriesInLane(lane).forEach { cell(it) }
                }
            }
        }
    }
}
