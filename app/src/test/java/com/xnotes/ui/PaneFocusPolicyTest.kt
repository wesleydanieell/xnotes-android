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

    @Test fun stylusFallbackOnlyTargetsMissedCanvasPresses() {
        assertTrue(shouldFallbackStylusToCanvas(
            interopSawDown = false,
            nativeSawEvent = false,
            insideCanvas = true,
            overToolbar = false,
        ))
        assertFalse(shouldFallbackStylusToCanvas(true, false, true, false))
        assertFalse(shouldFallbackStylusToCanvas(false, true, true, false))
        assertFalse(shouldFallbackStylusToCanvas(false, false, true, true))
        assertFalse(shouldFallbackStylusToCanvas(false, false, false, false))
    }
}
