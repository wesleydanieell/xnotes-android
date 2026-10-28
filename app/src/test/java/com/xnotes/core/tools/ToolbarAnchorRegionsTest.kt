package com.xnotes.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarAnchorRegionsTest {
    @Test fun singleAnchorCanUseEntirePane() {
        assertEquals(ToolbarAnchorRegion(0f, 0f, 900f, 600f),
            toolbarAnchorRegions(setOf(ToolbarAnchor.CENTER_LEFT), 900f, 600f)[ToolbarAnchor.CENTER_LEFT])
    }

    @Test fun distinctRowsAndColumnsHaveDisjointRegions() {
        val anchors = ToolbarAnchor.entries.toSet()
        val regions = toolbarAnchorRegions(anchors, 900f, 600f)
        assertEquals(ToolbarAnchorRegion(300f, 200f, 300f, 200f), regions[ToolbarAnchor.CENTER])
        for ((a, r) in regions) for ((b, s) in regions) if (a != b) {
            assertTrue(r.x + r.width <= s.x || s.x + s.width <= r.x ||
                r.y + r.height <= s.y || s.y + s.height <= r.y)
        }
    }

    @Test fun onlyOccupiedAxisIsDividedAndInvalidPaneIsRejected() {
        val regions = toolbarAnchorRegions(setOf(ToolbarAnchor.TOP_LEFT, ToolbarAnchor.BOTTOM_LEFT), 900f, 600f)
        assertEquals(ToolbarAnchorRegion(0f, 400f, 900f, 200f), regions[ToolbarAnchor.BOTTOM_LEFT])
        assertTrue(toolbarAnchorRegions(setOf(ToolbarAnchor.CENTER), Float.NaN, 1f).isEmpty())
    }
}
