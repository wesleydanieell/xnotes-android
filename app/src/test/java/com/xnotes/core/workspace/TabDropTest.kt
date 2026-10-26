package com.xnotes.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TabDropTest {
    private fun drop(x: Float, y: Float, from: Int = 1, scroll: Float = 0f) = resolveTabDrop(
        fromIndex = from, x = x, y = y, barLeft = 20f, barTop = 10f,
        barWidth = 800f, barHeight = 42f, slotWidth = 200f,
        scrollOffset = scroll, tabCount = 4, splitThreshold = 48f,
    )

    @Test fun horizontalDragReordersWithoutOpeningAnotherPane() {
        assertEquals(TabDrop.Reorder(1, 2), drop(520f, 30f))
        assertEquals(TabDrop.Reorder(1, 3), drop(620f, 30f, scroll = 200f))
        assertNull(drop(300f, 30f))
    }

    @Test fun draggingBelowStripTargetsTheCorrectHalf() {
        assertEquals(TabDrop.Split(false), drop(100f, 130f))
        assertEquals(TabDrop.Split(true), drop(700f, 130f))
        assertNull(drop(100f, -70f))
    }

    @Test fun insertionGuideAccountsForRemovedTab() {
        assertEquals(3, insertionBoundary(0, 2))
        assertEquals(1, insertionBoundary(3, 1))
    }

    @Test fun singleTabCannotClaimASplitDropTarget() {
        assertNull(resolveTabDrop(0, 700f, 130f, 20f, 10f, 800f, 42f, 800f, 0f, 1, 48f))
    }

    @Test fun invalidSourceCannotMoveOrSplit() {
        assertNull(drop(700f, 130f, from = 4))
    }
}
