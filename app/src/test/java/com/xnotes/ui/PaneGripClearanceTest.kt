package com.xnotes.ui

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Test

class PaneGripClearanceTest {
    private val pane = Rect(0f, 100f, 500f, 800f)
    @Test fun stackedBarsAndTabsClearTheGripRegardlessOfOrder() {
        val bars = listOf(Rect(0f, 100f, 500f, 140f), Rect(200f, 140f, 400f, 190f))
        assertEquals(94f, paneGripClearance(pane, bars.reversed(), 48f, 16f, 4f))
    }
    @Test fun AdjacentBarDoesNotPushAnUncoveredGrip() {
        assertEquals(4f, paneGripClearance(pane, listOf(Rect(0f, 100f, 150f, 160f)), 48f, 16f, 4f))
    }
    @Test fun DockOutsidePaneAndOtherPaneDoNotPushGrip() {
        assertEquals(4f, paneGripClearance(pane, listOf(Rect(0f, 40f, 500f, 100f), Rect(500f, 100f, 1000f, 200f)), 48f, 16f, 4f))
    }
    @Test fun NoRoomKeepsGripInsidePane() {
        assertEquals(684f, paneGripClearance(pane, listOf(pane), 48f, 16f, 4f))
    }
}
