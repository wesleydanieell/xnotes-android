package com.xnotes.core.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolbarResizeTest {
    @Test fun resizeTurnsAutomaticDimensionsIntoManualBounds() {
        val resized = resizeToolbarGeometry(ToolbarGeometry(), 40f, -12f)
        assertEquals(240, resized.widthDp)
        assertEquals(52, resized.heightDp)
    }

    @Test fun resizeClampsToSafeDimensions() {
        val resized = resizeToolbarGeometry(
            ToolbarGeometry(widthDp = 60, heightDp = 60),
            -1000f, 10000f,
        )
        assertEquals(48, resized.widthDp)
        assertEquals(2048, resized.heightDp)
    }
}
