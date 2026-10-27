package com.xnotes.ui

import androidx.compose.ui.input.pointer.PointerEventType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaneFocusPolicyTest {
    @Test fun inkDownAndMovementCannotSwitchFocus() {
        assertFalse(shouldFocusPaneAfterEvent(PointerEventType.Press, true, false))
        assertFalse(shouldFocusPaneAfterEvent(PointerEventType.Move, true, false))
    }

    @Test fun focusSwitchesAfterLastPointerLifts() {
        assertFalse(shouldFocusPaneAfterEvent(PointerEventType.Release, true, false))
        assertTrue(shouldFocusPaneAfterEvent(PointerEventType.Release, false, false))
        assertFalse(shouldFocusPaneAfterEvent(PointerEventType.Release, false, true))
    }
}
