package com.xnotes.ui
import com.xnotes.R
import com.xnotes.core.tools.*

class ReviewVideoTest : BaseCapture() {
    private fun dismissMenu() {
        val metrics=activity.resources.displayMetrics
        val x=metrics.widthPixels * .3f
        val y=metrics.heightPixels * .6f
        val time=android.os.SystemClock.uptimeMillis()
        pointer(android.view.MotionEvent.ACTION_DOWN,x,y,time)
        pointer(android.view.MotionEvent.ACTION_UP,x,y,time)
        settle()
    }
    fun testSharedToolsVideo() {
        launch(".sharedsplit")
        instrumentation.runOnMainSync {
            editor.applyHomePreferences(editor.preferences.copy(sharedSplitToolbars=true,mixedSplitToolbar=SplitToolbarInterface.NOTE))
            editor.secondaryPane().newCanvas()
            editor.focusPane(Pane.PRIMARY)
        }
        startRecording("430")
        Thread.sleep(1800)
        click(activity.getString(R.string.split_settings))
        Thread.sleep(2200)
        click(activity.getString(R.string.split_interface_canvas))
        assertEquals(SplitToolbarInterface.CANVAS,editor.preferences.mixedSplitToolbar)
        Thread.sleep(1800)
        dismissMenu()
        settle()
        Thread.sleep(1800)
        click(activity.getString(R.string.split_settings))
        click(activity.getString(R.string.split_per_pane))
        assertFalse(editor.preferences.sharedSplitToolbars)
        dismissMenu()
        Thread.sleep(2500)
        stopRecording()
    }
}
