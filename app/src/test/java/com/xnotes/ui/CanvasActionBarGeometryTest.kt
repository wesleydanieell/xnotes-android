package com.xnotes.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.*
import org.junit.Test

class CanvasActionBarGeometryTest {
    @Test fun clampsBothEdgesInNarrowSplitPanes() {
        for (width in listOf(100f, 220f, 500f)) for (x in listOf(-40f, 10f, width - 10f, width + 40f)) {
            val placed = placeCanvasActionBar(Rect(x, 180f, x + 60f, 240f), Size(width, 400f), Size(500f, 46f), emptyList(), 8f)
            assertTrue(placed.left >= 0 && placed.right <= width)
            assertTrue(placed.top >= 0 && placed.bottom <= 400)
        }
    }
    @Test fun avoidsSelectionAndToolbarsWhenThereIsSpace() {
        val selected = Rect(200f, 90f, 280f, 150f)
        val tools = listOf(Rect(0f, 0f, 600f, 70f), Rect(540f, 70f, 600f, 500f))
        val placed = placeCanvasActionBar(selected, Size(600f, 500f), Size(400f, 46f), tools, 8f)
        assertFalse(placed.overlaps(selected))
        assertTrue(tools.none { it.overlaps(placed) })
    }
    @Test fun keepsDeleteInsideViewportEvenAtBottomAndInTinyWindows() {
        for (size in listOf(Size(120f, 50f), Size(500f, 300f), Size.Zero)) {
            val placed = placeCanvasActionBar(Rect(Offset.Zero, size), size, Size(500f, 46f), emptyList(), 8f)
            assertTrue(placed.left >= 0 && placed.top >= 0)
            assertTrue(placed.right <= size.width && placed.bottom <= size.height)
        }
    }
}
