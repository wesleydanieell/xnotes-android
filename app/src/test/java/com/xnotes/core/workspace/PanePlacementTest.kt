package com.xnotes.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanePlacementTest {
    @Test fun ghostUsesCommonHostCoordinatesForEitherSourcePane() {
        // A grip on the right pane may have a large window X, but the ghost's position
        // must depend only on its pointer and the workspace origin, not source-pane offsets.
        assertEquals(76f to 32f, paneGhostPosition(400f, 200f, 300f, 160f, 24f, 8f))
        assertEquals(76f to 32f, paneGhostPosition(100f, 40f, 0f, 0f, 24f, 8f))
    }

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
