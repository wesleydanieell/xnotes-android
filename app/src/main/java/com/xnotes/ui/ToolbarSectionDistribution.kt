package com.xnotes.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarDistribution
import com.xnotes.core.tools.ToolbarLayout
import com.xnotes.core.tools.ToolbarRegion
import com.xnotes.core.tools.ToolbarSection

/**
 * One section is the indivisible group: its one to three lanes stay together.
 * Regions anchor those groups to the start, physical center and end of a bar.
 * When the viewport is too small, the strip scrolls rather than overlapping tools.
 * Side toolbars use the identical algorithm on the vertical axis.
 */
@Composable
internal fun ToolbarSections(
    layout: ToolbarLayout,
    sectionContent: @Composable (Int, ToolbarSection) -> Unit,
) {
    val visible = layout.sections.withIndex().filter { it.value.hasVisible }
    if (layout.distribution == ToolbarDistribution.CONTINUOUS) {
        visible.forEachIndexed { visual, indexed ->
            if (visual > 0) Separator()
            sectionContent(indexed.index, indexed.value)
        }
        return
    }
    val vertical = LocalBar.current.vertical
    val regions = ToolbarRegion.entries.map { region -> visible.filter { it.value.region == region } }
    val content: List<@Composable () -> Unit> = regions.map { group ->
        {
            if (vertical) {
                Column {
                    group.forEachIndexed { index, indexed ->
                        if (index > 0) Separator()
                        sectionContent(indexed.index, indexed.value)
                    }
                }
            } else {
                Row {
                    group.forEachIndexed { index, indexed ->
                        if (index > 0) Separator()
                        sectionContent(indexed.index, indexed.value)
                    }
                }
            }
        }
    }
    BoxWithConstraints(if (vertical) Modifier.fillMaxHeight() else Modifier.fillMaxWidth()) {
        val available = with(LocalDensity.current) {
            if (vertical) maxHeight.roundToPx() else maxWidth.roundToPx()
        }
        val gap = with(LocalDensity.current) { 8.dp.roundToPx() }
        Layout(
            contents = content,
            modifier = if (vertical) Modifier.verticalScroll(rememberScrollState())
                else Modifier.horizontalScroll(rememberScrollState()),
        ) { measurables, constraints ->
            val loose = Constraints(
                maxWidth = if (vertical) constraints.maxWidth else Constraints.Infinity,
                maxHeight = if (vertical) Constraints.Infinity else constraints.maxHeight,
            )
            val children = measurables.map { it.single().measure(loose) }
            val lengths = children.map { if (vertical) it.height else it.width }
            val positions = toolbarRegionPositions(available, lengths, gap)
            val crossSize = children.maxOfOrNull { if (vertical) it.width else it.height } ?: 0
            if (vertical) {
                layout(crossSize.coerceIn(constraints.minWidth, constraints.maxWidth), positions.extent) {
                    children.forEachIndexed { index, child ->
                        child.place((crossSize - child.width) / 2, positions.offsets[index])
                    }
                }
            } else {
                layout(positions.extent, crossSize.coerceIn(constraints.minHeight, constraints.maxHeight)) {
                    children.forEachIndexed { index, child ->
                        child.place(positions.offsets[index], (crossSize - child.height) / 2)
                    }
                }
            }
        }
    }
}

internal data class ToolbarRegionPositions(val extent: Int, val offsets: List<Int>)

/**
 * Stable physical positions for Start/Center/End groups. Center stays mathematically centered
 * when there is room; otherwise it moves just far enough to clear the two edge groups.
 */
internal fun toolbarRegionPositions(available: Int, lengths: List<Int>, gap: Int): ToolbarRegionPositions {
    require(lengths.size == 3)
    val present = lengths.count { it > 0 }
    val spacing = gap.coerceAtLeast(0)
    val needed = lengths.sum() + spacing * (present - 1).coerceAtLeast(0)
    val extent = maxOf(available, needed, 0)
    val end = extent - lengths[2]
    val centerMin = if (lengths[0] > 0) lengths[0] + spacing else 0
    val centerMax = if (lengths[2] > 0) end - lengths[1] - spacing else extent - lengths[1]
    val center = if (lengths[1] > 0) ((extent - lengths[1]) / 2).coerceIn(centerMin, centerMax) else 0
    return ToolbarRegionPositions(extent, listOf(0, center, end))
}
