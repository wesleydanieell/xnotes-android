package com.xnotes.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanePlacementTest {
    @Test fun allFourDropTargetsAndOutOfBounds() {
        assertEquals(SplitSide.LEFT, paneDropSide(5f, 50f, 100f, 100f))
        assertEquals(SplitSide.RIGHT, paneDropSide(95f, 50f, 100f, 100f))
        assertEquals(SplitSide.TOP, paneDropSide(50f, 5f, 100f, 100f))
        assertEquals(SplitSide.BOTTOM, paneDropSide(50f, 95f, 100f, 100f))
        assertEquals(null, paneDropSide(-1f, 30f, 100f, 100f))
    }

    @Test fun sameHalfNeverChangesPaneOrder() {
        assertFalse(panePlacementChanges(true, true, false, SplitSide.LEFT))
        assertFalse(panePlacementChanges(false, true, false, SplitSide.RIGHT))
        assertFalse(panePlacementChanges(true, true, true, SplitSide.RIGHT))
        assertFalse(panePlacementChanges(false, false, true, SplitSide.TOP))
    }

    @Test fun crossingOrChangingAxisDoesChangePlacement() {
        assertTrue(panePlacementChanges(true, true, false, SplitSide.RIGHT))
        assertTrue(panePlacementChanges(true, true, false, SplitSide.TOP))
        assertTrue(panePlacementChanges(false, false, false, SplitSide.LEFT))
    }
}
