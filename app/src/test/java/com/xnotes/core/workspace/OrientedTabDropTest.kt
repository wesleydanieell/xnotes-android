package com.xnotes.core.workspace

import org.junit.Assert.*
import org.junit.Test

class OrientedTabDropTest {
    private val document = TabStripBounds(0f, 0f, 1000f, 800f)

    @Test fun stripsAtAllFourEdgesSplitTowardsTheDocument() {
        val cases = listOf(
            TabStripGeometry(TabStripBounds(0f, 0f, 1000f, 42f), 200f, 0f) to (500f to 150f),
            TabStripGeometry(TabStripBounds(0f, 758f, 1000f, 42f), 200f, 0f) to (500f to 650f),
            TabStripGeometry(TabStripBounds(0f, 0f, 160f, 800f), 100f, 0f, vertical = true) to (300f to 400f),
            TabStripGeometry(TabStripBounds(840f, 0f, 160f, 800f), 100f, 0f, vertical = true) to (700f to 400f),
        )
        for ((strip, point) in cases) {
            assertTrue(resolveTabStripDrop(0, point.first, point.second, strip, 3, 48f, document) is TabDrop.Split)
        }
    }

    @Test fun verticalReorderingUsesHeightScrollAndKeepsScreenSpaceSplitZones() {
        val strip = TabStripGeometry(TabStripBounds(840f, 50f, 160f, 500f), 100f, 100f, true)
        assertEquals(TabDrop.Reorder(0, 2), resolveTabStripDrop(0, 900f, 200f, strip, 5, 48f, document))
        assertEquals(TabDrop.Split(SplitSide.LEFT), resolveTabStripDrop(0, 50f, 400f, strip, 5, 48f, document))
        assertEquals(TabDrop.Split(SplitSide.BOTTOM), resolveTabStripDrop(0, 500f, 780f, strip, 5, 48f, document))
    }

    @Test fun invalidOrOutsideWorkspaceDropsDoNotOpenSplits() {
        val strip = TabStripGeometry(TabStripBounds(0f, 758f, 1000f, 42f), 200f, 0f)
        assertNull(resolveTabStripDrop(0, 500f, -200f, strip, 3, 48f, document))
        assertNull(resolveTabStripDrop(-1, 500f, 400f, strip, 3, 48f, document))
    }
}
