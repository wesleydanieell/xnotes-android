package com.xnotes.core.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ToolbarDropTargetTest {
    @Test fun resolvesAllNineDropRegions() {
        for (row in 0..2) for (column in 0..2) {
            assertEquals(ToolbarAnchor.entries[row * 3 + column],
                toolbarDropTarget(column * 100f + 50f, row * 100f + 50f, 300f, 300f))
        }
    }

    @Test fun edgeDropsAndInvalidBounds() {
        assertEquals(ToolbarAnchor.CENTER_LEFT, toolbarDropTarget(0f, 150f, 300f, 300f))
        assertEquals(ToolbarAnchor.CENTER_RIGHT, toolbarDropTarget(300f, 150f, 300f, 300f))
        assertEquals(ToolbarAnchor.CENTER, toolbarDropTarget(100f, 200f, 300f, 300f))
        assertNull(toolbarDropTarget(1f, 1f, 0f, 300f))
        assertNull(toolbarDropTarget(Float.NaN, 1f, 300f, 300f))
    }
}
