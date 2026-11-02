package com.xnotes.ui

import androidx.compose.ui.unit.dp
import com.xnotes.core.workspace.SplitSide
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitPreviewGeometryTest {
    @Test fun leftAndRightShareWholeWorkspace() {
        assertEquals(SplitPreviewRect(0.dp, 16.dp, 500.dp, 584.dp),
            splitPreviewRect(SplitSide.LEFT, 1000.dp, 600.dp, 16.dp))
        assertEquals(SplitPreviewRect(500.dp, 16.dp, 500.dp, 584.dp),
            splitPreviewRect(SplitSide.RIGHT, 1000.dp, 600.dp, 16.dp))
    }

    @Test fun topAndBottomExcludeTheirOwnToolbar() {
        assertEquals(SplitPreviewRect(0.dp, 42.dp, 1000.dp, 258.dp),
            splitPreviewRect(SplitSide.TOP, 1000.dp, 600.dp, 42.dp))
        assertEquals(SplitPreviewRect(0.dp, 342.dp, 1000.dp, 258.dp),
            splitPreviewRect(SplitSide.BOTTOM, 1000.dp, 600.dp, 42.dp))
    }

    @Test fun extremeToolbarDoesNotCreateNegativeHeight() {
        assertEquals(0.dp, splitPreviewRect(SplitSide.TOP, 200.dp, 100.dp, 80.dp).height)
    }
}
