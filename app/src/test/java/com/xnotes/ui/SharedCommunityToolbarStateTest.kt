package com.xnotes.ui

import androidx.compose.ui.geometry.Rect
import com.xnotes.core.tools.ToolbarPaneInsets
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedCommunityToolbarStateTest {
    private val first = Rect(0f, 100f, 500f, 900f)
    private val second = Rect(500f, 100f, 1000f, 900f)

    @Test fun differentFloatingBarsReserveTheirOwnPaneEdges() {
        val state = SharedCommunityToolbarState()
        state.update("first", Rect(20f, 108f, 400f, 156f), true, "top")
        state.update("second", Rect(600f, 108f, 960f, 206f), true, "top")
        assertEquals(ToolbarPaneInsets(top = 56.0), state.insets(first))
        assertEquals(ToolbarPaneInsets(top = 106.0), state.insets(second))
    }

    @Test fun stackedLanesUseTheFarthestCoverRatherThanAddingInsets() {
        val state = SharedCommunityToolbarState()
        state.update(1, Rect(100f, 108f, 400f, 156f), true, "top")
        state.update(2, Rect(100f, 164f, 400f, 212f), true, "top")
        assertEquals(ToolbarPaneInsets(top = 112.0), state.insets(first))
    }

    @Test fun dockedAndDisposedBarsStopReservingSpace() {
        val state = SharedCommunityToolbarState()
        state.update(1, Rect(8f, 200f, 56f, 600f), true, "left")
        assertEquals(ToolbarPaneInsets(left = 56.0), state.insets(first))
        state.update(1, Rect(8f, 200f, 56f, 600f), false, "left")
        assertEquals(ToolbarPaneInsets(), state.insets(first))
        state.update(1, Rect(8f, 200f, 56f, 600f), true, "left")
        state.remove(1)
        assertEquals(ToolbarPaneInsets(), state.insets(first))
    }

    @Test fun aSideRailDoesNotShrinkTheOppositePane() {
        val state = SharedCommunityToolbarState()
        state.update(1, Rect(944f, 200f, 992f, 700f), true, "right")
        assertEquals(ToolbarPaneInsets(), state.insets(first))
        assertEquals(ToolbarPaneInsets(right = 56.0), state.insets(second))
    }

    @Test fun coversUseTheNativeSurfaceAfterSidebarAndDockInsets() {
        val state = SharedCommunityToolbarState()
        state.update(1, Rect(8f, 200f, 56f, 700f), true, "left")
        assertEquals(ToolbarPaneInsets(), state.insets(Rect(160f, 100f, 500f, 900f)))
    }
}
