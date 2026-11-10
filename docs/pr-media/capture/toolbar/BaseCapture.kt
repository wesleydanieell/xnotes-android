package com.xnotes.ui

import android.content.Intent
import android.graphics.Rect
import android.graphics.Bitmap
import android.test.InstrumentationTestCase
import android.view.MotionEvent
import android.view.InputDevice
import android.view.accessibility.AccessibilityNodeInfo
import com.xnotes.MainActivity

@Suppress("DEPRECATION")
open class BaseCapture : InstrumentationTestCase() {
    protected lateinit var activity: MainActivity
    protected lateinit var editor: Editor
    protected fun launch(suffix: String) {
        assertTrue("Only use isolated review packages", instrumentation.targetContext.packageName.endsWith(suffix))
        activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        instrumentation.runOnMainSync {
            editor = MainActivity::class.java.getDeclaredField("editor").apply { isAccessible = true }.get(activity) as Editor
            editor.newNote()
        }
        settle()
    }
    protected fun settle() { instrumentation.waitForIdleSync(); Thread.sleep(650) }
    protected fun find(label: String): List<Rect> {
        val found=mutableListOf<Rect>()
        fun visit(node: AccessibilityNodeInfo?) {
            if(node==null)return
            if(node.contentDescription?.toString()==label || node.text?.toString()==label) {
                val bounds=Rect();node.getBoundsInScreen(bounds);if(!bounds.isEmpty)found.add(bounds)
            }
            for(i in 0 until node.childCount)visit(node.getChild(i))
        }
        visit(instrumentation.uiAutomation.rootInActiveWindow)
        return found
    }
    protected fun target(label: String): Rect {
        repeat(30) { val found=find(label);if(found.isNotEmpty())return found.first();Thread.sleep(100) }
        fail("Missing UI control: $label");return Rect()
    }
    protected fun pointer(action: Int,x: Float,y: Float,time: Long) {
        val event=MotionEvent.obtain(time,android.os.SystemClock.uptimeMillis(),action,x,y,0)
        event.source=InputDevice.SOURCE_TOUCHSCREEN;instrumentation.sendPointerSync(event);event.recycle()
    }
    protected fun click(label: String) {
        val bounds=target(label);val time=android.os.SystemClock.uptimeMillis()
        pointer(MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY(),time)
        pointer(MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY(),time);settle()
    }
    protected fun capture(name: String) {
        settle()
        val screenshot=instrumentation.uiAutomation.takeScreenshot()
        java.io.File(activity.getExternalFilesDir(null),name+".png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) }
        screenshot.recycle()
    }
    override fun tearDown() {
        if(::activity.isInitialized)instrumentation.runOnMainSync { activity.finish() }
        super.tearDown()
    }
}
