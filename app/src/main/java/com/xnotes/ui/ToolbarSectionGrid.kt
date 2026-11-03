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
    layout?.visibleSections?.maxOfOrNull { it.crossCells }?.coerceIn(1, 2) ?: 1

/** Identical layout in live notes, canvas and toolbar settings preview.
 * Sections hold one or two independent lanes. On sidebars, lanes become columns.
 * Each lane has no arbitrary limit, and composite items keep their natural dimensions. */
@Composable
internal fun ToolbarSectionGrid(
    section: ToolbarSection,
    cell: @Composable (ToolbarEntry) -> Unit,
) {
    if (section.lanes != 2) {
        section.visibleEntries.forEach { cell(it) }
        return
    }
    val first = section.entriesInLane(0)
    val second = section.entriesInLane(1)
    if (LocalBar.current.vertical) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { first.forEach { cell(it) } }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { second.forEach { cell(it) } }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { first.forEach { cell(it) } }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { second.forEach { cell(it) } }
        }
    }
}
