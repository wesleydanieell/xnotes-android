package com.xnotes.ui
import com.xnotes.R
import com.xnotes.core.tools.*
import android.view.MotionEvent

class ReviewVideoTest : BaseCapture() {
    fun testPenGroupVideo() {
        launch(".prpen")
        instrumentation.runOnMainSync {
            editor.selectTool(Tool.PAN)
            editor.applyToolbarLayout(ToolbarLayout(listOf(ToolbarSection(listOf(ToolbarItem.UNDO,ToolbarItem.REDO,ToolbarItem.UNIFIED_PEN,ToolbarItem.HIGHLIGHTER,ToolbarItem.ERASER,ToolbarItem.SELECT,ToolbarItem.COLORS,ToolbarItem.VIEW).map { ToolbarEntry(it) }))))
        }
        settle()
        startRecording("429")
        Thread.sleep(1200)
        click(activity.getString(R.string.toolbar_unified_pen))
        click(activity.getString(R.string.toolbar_unified_pen))
        Thread.sleep(2500)
        click(activity.getString(R.string.tool_dashed))
        Thread.sleep(1800)
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        settle()
        val bounds=target(activity.getString(R.string.toolbar_unified_pen))
        val time=android.os.SystemClock.uptimeMillis()
        pointer(MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY(),time)
        Thread.sleep(900)
        Thread.sleep(2500)
        pointer(MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY(),time)
        Thread.sleep(1200)
        stopRecording()
    }
}
