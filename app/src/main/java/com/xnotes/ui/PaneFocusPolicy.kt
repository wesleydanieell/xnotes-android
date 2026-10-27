package com.xnotes.ui

import androidx.compose.ui.input.pointer.PointerEventType

/**
 * A new panel takes focus after the complete pointer gesture, never during its
 * down/move sequence. Recomposition during ink can cancel a native canvas stroke.
 */
internal fun shouldFocusPaneAfterEvent(
    type: PointerEventType,
    anyPointerPressed: Boolean,
    alreadyFocused: Boolean,
): Boolean = !alreadyFocused && type == PointerEventType.Release && !anyPointerPressed
