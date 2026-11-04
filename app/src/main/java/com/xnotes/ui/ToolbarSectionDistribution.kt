package com.xnotes.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
    if (visible.isEmpty()) return
    val vertical = LocalBar.current.vertical
    val groups = toolbarRegionGroups(layout)
    val present = groups.filterValues { it.isNotEmpty() }.keys

    // Each region owns its viewport: an oversized Start section can scroll locally without
    // pushing Center and End off-screen. The center always means the physical center.
    val regionContent: @Composable (ToolbarRegion) -> Unit = { region ->
        androidx.compose.runtime.key(region) {
            val group = groups.getValue(region)
            if (vertical) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    group.forEachIndexed { index, indexed ->
                        if (index > 0) Separator()
                        sectionContent(indexed.index, indexed.value)
                    }
                }
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    group.forEachIndexed { index, indexed ->
                        if (index > 0) Separator()
                        sectionContent(indexed.index, indexed.value)
                    }
                }
            }
        }
    }
    if (present.size == 1) {
        val region = present.single()
        val alignment = if (vertical) when (region) {
            ToolbarRegion.START -> androidx.compose.ui.Alignment.TopCenter
            ToolbarRegion.CENTER -> androidx.compose.ui.Alignment.Center
            ToolbarRegion.END -> androidx.compose.ui.Alignment.BottomCenter
        } else when (region) {
            ToolbarRegion.START -> androidx.compose.ui.Alignment.CenterStart
            ToolbarRegion.CENTER -> androidx.compose.ui.Alignment.Center
            ToolbarRegion.END -> androidx.compose.ui.Alignment.CenterEnd
        }
        Box(if (vertical) Modifier.fillMaxHeight() else Modifier.fillMaxWidth(), contentAlignment = alignment) {
            regionContent(region)
        }
    } else if (vertical) {
        Column(Modifier.fillMaxHeight()) {
            ToolbarRegion.entries.forEach { region ->
                val alignment = when (region) {
                    ToolbarRegion.START -> androidx.compose.ui.Alignment.TopCenter
                    ToolbarRegion.CENTER -> androidx.compose.ui.Alignment.Center
                    ToolbarRegion.END -> androidx.compose.ui.Alignment.BottomCenter
                }
                Box(Modifier.weight(1f).clipToBounds(), contentAlignment = alignment) {
                    regionContent(region)
                }
            }
        }
    } else {
        Row(Modifier.fillMaxWidth()) {
            ToolbarRegion.entries.forEach { region ->
                val alignment = when (region) {
                    ToolbarRegion.START -> androidx.compose.ui.Alignment.CenterStart
                    ToolbarRegion.CENTER -> androidx.compose.ui.Alignment.Center
                    ToolbarRegion.END -> androidx.compose.ui.Alignment.CenterEnd
                }
                Box(Modifier.weight(1f).clipToBounds(), contentAlignment = alignment) {
                    regionContent(region)
                }
            }
        }
    }
}

/** The preview and live toolbars consume the same stable, original section indices.
 * Empty and hidden sections remain editable but never claim visible toolbar space. */
internal fun toolbarRegionGroups(layout: ToolbarLayout): Map<ToolbarRegion, List<IndexedValue<ToolbarSection>>> {
    val visible = layout.sections.withIndex().filter { it.value.hasVisible }
    return ToolbarRegion.entries.associateWith { region ->
        visible.filter { it.value.region == region }
    }
}
