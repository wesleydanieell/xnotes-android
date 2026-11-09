package com.xnotes.ui

import android.content.Intent
import android.os.SystemClock
import android.test.InstrumentationTestCase
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.xnotes.MainActivity
import com.xnotes.R
import com.xnotes.core.tools.*
import com.xnotes.settings.*

/** Exercises the production split host, real note/canvas toolbars, and overflow recovery. */
@Suppress("DEPRECATION")
class SplitCustomToolbarTest : InstrumentationTestCase() {
    private lateinit var activity: MainActivity
    private lateinit var editor: Editor
    private lateinit var previousPreferences: Preferences
    private lateinit var previousWorkspaces: ToolbarWorkspaces

    override fun tearDown() {
        if (::editor.isInitialized) {
            closeTestEditors(instrumentation, editor)
            instrumentation.runOnMainSync {
                if (::previousWorkspaces.isInitialized) editor.applyToolbarWorkspaces(previousWorkspaces)
                if (::previousPreferences.isInitialized) editor.applyHomePreferences(previousPreferences)
            }
        }
        if (::activity.isInitialized) instrumentation.runOnMainSync { activity.finish() }
        super.tearDown()
    }

    private fun show(firstCanvas: Boolean, secondCanvas: Boolean, vertical: Boolean = false, extras: Boolean = false) {
        assertTrue((instrumentation.targetContext.packageName.endsWith(".debug") || instrumentation.targetContext.packageName.endsWith(".sharedsplitcommunity")))
        activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        instrumentation.runOnMainSync {
            val field = MainActivity::class.java.getDeclaredField("editor").apply { isAccessible = true }
            editor = field.get(activity) as Editor
            previousPreferences = editor.preferences
        }
        prepareTestEditor(instrumentation, editor)
        instrumentation.runOnMainSync {
            previousWorkspaces = editor.toolbarWorkspaces
            val layout = ToolbarLayout(listOf(ToolbarSection(listOf(
                ToolbarEntry(ToolbarItem.PEN, sizeDp = 72), ToolbarEntry(ToolbarItem.SELECT), ToolbarEntry(ToolbarItem.ERASER)))))
            val primary = ToolbarBar("primary", "Main", layout,
                geometry = ToolbarGeometry(dockEdge = ToolbarDockEdge.BOTTOM, widthDp = 1000, elementSizeDp = 64))
            val bars = listOf(primary) + if (extras) (0..3).map {
                ToolbarBar("float-$it", "Extra ${it + 1}", layout,
                    geometry = ToolbarGeometry(floating = true, anchor = ToolbarAnchor.TOP_LEFT, widthDp = 300))
            } else emptyList()
            val workspace = ToolbarWorkspace(bars)
            editor.applyHomePreferences(previousPreferences.copy(sharedSplitToolbars = false, adaptiveSplitToolbars = true,
                toolbarLook = ToolbarLook(size = ToolbarSize.COMFORTABLE), documentTabsBar = DocumentTabsBarConfig()))
            editor.applyToolbarWorkspaces(ToolbarWorkspaces(workspace, workspace))
            if (firstCanvas) editor.newCanvas() else editor.newNote()
            val second = editor.secondaryPane()
            if (secondCanvas) second.newCanvas() else second.newNote()
            editor.splitAxis = if (vertical) SplitAxis.VERTICAL else SplitAxis.HORIZONTAL
            editor.splitRatio = .5f
        }
        instrumentation.waitForIdleSync()
        Thread.sleep(500)
    }

    private fun nodes(predicate: (AccessibilityNodeInfo) -> Boolean): List<android.graphics.Rect> {
        val result = mutableListOf<android.graphics.Rect>()
        fun visit(n: AccessibilityNodeInfo?) {
            if (n == null) return
            if (predicate(n)) {
                val r = android.graphics.Rect(); n.getBoundsInScreen(r)
                if (!r.isEmpty) result.add(r)
            }
            for (i in 0 until n.childCount) visit(n.getChild(i))
        }
        visit(instrumentation.uiAutomation.rootInActiveWindow)
        return result
    }

