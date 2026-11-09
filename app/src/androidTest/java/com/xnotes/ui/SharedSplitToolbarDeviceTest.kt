package com.xnotes.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.SystemClock
import android.test.InstrumentationTestCase
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.xnotes.MainActivity
import com.xnotes.R
import com.xnotes.core.tools.*
import com.xnotes.settings.*

/** Real split host, real surfaces and real toolbar actions. Use an isolated test application ID. */
@Suppress("DEPRECATION")
class SharedSplitToolbarDeviceTest : InstrumentationTestCase() {
    private lateinit var activity: MainActivity
    private lateinit var editor: Editor
    private lateinit var previousTabs: com.xnotes.core.workspace.NoteTabs
    private lateinit var previousWorkspaces: ToolbarWorkspaces
    private lateinit var previousPreferences: Preferences

    override fun tearDown() {
        if (::activity.isInitialized) instrumentation.runOnMainSync {
            val input = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            input.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        if (::editor.isInitialized) {
            closeTestEditors(instrumentation, editor)
            instrumentation.runOnMainSync {
                if (::previousWorkspaces.isInitialized) editor.applyToolbarWorkspaces(previousWorkspaces)
                if (::previousPreferences.isInitialized) editor.applyHomePreferences(previousPreferences)
            }
        }
        if (::activity.isInitialized) instrumentation.runOnMainSync { activity.finish() }
        instrumentation.waitForIdleSync()
        if (::previousTabs.isInitialized) com.xnotes.platform.NoteTabsStore(instrumentation.targetContext).save(previousTabs)
        super.tearDown()
    }

    private fun show(firstCanvas: Boolean, secondCanvas: Boolean) {
        assertTrue("Use the isolated validation build", instrumentation.targetContext.packageName.endsWith(".sharedsplitcommunity"))
        val tabsStore = com.xnotes.platform.NoteTabsStore(instrumentation.targetContext)
        previousTabs = tabsStore.load()
        val firstUri = java.io.File(instrumentation.targetContext.cacheDir,
            if (firstCanvas) "split-fixture-a.xcanvas" else "split-fixture-a.xnote").toURI().toString()
        val secondUri = java.io.File(instrumentation.targetContext.cacheDir,
            if (secondCanvas) "split-fixture-b.xcanvas" else "split-fixture-b.xnote").toURI().toString()
        tabsStore.save(com.xnotes.core.workspace.NoteTabs().open(firstUri, "Fixture A").open(secondUri, "Fixture B", true))
        activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        instrumentation.runOnMainSync {
            editor = MainActivity::class.java.getDeclaredField("editor").apply { isAccessible = true }.get(activity) as Editor
            previousPreferences = editor.preferences
            editor.applyHomePreferences(editor.preferences.copy(checkUpdatesOnStartup = false))
        }
        prepareTestEditor(instrumentation, editor)
        instrumentation.runOnMainSync {
            previousWorkspaces = editor.toolbarWorkspaces
            editor.applyToolbarWorkspaces(ToolbarWorkspaces.fromLegacy(ToolbarLayout.DEFAULT, ToolbarLayout.CANVAS_DEFAULT))
            fun layout(vararg items: ToolbarItem) = ToolbarLayout(listOf(ToolbarSection(items.map { ToolbarEntry(it) })))
            editor.applyToolbarLayout(layout(ToolbarItem.PEN, ToolbarItem.SELECT, ToolbarItem.UNDO, ToolbarItem.REDO, ToolbarItem.SIDEBAR))
            editor.applyCanvasToolbarLayout(layout(ToolbarItem.PEN, ToolbarItem.SELECT, ToolbarItem.UNDO, ToolbarItem.REDO, ToolbarItem.MINIMAP))
            editor.applyHomePreferences(previousPreferences.copy(checkUpdatesOnStartup = false, sharedSplitToolbars = true, toolbarLook = ToolbarLook(), mixedSplitToolbar = SplitToolbarInterface.NOTE))
            if (firstCanvas) editor.newCanvas() else editor.newNote()
            val second = editor.secondaryPane()
            if (secondCanvas) second.newCanvas() else second.newNote()
            editor.focusPane(Pane.PRIMARY)
            // Fixture tool state must not inherit a TEXT session from the preceding IME test.
            editor.selectTool(Tool.PEN)
            if (second.canvasOpen) second.infinite.armTool(Tool.PEN) else second.selectTool(Tool.PEN)
        }
        settle()
    }

    private fun settle() {
        instrumentation.waitForIdleSync()
        Thread.sleep(200)
    }

    private fun nodes(description: String, descriptionOnly: Boolean = false): List<Pair<Rect, Boolean>> {
        // The same semantics node moves between docked/floating parents. Android's
        // accessibility cache can retain its preceding screen bounds after that move.
        if (android.os.Build.VERSION.SDK_INT >= 33) instrumentation.uiAutomation.clearCache()
        val found = mutableListOf<Pair<Rect, Boolean>>()
        fun visit(node: AccessibilityNodeInfo?) {
            if (node == null) return
            if (node.contentDescription?.toString() == description || (!descriptionOnly && node.text?.toString() == description)) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                if (!bounds.isEmpty) found.add(bounds to node.isEnabled)
            }
            for (i in 0 until node.childCount) visit(node.getChild(i))
        }
        visit(instrumentation.uiAutomation.rootInActiveWindow)
        return found
    }

