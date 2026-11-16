package com.xnotes.core.workspace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanePlacementTest {
    @Test fun gripFollowsVisualSlotAfterSwappingPanels() {
        // The primary editor can occupy either side. The grip follows the SLOT, not
        // the editor's construction order or previously opened document.
        assertEquals(76f to 0f, paneGripOffset(true, 200f, 200f, 400f, true, 48f))
        assertEquals(276f to 0f, paneGripOffset(true, 200f, 200f, 400f, false, 48f))
        assertEquals(126f to 0f, paneGripOffset(true, 300f, 100f, 400f, true, 48f))
        assertEquals(176f to 200f, paneGripOffset(false, 200f, 200f, 400f, false, 48f))
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