    private fun checkToolsInBothPanels(vertical: Boolean) {
        for (ratio in listOf(.25f, .5f, .75f)) {
            instrumentation.runOnMainSync { editor.splitRatio = ratio }
            Thread.sleep(300)
            val label = activity.getString(R.string.tool_pen)
            var pens = nodes { it.contentDescription?.toString() == label }
            val deadline = SystemClock.uptimeMillis() + 4000
            while (pens.size < 2 && SystemClock.uptimeMillis() < deadline) {
                Thread.sleep(50); pens = nodes { it.contentDescription?.toString() == label }
            }
            val dm = activity.resources.displayMetrics
            val nativeBounds = listOf(editor, editor.secondary!!).map { pane ->
                val surface = if (pane.canvasOpen) pane.infinite.surfaces else pane.surfaces
                android.graphics.Rect().also { rect ->
                    instrumentation.runOnMainSync { surface.getGlobalVisibleRect(rect) }
                }
            }
            // The split begins below navigation/header/docked chrome; screenHeight*ratio is
            // not its seam. These fixtures dock their bars at the bottom of each pane.
            val seam = nativeBounds.maxOf { if (vertical) it.top else it.left }
            assertTrue("Missing tools in first panel at $ratio: $pens", pens.any { (if (vertical) it.exactCenterY() else it.exactCenterX()) < seam })
            assertTrue("Missing tools in second panel at $ratio: $pens", pens.any { (if (vertical) it.exactCenterY() else it.exactCenterX()) > seam })
            val compactPens = pens.filter { pen ->
                val slot = nativeBounds.sortedBy { if (vertical) it.top else it.left }[
                    if ((if (vertical) pen.exactCenterY() else pen.exactCenterX()) < seam) 0 else 1]
                minOf(slot.width(), slot.height()) / dm.density < 600f
            }
            assertTrue("Saved oversized controls did not adapt in a narrow pane: $compactPens",
                compactPens.all { it.width() <= 24 * dm.density + 2 })
        }
    }

    fun testNotesRemainUsableAtUnequalHorizontalRatios() { show(false, false); checkToolsInBothPanels(false) }
    fun testCanvasesRemainUsableAtUnequalVerticalRatios() { show(true, true, true); checkToolsInBothPanels(true) }
    fun testMixedPanesRemainUsableAfterSwapping() {
        show(false, true)
        instrumentation.runOnMainSync { editor.splitSwapped = true }
        checkToolsInBothPanels(false)
    }
    fun testRejectedBarsRemainReachableFromOverflow() {
        show(false, true, extras = true)
        instrumentation.runOnMainSync { editor.splitRatio = .25f }
        Thread.sleep(500)
        val overflow = nodes { it.contentDescription?.toString()?.startsWith("Outras barras (") == true }
        assertTrue("Constrained pane lost bars without recovery", overflow.isNotEmpty())
        val r = overflow.first()
        val origin = IntArray(2)
        instrumentation.runOnMainSync { activity.window.decorView.getLocationOnScreen(origin) }
        val down = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val e = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action,
                r.exactCenterX() - origin[0], r.exactCenterY() - origin[1], 0)
            e.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.runOnMainSync { activity.dispatchTouchEvent(e) }; e.recycle()
            Thread.sleep(100)
        }
        val deadline = SystemClock.uptimeMillis() + 4000
        var exposed = nodes { it.text?.toString() == "Extra 4" }.isNotEmpty()
        while (!exposed && SystemClock.uptimeMillis() < deadline) {
            Thread.sleep(50)
            exposed = nodes { it.text?.toString() == "Extra 4" }.isNotEmpty()
        }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(instrumentation.targetContext.cacheDir, "split-overflow.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
        assertTrue("Overflow must expose the unavailable bar", exposed)
    }
}