    private fun waitNodes(description: String, descriptionOnly: Boolean = false): List<Pair<Rect, Boolean>> {
        val until = SystemClock.uptimeMillis() + 4000
        var result = nodes(description, descriptionOnly)
        while (result.isEmpty() && SystemClock.uptimeMillis() < until) {
            Thread.sleep(80)
            result = nodes(description, descriptionOnly)
        }
        if (result.isEmpty()) {
            val debug = StringBuilder("Missing: $description\n")
            instrumentation.runOnMainSync {
                debug.append("primary open=${editor.noteOpen} canvas=${editor.canvasOpen}; secondary open=${editor.secondary?.noteOpen} canvas=${editor.secondary?.canvasOpen}\n")
                debug.append("shared=${editor.preferences.sharedSplitToolbars}; interface=${editor.preferences.mixedSplitToolbar}; tabs=${editor.preferences.documentTabsBar}\n")
            }
            fun dump(n: AccessibilityNodeInfo?) {
                if (n == null) return
                if (n.text != null || n.contentDescription != null) debug.append("text=${n.text}; desc=${n.contentDescription}; click=${n.isClickable}\n")
                for (i in 0 until n.childCount) dump(n.getChild(i))
            }
            dump(instrumentation.uiAutomation.rootInActiveWindow)
            java.io.File(activity.getExternalFilesDir(null), "shared-split-failure.txt").writeText(debug.toString())
            val shot = instrumentation.uiAutomation.takeScreenshot()
            java.io.File(activity.getExternalFilesDir(null), "shared-split-failure.png").outputStream().use {
                shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            shot.recycle()
        }
        assertFalse("Missing $description", result.isEmpty())
        return result
    }

    private fun click(description: String) {
        val descriptionOnly = description == activity.getString(R.string.split_settings)
        var target = waitNodes(description, descriptionOnly).first().first
        var stableSince = SystemClock.uptimeMillis()
        val deadline = stableSince + 5000
        // Floating tabs move with springs and avoid measured toolbars. Tap only a settled
        // accessibility target, including popup windows, rather than its preceding frame.
        while (SystemClock.uptimeMillis() - stableSince < 250 && SystemClock.uptimeMillis() < deadline) {
            Thread.sleep(80)
            val next = waitNodes(description, descriptionOnly).first().first
            if (next != target) { target = next; stableSince = SystemClock.uptimeMillis() }
        }
        tap(target.exactCenterX(), target.exactCenterY())
        settle()
    }

    private fun tap(screenX: Float, screenY: Float) {
        val time = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(time, SystemClock.uptimeMillis(), action, screenX, screenY, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.sendPointerSync(event)
            event.recycle()
        }
    }

    private fun checkEdgesAndRatios() {
        val pen = activity.getString(R.string.tool_pen)
        val firstSurface = if (editor.canvasOpen) editor.infinite.surfaces else editor.surfaces
        val second = editor.secondary!!
        val secondSurface = if (second.canvasOpen) second.infinite.surfaces else second.surfaces
        for (orientation in listOf(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)) {
        instrumentation.runOnMainSync { activity.requestedOrientation = orientation }
        Thread.sleep(600)
        for (floating in listOf(false, true)) for (edge in ToolbarPosition.entries) {
            instrumentation.runOnMainSync {
                editor.applyHomePreferences(editor.preferences.copy(toolbarLook = ToolbarLook(edge, floating = floating)))
            }
            settle()
            for (ratio in listOf(.25f, .5f, .75f)) {
                instrumentation.runOnMainSync { editor.splitRatio = ratio }
                settle()
                assertEquals("Duplicate tools at $edge/$floating/$ratio", 1, waitNodes(pen).size)
                instrumentation.runOnMainSync {
                    assertTrue(firstSurface.width > 0 && secondSurface.width > 0)
                    assertSame(firstSurface, if (editor.canvasOpen) editor.infinite.surfaces else editor.surfaces)
                    assertSame(secondSurface, if (second.canvasOpen) second.infinite.surfaces else second.surfaces)
                }
            }
        }
        }
    }

    fun testNotesShareOneToolbarOnEveryEdge() { show(false, false); checkEdgesAndRatios() }
    fun testCanvasesShareOneToolbarOnEveryEdge() { show(true, true); checkEdgesAndRatios() }

    fun testMixedInterfaceChoiceAndUnavailableControls() {
        show(false, true)
        val second = editor.secondary!!
        waitNodes(activity.getString(R.string.side_panel))
        instrumentation.runOnMainSync { editor.focusPane(Pane.SECONDARY) }
        settle()
        val sidebar = activity.getString(R.string.split_tool_unavailable, activity.getString(ToolbarItem.SIDEBAR.labelRes))
        assertTrue(waitNodes(sidebar).all { !it.second })
        assertSame(second, editor.active)
        click(activity.getString(R.string.split_settings))
        click(activity.getString(R.string.split_interface_canvas))
        assertEquals(SplitToolbarInterface.CANVAS, editor.preferences.mixedSplitToolbar)
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        settle()
        waitNodes(activity.getString(R.string.toolbar_minimap))
        instrumentation.runOnMainSync { editor.focusPane(Pane.PRIMARY) }
        settle()
        val minimap = activity.getString(R.string.split_tool_unavailable, activity.getString(ToolbarItem.MINIMAP.labelRes))
        assertTrue(waitNodes(minimap).all { !it.second })
        assertEquals(1, waitNodes(activity.getString(R.string.tool_pen)).size)
    }

    fun testPreferenceSwitchesSharedAndPerPaneInTheNote() {
        show(false, true)
        click(activity.getString(R.string.split_settings))
        click(activity.getString(R.string.split_per_pane))
        assertFalse(editor.preferences.sharedSplitToolbars)
        // Moving navigation to/from the fixed split row can already dismiss its popup.
        if (nodes(activity.getString(R.string.split_per_pane)).isNotEmpty())
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        settle()
        assertEquals(2, waitNodes(activity.getString(R.string.tool_pen)).size)
        instrumentation.runOnMainSync {
            editor.selectTool(Tool.PAN)
            editor.secondary!!.infinite.armTool(Tool.PEN)
            editor.focusPane(Pane.SECONDARY)
            assertEquals(Tool.PEN, editor.secondary!!.infinite.tool)
        }
        click(activity.getString(R.string.split_settings))
        click(activity.getString(R.string.split_shared))
        assertTrue(editor.preferences.sharedSplitToolbars)
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        java.io.File(activity.getExternalFilesDir(null), "shared-split-settings.png").outputStream().use {
            screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        screenshot.recycle()
        // Moving navigation to/from the fixed split row can already dismiss its popup.
        if (nodes(activity.getString(R.string.split_per_pane)).isNotEmpty())
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        settle()
        assertEquals(1, waitNodes(activity.getString(R.string.tool_pen)).size)
    }

    private fun count(pane: Editor): Int = if (pane.canvasOpen) pane.infinite.document.items.size
        else pane.state.document.pages.sumOf { it.items.size }

    private fun stroke(pane: Editor) {
        val origin = IntArray(2)
        val location = IntArray(2)
        var x = 0f
        var y = 0f
        instrumentation.runOnMainSync {
            val surface = if (pane.canvasOpen) pane.infinite.surfaces else pane.surfaces
            surface.getLocationOnScreen(location)
            activity.window.decorView.getLocationOnScreen(origin)
            x = location[0] - origin[0] + surface.width * .45f
            y = location[1] - origin[1] + surface.height * .4f
        }
        val time = SystemClock.uptimeMillis()
        for (i in 0..5) {
            val props = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_STYLUS }
            val coords = MotionEvent.PointerCoords().apply { this.x = x + i * 8; this.y = y + i * 6; pressure = .7f; size = .1f }
            val event = MotionEvent.obtain(time, time + i * 16, when (i) { 0 -> MotionEvent.ACTION_DOWN; 5 -> MotionEvent.ACTION_UP; else -> MotionEvent.ACTION_MOVE },
                1, arrayOf(props), arrayOf(coords), 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_STYLUS, 0)
            instrumentation.runOnMainSync { activity.dispatchTouchEvent(event) }
            event.recycle()
        }
        settle()
    }

    private fun checkDrawingAndUndo(firstCanvas: Boolean, secondCanvas: Boolean, canvasInterface: Boolean = false) {
        show(firstCanvas, secondCanvas)
        if (canvasInterface) {
            instrumentation.runOnMainSync {
                editor.applyHomePreferences(editor.preferences.copy(mixedSplitToolbar = SplitToolbarInterface.CANVAS))
            }
            settle()
        }
        click(activity.getString(R.string.tool_pen))
        stroke(editor)
        assertEquals(1, count(editor))
        val second = editor.secondary!!
        stroke(second) // first DOWN must focus and carry the pen before the native view draws.
        assertSame(second, editor.active)
        assertEquals(1, count(second))
        click(activity.getString(R.string.undo))
        assertEquals(0, count(second))
        assertEquals(1, count(editor))
        click(activity.getString(R.string.redo))
        assertEquals(1, count(second))
    }

    fun testNoteUndoTargetsOnlyActiveDocument() = checkDrawingAndUndo(false, false)
    fun testCanvasUndoTargetsOnlyActiveDocument() = checkDrawingAndUndo(true, true)
    fun testMixedUndoTargetsOnlyActiveDocument() = checkDrawingAndUndo(false, true)
    fun testReversedMixedCanvasInterfaceUndo() = checkDrawingAndUndo(true, false, true)

    private fun checkSelectionSurvivesFocus(canvas: Boolean) {
        show(false, canvas)
        click(activity.getString(R.string.tool_pen))
        val second = editor.secondary!!
        stroke(second)
        instrumentation.runOnMainSync {
            if (canvas) second.infinite.selectAll() else {
                second.selectTool(Tool.SELECT)
                second.selectAll()
            }
            assertTrue(if (canvas) second.infinite.hasSelection else second.hasSelection)
            editor.focusPane(Pane.PRIMARY)
            editor.selectTool(Tool.PEN)
            editor.focusPane(Pane.SECONDARY)
            assertTrue("Changing focus cleared the action's selection",
                if (canvas) second.infinite.hasSelection else second.hasSelection)
            assertEquals(1, count(second))
            if (canvas) second.infinite.deleteSelection() else second.deleteSelection()
            assertEquals(0, count(second))
        }
    }

    fun testNoteSelectionSurvivesReturningFocus() = checkSelectionSurvivesFocus(false)
    fun testCanvasSelectionSurvivesReturningFocus() = checkSelectionSurvivesFocus(true)

    fun testFocusCanBeRequestedBeforeThePrimaryDocumentOpens() {
        show(false, true)
        instrumentation.runOnMainSync {
            editor.focusPane(Pane.SECONDARY)
            editor.goHome()
            assertFalse(editor.noteOpen)
            editor.focusPane(Pane.PRIMARY)
            assertEquals(Pane.PRIMARY, editor.focusedPane)
            editor.newNote()
            assertSame(editor, editor.active)
        }
    }

    fun testRotationAndClosingKeepTheRemainingSurface() {
        show(false, true)
        val second = editor.secondary!!
        val surface = second.infinite.surfaces
        for (orientation in listOf(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)) {
            instrumentation.runOnMainSync { activity.requestedOrientation = orientation }
            Thread.sleep(800)
            settle()
            assertEquals(1, waitNodes(activity.getString(R.string.tool_pen)).size)
            assertSame(surface, second.infinite.surfaces)
        }
        instrumentation.runOnMainSync { editor.goHome() }
        settle()
        assertSame(second, editor.active)
        assertSame(surface, second.infinite.surfaces)
        assertEquals(1, waitNodes(activity.getString(R.string.tool_pen)).size)
        assertTrue(nodes(activity.getString(R.string.split_settings)).isEmpty())
    }
    private fun checkSharedMultiBars(canvas: Boolean) {
        show(canvas, canvas)
        fun single(item: ToolbarItem) = ToolbarLayout(listOf(ToolbarSection(listOf(ToolbarEntry(item)))))
        val workspace = ToolbarWorkspace(listOf(
            ToolbarBar("primary", "Ink", single(ToolbarItem.PEN), geometry = ToolbarGeometry(dockEdge = ToolbarDockEdge.TOP, widthDp = 320)),
            ToolbarBar("bottom", "Erase", single(ToolbarItem.ERASER), geometry = ToolbarGeometry(dockEdge = ToolbarDockEdge.BOTTOM, widthDp = 240)),
            ToolbarBar("float", "Select", single(ToolbarItem.SELECT), geometry = ToolbarGeometry(anchor = ToolbarAnchor.CENTER_RIGHT, floating = true)),
        ))
        val profiles = ToolbarWorkspaces(workspace, workspace)
        instrumentation.runOnMainSync { editor.applyToolbarWorkspaces(profiles) }
        settle()
        for (swapped in listOf(false, true)) {
            instrumentation.runOnMainSync { editor.splitSwapped = swapped; editor.splitRatio = .35f }
            settle()
            for (label in listOf(R.string.tool_pen, R.string.tool_eraser, R.string.tool_select)) {
                assertEquals("A shared workspace bar was duplicated", 1, waitNodes(activity.getString(label)).size)
            }
            assertEquals(profiles, editor.toolbarWorkspaces)
        }
        click(activity.getString(R.string.tool_pen))
        stroke(editor.secondary!!)
        assertEquals(1, count(editor.secondary!!))
        assertEquals(0, count(editor))
        var nativeDowns = 0
        instrumentation.runOnMainSync {
            if (canvas) {
                editor.infinite.beforeSurfaceDown = { nativeDowns++ }
                editor.secondary!!.infinite.beforeSurfaceDown = { nativeDowns++ }
            } else {
                editor.beforeSurfaceDown = { nativeDowns++ }
                editor.secondary!!.beforeSurfaceDown = { nativeDowns++ }
            }
        }
        val rect = waitNodes(activity.getString(R.string.tool_select)).first().first
        val origin = IntArray(2)
        instrumentation.runOnMainSync { activity.window.decorView.getLocationOnScreen(origin) }
        val time = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val props = MotionEvent.PointerProperties().apply { id = 0; toolType = MotionEvent.TOOL_TYPE_STYLUS }
            val coords = MotionEvent.PointerCoords().apply {
                x = rect.exactCenterX() - origin[0]; y = rect.exactCenterY() - origin[1]; pressure = .7f; size = .1f
            }
            val event = MotionEvent.obtain(time, SystemClock.uptimeMillis(), action, 1, arrayOf(props), arrayOf(coords),
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_STYLUS, 0)
            instrumentation.runOnMainSync { activity.dispatchTouchEvent(event) }
            event.recycle()
        }
        settle()
        assertEquals("Shared floating tools forwarded a stylus DOWN to a document", 0, nativeDowns)
        assertEquals(Tool.SELECT, if (canvas) editor.secondary!!.infinite.tool else editor.secondary!!.tool)
        assertEquals(1, count(editor.secondary!!))
    }

