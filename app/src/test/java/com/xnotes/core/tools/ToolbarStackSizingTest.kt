package com.xnotes.core.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolbarStackSizingTest {
    @Test fun leavesRoomForAnotherBarAndGap() {
        assertEquals(300f, toolbarStackBarLimit(300f, 1, 8f), 0f)
        assertEquals(146f, toolbarStackBarLimit(300f, 2, 8f), 0f)
        assertEquals(146f, toolbarStackBarLimit(300f, 5, 8f), 0f)
    }

    @Test fun tinyAndInvalidSpacesDoNotProduceNegativeBounds() {
        assertEquals(0f, toolbarStackBarLimit(4f, 2, 8f), 0f)
        assertEquals(0f, toolbarStackBarLimit(Float.NaN, 2, 8f), 0f)
        assertEquals(0f, toolbarStackBarLimit(300f, 0, 8f), 0f)
    }
}
