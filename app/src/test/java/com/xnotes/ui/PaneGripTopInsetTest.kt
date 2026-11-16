package com.xnotes.ui

import androidx.compose.ui.unit.dp
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import com.xnotes.settings.ToolbarSize
import org.junit.Assert.assertEquals
import org.junit.Test

class PaneGripTopInsetTest {
    @Test fun topDockedToolbarKeepsGripBelowItsActualHeight() {
        assertEquals(48.dp, paneGripTopInset(ToolbarLook(size = ToolbarSize.COMPACT)))
        assertEquals(54.dp, paneGripTopInset(ToolbarLook(size = ToolbarSize.REGULAR)))
        assertEquals(62.dp, paneGripTopInset(ToolbarLook(size = ToolbarSize.COMFORTABLE)))
    }

    @Test fun topFloatingToolbarKeepsGripClearOfItsMargin() {
        assertEquals(66.dp, paneGripTopInset(ToolbarLook(floating = true)))
    }

    @Test fun toolbarsOnOtherEdgesDoNotPushGripDown() {
        for (position in listOf(ToolbarPosition.BOTTOM, ToolbarPosition.LEFT, ToolbarPosition.RIGHT)) {
            assertEquals(4.dp, paneGripTopInset(ToolbarLook(position = position)))
            assertEquals(4.dp, paneGripTopInset(ToolbarLook(position = position, floating = true)))
        }
    }
}
