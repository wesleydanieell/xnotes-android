package com.xnotes.ui

import android.content.Intent
import android.os.SystemClock
import android.test.InstrumentationTestCase
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.xnotes.MainActivity
import com.xnotes.SplitStylusRouter
import com.xnotes.R
import com.xnotes.core.geometry.Rect
import java.lang.reflect.Proxy

/** Real Compose menu and Activity stylus routing, with a synthetic drawing surface. */
@Suppress("DEPRECATION")
class SelectionMenuStylusTest : InstrumentationTestCase() {
    private lateinit var activity: MainActivity
    private var selected by mutableStateOf(true)
    private var deletes = 0
    private var drawingDowns = 0

    override fun tearDown() {
        if (::activity.isInitialized) instrumentation.runOnMainSync {
            SplitStylusRouter.unregister("selection-menu-test")
            activity.finish()
        }
        super.tearDown()
    }

    private fun show(narrow: Boolean) {
        activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val host = Proxy.newProxyInstance(SelectionMenuHost::class.java.classLoader,
            arrayOf(SelectionMenuHost::class.java)) { _, method, _ ->
            when (method.name) {
                "getSelectionMenuRect" -> if (selected) Rect(80.0, 400.0, 180.0, 460.0) else null
                "deleteSelection" -> { deletes++; selected = false; null }
                "toString" -> "Synthetic selection"
                "hashCode" -> 1
                "equals" -> false
                else -> when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Integer.TYPE -> 0
                    java.lang.Double.TYPE -> 96.0
                    else -> null
                }
            }
        } as SelectionMenuHost
        instrumentation.runOnMainSync {
            val drawing = object : View(activity) {
                override fun onTouchEvent(event: MotionEvent): Boolean {
                    SplitStylusRouter.nativeSawEvent()
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) { drawingDowns++; selected = false }
                    return true
                }
            }
            activity.setContent {
                MaterialTheme {
                    val areas = remember { ToolbarHitAreas() }
                    val tools = remember { ToolbarHitAreas() }
                    val bounds = areas.bounds()
                    SideEffect {
                        val origin = IntArray(2)
                        val screen = IntArray(2)
                        activity.window.decorView.getLocationInWindow(origin)
                        activity.window.decorView.getLocationOnScreen(screen)
                        SplitStylusRouter.register("selection-menu-test", drawing, bounds.map {
                            android.graphics.Rect((it.left + screen[0] - origin[0]).toInt(),
                                (it.top + screen[1] - origin[1]).toInt(),
                                (it.right + screen[0] - origin[0]).toInt(),
                                (it.bottom + screen[1] - origin[1]).toInt())
                        })
                    }
                    CompositionLocalProvider(LocalToolbarHitAreas provides areas, LocalGlobalToolHitAreas provides tools) {
                        Box(Modifier.fillMaxSize()) {
                            Box(Modifier.align(Alignment.CenterEnd).then(if (narrow) Modifier.width(220.dp) else Modifier.fillMaxWidth()).fillMaxHeight()) {
                                AndroidView(factory = { drawing }, modifier = Modifier.fillMaxSize())
                                SelectionMenu(host)
                            }
                        }
                    }
                }
            }
        }
        instrumentation.waitForIdleSync()
    }

    private fun findDelete(node: AccessibilityNodeInfo?): android.graphics.Rect? {
        if (node == null) return null
        if (node.contentDescription?.toString() == activity.getString(R.string.delete)) {
            val bounds = android.graphics.Rect(); node.getBoundsInScreen(bounds)
            if (!bounds.isEmpty) return bounds
        }
        for (i in 0 until node.childCount) findDelete(node.getChild(i))?.let { return it }
        return null
    }

    private fun checkDelete(stylus: Boolean, narrow: Boolean) {
        show(narrow)
        var bounds: android.graphics.Rect? = null
        val deadline = SystemClock.uptimeMillis() + 5000
        while (bounds == null && SystemClock.uptimeMillis() < deadline) {
            bounds = findDelete(instrumentation.uiAutomation.rootInActiveWindow)
            if (bounds == null) Thread.sleep(50)
        }
        assertNotNull("Delete action must be reachable inside this pane", bounds)
        Thread.sleep(150)
        val location = IntArray(2)
        instrumentation.runOnMainSync { activity.window.decorView.getLocationOnScreen(location) }
        val down = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val property = MotionEvent.PointerProperties().apply { id = 0; toolType = if (stylus) MotionEvent.TOOL_TYPE_STYLUS else MotionEvent.TOOL_TYPE_FINGER }
            val point = MotionEvent.PointerCoords().apply {
                x = bounds!!.exactCenterX() - location[0]; y = bounds!!.exactCenterY() - location[1]; pressure = 1f; size = 1f
            }
            val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, 1, arrayOf(property), arrayOf(point),
                0, 0, 1f, 1f, 0, 0, if (stylus) InputDevice.SOURCE_STYLUS else InputDevice.SOURCE_TOUCHSCREEN, 0)
            instrumentation.runOnMainSync { activity.dispatchTouchEvent(event) }
            event.recycle()
            Thread.sleep(100)
        }
        assertEquals("Delete must execute once", 1, deletes)
        assertEquals("Selection action must never reach the drawing surface", 0, drawingDowns)
    }

    fun testStylusDeleteDoesNotReachDrawing() = checkDelete(true, false)
    fun testFingerDeleteDoesNotReachDrawing() = checkDelete(false, false)
    fun testStylusDeleteInNarrowPane() = checkDelete(true, true)
}
