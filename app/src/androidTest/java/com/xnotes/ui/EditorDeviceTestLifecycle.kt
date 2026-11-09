package com.xnotes.ui

import android.app.Instrumentation
import android.os.SystemClock
import junit.framework.Assert.assertTrue

/** Fixtures must not race the activity's asynchronous session restore or pending close writes. */
internal fun prepareTestEditor(instrumentation: Instrumentation, editor: Editor) {
    val loaded = Editor::class.java.getDeclaredField("sessionLoaded").apply { isAccessible = true }
    val deadline = SystemClock.uptimeMillis() + 10000
    var ready = false
    while (!ready && SystemClock.uptimeMillis() < deadline) {
        instrumentation.runOnMainSync { ready = loaded.getBoolean(editor) }
        if (!ready) Thread.sleep(30)
    }
    assertTrue("Session restore did not complete before fixture setup", ready)
    // The production loader has a minimum display time and then fades out.
    Thread.sleep(700)
    instrumentation.waitForIdleSync()
    closeTestEditors(instrumentation, editor)
}

internal fun closeTestEditors(instrumentation: Instrumentation, editor: Editor) {
    instrumentation.runOnMainSync { editor.goHomeAll() }
    val deadline = SystemClock.uptimeMillis() + 10000
    var closed = false
    while (!closed && SystemClock.uptimeMillis() < deadline) {
        instrumentation.runOnMainSync { closed = !editor.noteOpen && editor.secondary?.noteOpen != true }
        if (!closed) Thread.sleep(30)
    }
    assertTrue("Fixture editors did not finish closing", closed)
    instrumentation.waitForIdleSync()
}
