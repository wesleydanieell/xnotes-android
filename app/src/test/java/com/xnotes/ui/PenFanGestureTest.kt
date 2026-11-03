package com.xnotes.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PenFanGestureTest {
    @Test fun dragTowardEachPenChoosesMatchingItemInAllOrientations() {
        PenFanDirection.entries.forEach { direction ->
            repeat(6) { index ->
                val delta = penFanOffset(index, 6, direction, 140f)
                assertEquals(index, penFanTarget(delta, direction, 6, 18f))
            }
        }
    }

    @Test fun tapAndWrongDirectionDoNotCommitAccidentally() {
        assertNull(penFanTarget(Offset.Zero, PenFanDirection.DOWN, 6, 18f))
        assertNull(penFanTarget(Offset(0f, -120f), PenFanDirection.DOWN, 6, 18f))
        assertNull(penFanTarget(Offset(50f, 0f), PenFanDirection.DOWN, 6, 18f))
        assertNull(penFanTarget(Offset(Float.NaN, 120f), PenFanDirection.DOWN, 6, 18f))
        assertNull(penFanTarget(Offset(0f, 120f), PenFanDirection.DOWN, 0, 18f))
    }
}
