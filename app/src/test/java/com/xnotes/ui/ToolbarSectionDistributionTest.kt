package com.xnotes.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.xnotes.core.tools.ToolbarDistribution
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.core.tools.ToolbarEntry
import com.xnotes.core.tools.ToolbarLayout
import com.xnotes.core.tools.ToolbarRegion
import com.xnotes.core.tools.ToolbarSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarSectionDistributionTest {
    @Test fun centeredRegionKeepsPhysicalCenterWhenThereIsRoom() {
        val plan = toolbarRegionPositions(600, listOf(120, 100, 80), 8)
        assertEquals(600, plan.extent)
        assertEquals(listOf(0, 250, 520), plan.offsets)
    }

    @Test fun constrainedViewportExpandsTheScrollableStripInsteadOfOverlapping() {
        val sizes = listOf(120, 100, 80)
        val plan = toolbarRegionPositions(230, sizes, 8)
        assertEquals(316, plan.extent)
        assertEquals(listOf(0, 128, 236), plan.offsets)
        assertTrue(plan.offsets[1] >= plan.offsets[0] + sizes[0] + 8)
        assertTrue(plan.offsets[2] >= plan.offsets[1] + sizes[1] + 8)
    }

    @Test fun missingCenterDoesNotMoveEdgeGroupsFromTheirAnchors() {
        assertEquals(listOf(0, 0, 220), toolbarRegionPositions(300, listOf(80, 0, 80), 8).offsets)
        assertEquals(listOf(0, 125, 250), toolbarRegionPositions(300, listOf(0, 50, 50), 8).offsets)
    }

    @Test fun sectionCanBeDroppedInAnEmptyCenterColumn() {
        val layout = ToolbarLayout(listOf(ToolbarSection(listOf(ToolbarEntry(ToolbarItem.HOME)))),
            distribution = ToolbarDistribution.REGIONS)
        val bounds = mapOf(ToolbarRegion.START to Rect(0f, 0f, 90f, 400f),
            ToolbarRegion.CENTER to Rect(100f, 0f, 190f, 400f),
            ToolbarRegion.END to Rect(200f, 0f, 290f, 400f))
        val target = toolbarSectionDropTarget(layout, Offset(130f, 80f),
            mapOf(0 to Rect(0f, 40f, 90f, 100f)), bounds)
        assertEquals(ToolbarSectionTarget(ToolbarRegion.CENTER), target)
    }

    @Test fun verticalLayerDropChecksRowMidpointNotHorizontalPosition() {
        val layout = ToolbarLayout(listOf(ToolbarSection(listOf(
            ToolbarEntry(ToolbarItem.HOME), ToolbarEntry(ToolbarItem.TITLE)))))
        val section = mapOf(0 to Rect(0f, 0f, 200f, 200f))
        val lanes = mapOf((0 to 0) to Rect(0f, 0f, 200f, 200f))
        val chips = mapOf((0 to 0) to Rect(0f, 20f, 200f, 60f),
            (0 to 1) to Rect(0f, 70f, 200f, 110f))
        assertEquals(Triple(0, 0, 1), toolbarDropTarget(layout, Offset(190f, 72f), chips, lanes, section))
    }
}
