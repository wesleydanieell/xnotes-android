package com.xnotes.ui

import android.content.Intent
import android.test.InstrumentationTestCase
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.xnotes.MainActivity
import com.xnotes.core.tools.*
import com.xnotes.core.workspace.NoteTabs
import com.xnotes.settings.*
import java.util.Collections

/** Synthetic app navigation in the isolated debug package; never edits user documents. */
@Suppress("DEPRECATION")
class DocumentTabsBarAnimationTest : InstrumentationTestCase() {
    private lateinit var activity: MainActivity
    private val sizes = Collections.synchronizedList(mutableListOf<Rect>())
    private var expanded by mutableStateOf(true)
    private var uri by mutableStateOf("one")
    private var host by mutableStateOf(false)
    private var neighbor by mutableStateOf(false)
    private var neighborBounds = Rect.Zero
    private lateinit var motion: DocumentTabsSelectionState
    private var config by mutableStateOf(DocumentTabsBarConfig())

    override fun tearDown() {
        if (::activity.isInitialized) instrumentation.runOnMainSync { activity.finish() }
        super.tearDown()
    }

    private fun show(position: ToolbarPosition, floating: Boolean, preview: Boolean = false) {
        config = DocumentTabsBarConfig(floating, position, DocumentTabsBarConfig.anchorOf(position))
        activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val tabs = NoteTabs().open("one", "First").open("two", "Second")
        val layout = ToolbarLayout(listOf(ToolbarSection(listOf(ToolbarEntry(ToolbarItem.PEN)))))
        val tools = ToolbarWorkspace(listOf(ToolbarBar("tools", "Tools", layout)))
        instrumentation.runOnMainSync {
            activity.setContent {
                MaterialTheme {
                    motion = rememberDocumentTabsSelection(tabs, uri)
                    val documents = DocumentTabsState(tabs, motion, uri, expanded, { expanded = it },
                        { uri = it }, {}, { _, _ -> }, { _, _ -> }, null)
                    CompositionLocalProvider(LocalDocumentTabsState provides documents,
                        LocalDocumentTabsConfig provides config,
                        LocalDocumentTabsBoundsObserver provides { sizes.add(it) }) {
                        if (preview) {
                            Box(Modifier.padding(top = 64.dp).width(420.dp)) {
                            ToolbarWorkspacePreview(tools, ToolbarLook(), "tools",
                                colors = listOf(com.xnotes.core.model.Rgba(0, 0, 0, 255)), activeColorIndex = 0,
                                pageNumber = 1, pageCount = 1, zoomLabel = "100%", titleLabel = "First",
                                onSelectBar = {}, onSelectEntry = { _, _, _ -> }, selectedEntry = null,
                                onMoveBar = { _, _ -> }, documentTabsConfig = config,
                                onMoveDocumentTabs = { config = config.movedTo(it) })
                            }
                        } else {
                            DocumentTabsHost(documents, config, { config = config.movedTo(it) }, Modifier.fillMaxSize()) {
                                key(host) {
                                    if (neighbor) Box(Modifier.fillMaxSize()) {
                                        ToolbarHitArea(Modifier.align(Alignment.TopEnd).width(180.dp).height(50.dp)
                                            .onGloballyPositioned { neighborBounds = it.boundsInWindow() }) {}
                                    } else Column(Modifier.fillMaxSize()) {
                                        ToolbarFrame(null, layout) { ToolbarIcon(com.xnotes.ui.icons.XnotesIcons.edit, "Tool") {} }
                                        Box(Modifier.weight(1f).fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        instrumentation.waitForIdleSync()
        val deadline = android.os.SystemClock.uptimeMillis() + 5000
        while (sizes.isEmpty() && android.os.SystemClock.uptimeMillis() < deadline) Thread.sleep(25)
        assertTrue("Navigation did not render", sizes.isNotEmpty())
        Thread.sleep(150)
    }

    private fun checkCollapse(position: ToolbarPosition, floating: Boolean, preview: Boolean = false) {
        show(position, floating, preview)
        val before = sizes.last()
        sizes.clear()
        instrumentation.runOnMainSync { expanded = false }
        Thread.sleep(900)
        instrumentation.waitForIdleSync()
        val samples = synchronized(sizes) { sizes.toList() }
        assertTrue("No animated frames", samples.isNotEmpty())
        val after = samples.last()
        val vertical = position == ToolbarPosition.LEFT || position == ToolbarPosition.RIGHT
        val extent = { r: Rect -> if (vertical && !floating) r.width else if (vertical) r.height else r.width }
        if (floating || vertical) {
            assertTrue("Did not shrink: $before -> $after", extent(after) < extent(before) - 10)
            assertTrue("No intermediate extent", samples.any { extent(it) > extent(after) + 1 && extent(it) < extent(before) - 1 })
        } else {
            assertEquals("Docked navigation must keep its full edge", before.width, after.width, 2f)
        }
    }

    fun testFloatingTop() = checkCollapse(ToolbarPosition.TOP, true)
    fun testFloatingBottom() = checkCollapse(ToolbarPosition.BOTTOM, true)
    fun testFloatingLeft() = checkCollapse(ToolbarPosition.LEFT, true)
    fun testFloatingRight() = checkCollapse(ToolbarPosition.RIGHT, true)
    fun testDockedTop() = checkCollapse(ToolbarPosition.TOP, false)
    fun testDockedBottom() = checkCollapse(ToolbarPosition.BOTTOM, false)
    fun testDockedLeft() = checkCollapse(ToolbarPosition.LEFT, false)
    fun testDockedRight() = checkCollapse(ToolbarPosition.RIGHT, false)
    fun testPreviewFloatingTop() = checkCollapse(ToolbarPosition.TOP, true, preview = true)
    fun testPreviewFloatingLeft() = checkCollapse(ToolbarPosition.LEFT, true, preview = true)
    fun testPreviewFloatingRight() = checkCollapse(ToolbarPosition.RIGHT, true, preview = true)
    fun testPreviewDockedTop() = checkCollapse(ToolbarPosition.TOP, false, preview = true)
    fun testPreviewDockedBottom() = checkCollapse(ToolbarPosition.BOTTOM, false, preview = true)
    fun testPreviewDockedLeft() = checkCollapse(ToolbarPosition.LEFT, false, preview = true)
    fun testPreviewFloatingBottom() = checkCollapse(ToolbarPosition.BOTTOM, true, preview = true)
    fun testPreviewDockedRight() = checkCollapse(ToolbarPosition.RIGHT, false, preview = true)

    private fun dragNavigation(floating: Boolean, preview: Boolean) {
        show(ToolbarPosition.TOP, floating, preview)
        val before = sizes.last()
        val display = activity.resources.displayMetrics
        val scale = if (preview) minOf(1f, 420f * display.density / display.widthPixels) else 1f
        val x = before.left + minOf(16f * display.density * scale, before.width / 4)
        val y = before.center.y
        val viewportHeight = if (preview) minOf(display.widthPixels.toFloat(), 420f * display.density) * display.heightPixels / display.widthPixels else display.heightPixels.toFloat()
        val delta = (if (preview) 64f * display.density else 0f) + viewportHeight * .8f - before.center.y
        val down = android.os.SystemClock.uptimeMillis()
        fun send(action: Int, py: Float) {
            val event = android.view.MotionEvent.obtain(down, android.os.SystemClock.uptimeMillis(), action, x, py, 0)
            instrumentation.sendPointerSync(event)
            event.recycle()
        }
        send(android.view.MotionEvent.ACTION_DOWN, y)
        Thread.sleep(android.view.ViewConfiguration.getLongPressTimeout().toLong() + 150)
        for (i in 1..12) {
            send(android.view.MotionEvent.ACTION_MOVE, y + delta * i / 12)
            Thread.sleep(16)
        }
        send(android.view.MotionEvent.ACTION_UP, y + delta)
        Thread.sleep(500)
        lateinit var moved: DocumentTabsBarConfig
        instrumentation.runOnMainSync { moved = config }
        assertEquals(ToolbarAnchor.BOTTOM_CENTER, moved.anchor)
        assertEquals(ToolbarPosition.BOTTOM, moved.position)
        assertEquals(floating, moved.floating)
        assertTrue("Navigation did not move to the bottom", sizes.last().center.y > before.center.y + 100)
    }

    fun testDragFloatingNavigation() = dragNavigation(true, false)
    fun testDragDockedNavigation() = dragNavigation(false, false)
    fun testDragPreviewFloatingNavigation() = dragNavigation(true, true)
    fun testDragPreviewDockedNavigation() = dragNavigation(false, true)

    fun testDynamicAndManualSizingStopBeforeNeighborOnSameRow() {
        neighbor = true
        show(ToolbarPosition.TOP, true)
        Thread.sleep(300)
        val dynamic = sizes.last()
        assertTrue("Dynamic bar must use the available row", dynamic.width > 360 * activity.resources.displayMetrics.density)
        assertTrue("Navigation should stay beside its neighbor", dynamic.top < neighborBounds.bottom)
        assertTrue("Dynamic navigation overlaps neighbor", dynamic.right < neighborBounds.left)
        instrumentation.runOnMainSync { config = config.copy(sizing = DocumentTabsSizing.MANUAL, manualExtentDp = 240) }
        Thread.sleep(900)
        val manual = sizes.last()
        assertEquals(240f * activity.resources.displayMetrics.density, manual.width, 3f)
        assertTrue(manual.right < neighborBounds.left)
        instrumentation.runOnMainSync { config = config.copy(manualExtentDp = 4000) }
        Thread.sleep(900)
        assertTrue("Oversized manual navigation overlaps neighbor", sizes.last().right < neighborBounds.left)
        assertEquals(dynamic.width, sizes.last().width, 3f)
    }

    fun testSelectionKeepsAnimatingWhenDocumentRendererChanges() {
        show(ToolbarPosition.TOP, true)
        val positions = mutableListOf<Float>()
        val weights = mutableListOf<Float>()
        instrumentation.runOnMainSync { uri = "two"; host = !host }
        repeat(25) {
            Thread.sleep(16)
            instrumentation.runOnMainSync { positions.add(motion.position.value); weights.add(motion.weight("two")) }
        }
        assertTrue(positions.any { it > .01f && it < .99f })
        assertTrue(weights.any { it > .01f && it < .99f })
        assertTrue(positions.last() > .99f)
    }
}