    fun testSharedMultiBarNotesKeepPlacementAndProtectStylus() = checkSharedMultiBars(false)
    fun testSharedMultiBarCanvasesKeepPlacementAndProtectStylus() = checkSharedMultiBars(true)

    fun testMixedInterfaceChoiceTravelsWithTabsOnEveryEdge() {
        show(false, true)
        for (floating in listOf(false, true)) for (edge in ToolbarPosition.entries) {
            instrumentation.runOnMainSync {
                editor.applyHomePreferences(editor.preferences.copy(documentTabsBar = DocumentTabsBarConfig(
                    floating = floating, position = edge, anchor = DocumentTabsBarConfig.anchorOf(edge)),
                    documentTabsMode = if (edge == ToolbarPosition.LEFT || edge == ToolbarPosition.BOTTOM) "expanded" else "compact"))
            }
            settle()
            Thread.sleep(350)
            click(activity.getString(R.string.split_settings))
            val next = if (editor.preferences.mixedSplitToolbar == SplitToolbarInterface.NOTE)
                SplitToolbarInterface.CANVAS else SplitToolbarInterface.NOTE
            click(activity.getString(if (next == SplitToolbarInterface.NOTE)
                R.string.split_interface_note else R.string.split_interface_canvas))
            assertEquals(next, editor.preferences.mixedSplitToolbar)
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            settle()
            assertEquals(1, waitNodes(activity.getString(R.string.tool_pen)).size)
        }
    }
    fun testBottomToolsAndTabsStayAboveTheKeyboard() {
        if (android.os.Build.VERSION.SDK_INT < 30) return
        show(false, false)
        instrumentation.runOnMainSync {
            editor.applyHomePreferences(editor.preferences.copy(
                toolbarLook = ToolbarLook(position = ToolbarPosition.BOTTOM),
                documentTabsBar = DocumentTabsBarConfig(floating = false, position = ToolbarPosition.BOTTOM,
                    anchor = ToolbarAnchor.BOTTOM_CENTER)))
        }
        settle()
        val penBeforeIme = waitNodes(activity.getString(R.string.tool_pen)).single().first
        val redoBeforeIme = waitNodes(activity.getString(R.string.redo)).single().first
        instrumentation.runOnMainSync {
            editor.selectTool(Tool.TEXT)
            editor.flowText.startSession(com.xnotes.core.text.FlowRange.caret(com.xnotes.core.text.FlowPos.START))
        }
        val deadline = SystemClock.uptimeMillis() + 6000
        var visible = false
        var keyboardTop = Int.MAX_VALUE
        while (!visible && SystemClock.uptimeMillis() < deadline) {
            instrumentation.runOnMainSync {
                val insets = androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)
                visible = insets?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
                if (visible) keyboardTop = activity.windowManager.currentWindowMetrics.bounds.bottom -
                    (insets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())?.bottom ?: 0)
            }
            if (!visible) Thread.sleep(80)
        }
        assertTrue("The real text session did not open the IME", visible)
        Thread.sleep(500)
        instrumentation.runOnMainSync {
            val insets = androidx.core.view.ViewCompat.getRootWindowInsets(activity.window.decorView)
            keyboardTop = activity.windowManager.currentWindowMetrics.bounds.bottom -
                (insets?.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())?.bottom ?: 0)
        }
        val penAfterIme = waitNodes(activity.getString(R.string.tool_pen)).single().first
        val redoAfterIme = waitNodes(activity.getString(R.string.redo)).single().first
        assertEquals("Shared buttons shrank with the viewport", penBeforeIme.width(), penAfterIme.width())
        assertEquals("Shared bar length changed with the viewport",
            redoBeforeIme.centerX() - penBeforeIme.centerX(), redoAfterIme.centerX() - penAfterIme.centerX())
        for (label in listOf(activity.getString(R.string.tool_pen), activity.getString(R.string.split_settings))) {
            assertTrue("Bottom UI is covered by the IME: $label", waitNodes(label).all { it.first.bottom <= keyboardTop + 2 })
        }
        instrumentation.runOnMainSync {
            editor.flowText.endSession()
            val input = activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            input.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        settle()
    }

    fun testMixedPerPaneBarsStayInsideBothPanesAfterModeSwitch() = checkPerPaneBars(false, true)
    fun testNotesPerPaneBarsStayInsideBothPanesAfterModeSwitch() = checkPerPaneBars(false, false)
    fun testCanvasesPerPaneBarsStayInsideBothPanesAfterModeSwitch() = checkPerPaneBars(true, true)

    private fun checkPerPaneBars(firstCanvas: Boolean, secondCanvas: Boolean) {
        show(firstCanvas, secondCanvas)
        instrumentation.runOnMainSync {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            editor.splitAxis = SplitAxis.HORIZONTAL
            editor.splitRatio = .5f
        }
        Thread.sleep(600)
        fun one(item: ToolbarItem) = ToolbarLayout(listOf(ToolbarSection(listOf(ToolbarEntry(item)))))
        for (floating in listOf(false, true)) for (edge in ToolbarPosition.entries) {
            val workspace = ToolbarWorkspace(listOf(
                ToolbarBar("primary", "Pen", one(ToolbarItem.PEN), geometry = ToolbarGeometry(
                    floating = floating, anchor = DocumentTabsBarConfig.anchorOf(edge),
                    dockEdge = if (floating) null else edge.dockEdge(),
                    widthDp = if (edge.vertical) null else 700,
                    heightDp = if (edge.vertical) 500 else null)),
                ToolbarBar("erase", "Erase", one(ToolbarItem.ERASER), geometry = ToolbarGeometry(
                    floating = true, anchor = ToolbarAnchor.BOTTOM_RIGHT)),
            ))
            instrumentation.runOnMainSync {
                editor.applyToolbarWorkspaces(ToolbarWorkspaces(workspace, workspace))
                editor.applyHomePreferences(editor.preferences.copy(sharedSplitToolbars = true,
                    toolbarLook = ToolbarLook(edge, floating = floating),
                    documentTabsMode = "expanded",
                    documentTabsBar = DocumentTabsBarConfig(floating = true,
                        anchor = if (edge == ToolbarPosition.BOTTOM) ToolbarAnchor.BOTTOM_LEFT else ToolbarAnchor.TOP_LEFT)))
            }
            settle()
            assertEquals(1, waitNodes(activity.getString(R.string.tool_pen)).size)
            instrumentation.runOnMainSync {
                editor.applyHomePreferences(editor.preferences.copy(sharedSplitToolbars = false))
            }
            settle()
            for (label in listOf(R.string.tool_pen, R.string.tool_eraser)) {
                val boxes = waitNodes(activity.getString(label)).map { it.first }
                assertEquals("Missing bar at $edge/$floating", 2, boxes.size)
                val seam = activity.window.decorView.width / 2
                assertEquals("Tools escaped their pane at $edge/$floating: $boxes", 1,
                    boxes.count { it.centerX() < seam })
                assertTrue(boxes.all { it.left >= 0 && it.right <= activity.window.decorView.width })
                val grips = waitNodes(activity.getString(R.string.move_split_pane)).map { it.first }
                assertTrue("Pane grip overlaps tools at $edge/$floating", grips.none { g -> boxes.any { Rect.intersects(g, it) } })
                val tabs = Rect()
                for (tabLabel in listOf("Fixture A", "Fixture B", activity.getString(R.string.split_settings))) {
                    for ((bounds, _) in waitNodes(tabLabel)) tabs.union(bounds)
                }
                assertTrue("Tabs cover a pane toolbar at $edge/$floating: $tabs / $boxes",
                    boxes.none { Rect.intersects(tabs, it) })
                assertTrue("Pane grip overlaps tabs at $edge/$floating", grips.none { Rect.intersects(tabs, it) })
            }
        }
    }

    private fun dragBar(x: Float, y: Float, targetX: Float, targetY: Float) {
        val down = SystemClock.uptimeMillis()
        fun send(action: Int, sx: Float, sy: Float) {
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, sx, sy, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            val origin = IntArray(2)
            instrumentation.runOnMainSync {
                activity.window.decorView.getLocationOnScreen(origin)
                event.offsetLocation(-origin[0].toFloat(), -origin[1].toFloat())
                activity.dispatchTouchEvent(event)
            }
            event.recycle()
        }
        send(MotionEvent.ACTION_DOWN, x, y)
        Thread.sleep(android.view.ViewConfiguration.getLongPressTimeout().toLong() + 150)
        for (step in 1..16) {
            send(MotionEvent.ACTION_MOVE, x + (targetX - x) * step / 16, y + (targetY - y) * step / 16)
            Thread.sleep(20)
        }
        send(MotionEvent.ACTION_UP, targetX, targetY)
        Thread.sleep(500)
        settle()
    }

    fun testDraggingTabsPreservesLatestSplitChoices() {
        show(false, true)
        instrumentation.runOnMainSync {
            editor.applyHomePreferences(editor.preferences.copy(mixedSplitToolbar = SplitToolbarInterface.CANVAS,
                documentTabsBar = DocumentTabsBarConfig(floating = true, anchor = ToolbarAnchor.TOP_LEFT)))
        }
        settle()
        for (shared in listOf(true)) {
            instrumentation.runOnMainSync {
                editor.applyHomePreferences(editor.preferences.copy(sharedSplitToolbars = shared,
                    mixedSplitToolbar = SplitToolbarInterface.NOTE))
            }
            settle()
            val grip = waitNodes("Mover barra de abas").single().first
            val previousAnchor = editor.preferences.documentTabsBar.anchor
            val targetX = if (editor.preferences.documentTabsBar.anchor == ToolbarAnchor.BOTTOM_RIGHT) 60f
                else activity.window.decorView.width - 60f
            dragBar(grip.exactCenterX(), grip.exactCenterY(), targetX, activity.window.decorView.height - 160f)
            assertTrue("Navigation did not move", previousAnchor != editor.preferences.documentTabsBar.anchor)
            assertEquals(shared, editor.preferences.sharedSplitToolbars)
            assertEquals(SplitToolbarInterface.NOTE, editor.preferences.mixedSplitToolbar)
        }
    }

    fun testDraggingSharedToolsUsesLatestMixedInterface() {
        show(false, true)
        val layout = ToolbarLayout(listOf(ToolbarSection(listOf(ToolbarEntry(ToolbarItem.PEN)))))
        val workspace = ToolbarWorkspace(listOf(ToolbarBar("primary", "Ink", layout,
            geometry = ToolbarGeometry(floating = true, widthDp = 240))))
        instrumentation.runOnMainSync {
            editor.applyToolbarWorkspaces(ToolbarWorkspaces(workspace, workspace))
            editor.applyHomePreferences(editor.preferences.copy(mixedSplitToolbar = SplitToolbarInterface.CANVAS))
        }
        settle()
        instrumentation.runOnMainSync {
            editor.applyHomePreferences(editor.preferences.copy(mixedSplitToolbar = SplitToolbarInterface.NOTE))
        }
        settle()
        val pen = waitNodes(activity.getString(R.string.tool_pen)).single().first
        dragBar(pen.left + 300f, pen.exactCenterY(), 80f, activity.window.decorView.height - 150f)
        assertTrue(editor.preferences.sharedSplitToolbars)
        assertEquals(SplitToolbarInterface.NOTE, editor.preferences.mixedSplitToolbar)
        assertEquals(workspace, editor.toolbarWorkspaces.canvas)
        assertTrue("The drag did not move the current xnote profile", workspace != editor.toolbarWorkspaces.note)
    }

    fun testNotePaneDraggingDoesNotMoveTheOtherPane() = checkPaneDragIsolation(false)
    fun testCanvasPaneDraggingDoesNotMoveTheOtherPane() = checkPaneDragIsolation(true)

    fun testMixedPaneDraggingDoesNotMoveTheOtherPane() = checkPaneDragIsolation(false, true)

    private fun checkPaneDragIsolation(firstCanvas: Boolean, secondCanvas: Boolean = firstCanvas) {
        show(firstCanvas, secondCanvas)
        val layout = ToolbarLayout(listOf(ToolbarSection(listOf(ToolbarEntry(ToolbarItem.PEN)))))
        val workspace = ToolbarWorkspace(listOf(ToolbarBar("primary", "Ink", layout,
            geometry = ToolbarGeometry(floating = true, widthDp = 240))))
        val profiles = ToolbarWorkspaces(workspace, workspace)
        instrumentation.runOnMainSync {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            editor.splitAxis = SplitAxis.HORIZONTAL
            editor.splitRatio = .5f
            editor.applyToolbarWorkspaces(profiles)
            editor.applyHomePreferences(editor.preferences.copy(sharedSplitToolbars = false,
                mixedSplitToolbar = SplitToolbarInterface.NOTE))
        }
        Thread.sleep(700)
        val label = activity.getString(R.string.tool_pen)
        val before = waitNodes(label).map { it.first }.sortedBy { it.left }
        assertEquals(2, before.size)
        dragBar(before[0].left + 300f, before[0].exactCenterY(), 80f, activity.window.decorView.height - 150f)
        val after = waitNodes(label).map { it.first }.sortedBy { it.left }
        assertEquals("Moving the left toolbar changed the right toolbar", before[1], after[1])
        assertTrue("Left toolbar did not move", after[0].top > before[0].bottom)
        assertNotNull(editor.paneToolbarWorkspaces)
        assertNull(editor.secondary!!.paneToolbarWorkspaces)
        assertEquals(profiles, editor.toolbarWorkspaces)
        assertFalse(editor.preferences.sharedSplitToolbars)
        assertEquals(SplitToolbarInterface.NOTE, editor.preferences.mixedSplitToolbar)
        val firstPosition = after[0]
        dragBar(after[1].left + 300f, after[1].exactCenterY(), activity.window.decorView.width - 100f,
            activity.window.decorView.height - 150f)
        val final = waitNodes(label).map { it.first }.sortedBy { it.left }
        assertEquals("Moving the right toolbar changed the left toolbar", firstPosition, final[0])
        assertNotNull(editor.secondary!!.paneToolbarWorkspaces)
        assertEquals(profiles, editor.toolbarWorkspaces)
    }

    fun testPerPaneTabsDockAboveBothViewsAndRestoreSharedPlacement() {
        show(false, true)
        val configured = DocumentTabsBarConfig(floating = true, anchor = ToolbarAnchor.BOTTOM_RIGHT)
        instrumentation.runOnMainSync {
            editor.applyHomePreferences(editor.preferences.copy(sharedSplitToolbars = false,
                documentTabsBar = configured, mixedSplitToolbar = SplitToolbarInterface.NOTE))
        }
        settle()
        assertTrue(nodes("Mover barra de abas").isEmpty())
        val control = waitNodes(activity.getString(R.string.split_settings)).single().first
        val origins = listOf(editor.surfaces, editor.secondary!!.infinite.surfaces).map {
            IntArray(2).also { location -> instrumentation.runOnMainSync { it.getLocationOnScreen(location) } }
        }
        assertTrue("Navigation must sit above both document viewports", origins.all { control.bottom <= it[1] })
        assertEquals(configured, editor.preferences.documentTabsBar)
        instrumentation.runOnMainSync {
            editor.applyHomePreferences(editor.preferences.copy(sharedSplitToolbars = true))
        }
        settle()
        val grip = waitNodes("Mover barra de abas").single().first
        assertTrue("Shared navigation did not restore its floating bottom placement", grip.top > activity.window.decorView.height / 2)
        assertEquals(configured, editor.preferences.documentTabsBar)
    }

}
