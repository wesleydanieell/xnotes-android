package com.xnotes.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.SystemClock
import android.test.InstrumentationTestCase
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.xnotes.MainActivity
import com.xnotes.R
import com.xnotes.core.model.Stroke
import com.xnotes.core.stroke.Sample
import com.xnotes.core.tools.Tool
import com.xnotes.core.tools.ToolDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Imports a real synthetic PDF, deletes an ink stroke using the real menu and stylus route. */
@Suppress("DEPRECATION")
class PdfSelectionDeleteTest : InstrumentationTestCase() {
    private lateinit var activity: MainActivity
    private lateinit var editor: Editor

    override fun tearDown() {
        if (::editor.isInitialized) instrumentation.runOnMainSync { editor.goHomeAll() }
        if (::activity.isInitialized) instrumentation.runOnMainSync { activity.finish() }
        super.tearDown()
    }

    private fun findDelete(n: AccessibilityNodeInfo?): android.graphics.Rect? {
        if (n == null) return null
        if (n.contentDescription?.toString() == activity.getString(R.string.delete)) {
            val r = android.graphics.Rect(); n.getBoundsInScreen(r)
            if (!r.isEmpty) return r
        }
        for (i in 0 until n.childCount) findDelete(n.getChild(i))?.let { return it }
        return null
    }

    fun testPdfInkCanBeDeletedWithStylusAndRestoredWithoutChangingPdf() {
        assertTrue(instrumentation.targetContext.packageName.endsWith(".debug"))
        activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val cache = instrumentation.targetContext.cacheDir
        val pdf = File(cache, "selection-regression.pdf")
        val doc = PdfDocument()
        try {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(600, 800, 1).create())
            page.canvas.drawColor(Color.WHITE)
            page.canvas.drawText("Synthetic PDF selection fixture", 30f, 40f, Paint().apply { color = Color.BLACK; textSize = 18f })
            doc.finishPage(page)
            pdf.outputStream().use { doc.writeTo(it) }
        } finally { doc.close() }
        val latch = CountDownLatch(1)
        var saved = false
        instrumentation.runOnMainSync {
            val field = MainActivity::class.java.getDeclaredField("editor").apply { isAccessible = true }
            editor = field.get(activity) as Editor
            editor.newNote()
            // Autosave remains inside the test package's cache, never in a user's notes folder.
            editor.saveToThen(Uri.fromFile(File(cache, "selection-regression.xnote")).toString()) { saved = it; latch.countDown() }
        }
        assertTrue(latch.await(15, TimeUnit.SECONDS)); assertTrue(saved)
        runBlocking { withContext(Dispatchers.Main) { editor.insertPdfAfter(0, Uri.fromFile(pdf).toString()) } }
        Thread.sleep(500)
        val page = editor.state.document.pages.firstOrNull { it.pdfPage != null }
        assertNotNull("PDF import failed", page)
        val beforePdf = editor.state.document.pdfFile!!.readBytes()
        val ink = Stroke(Tool.PEN, ToolDefaults.configFor(Tool.PEN), mutableListOf(
            Sample(100.0, 100.0, 1.0), Sample(160.0, 140.0, 1.0), Sample(220.0, 120.0, 1.0)))
        instrumentation.runOnMainSync {
            page!!.items.add(ink)
            editor.state.invalidatePage(page)
            editor.history.clear()
            editor.selectAll()
            editor.view.requestRender()
        }
        var bounds: android.graphics.Rect? = null
        val deadline = SystemClock.uptimeMillis() + 5000
        while (bounds == null && SystemClock.uptimeMillis() < deadline) {
            bounds = findDelete(instrumentation.uiAutomation.rootInActiveWindow)
            if (bounds == null) Thread.sleep(50)
        }
        assertNotNull("PDF ink selection did not expose Delete", bounds)
        Thread.sleep(150)
        val origin = IntArray(2)
        instrumentation.runOnMainSync { activity.window.decorView.getLocationOnScreen(origin) }
        val down = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val prop = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_STYLUS }
            val point = MotionEvent.PointerCoords().apply {
                x = bounds!!.exactCenterX() - origin[0]; y = bounds!!.exactCenterY() - origin[1]; pressure = 1f; size = 1f
            }
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, 1, arrayOf(prop), arrayOf(point),
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_STYLUS, 0)
            instrumentation.runOnMainSync { activity.dispatchTouchEvent(event) }; event.recycle()
            Thread.sleep(100)
        }
        assertFalse("Selected ink survived the trash action", ink in page!!.items)
        instrumentation.runOnMainSync { editor.undo() }
        assertTrue("Undo did not restore PDF ink", ink in page.items)
        instrumentation.runOnMainSync { editor.redo() }
        assertFalse("Redo did not delete PDF ink again", ink in page.items)
        assertTrue("PDF background was modified", beforePdf.contentEquals(editor.state.document.pdfFile!!.readBytes()))
    }
}
