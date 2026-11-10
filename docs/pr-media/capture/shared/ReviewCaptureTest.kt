package com.xnotes.ui
import com.xnotes.R
import com.xnotes.core.tools.*

class ReviewCaptureTest : BaseCapture() {
    fun testSharedAndPerPane() {
        launch(".sharedsplit")
        instrumentation.runOnMainSync {
            editor.applyHomePreferences(editor.preferences.copy(sharedSplitToolbars=true,mixedSplitToolbar=SplitToolbarInterface.NOTE))
            editor.secondaryPane().newCanvas()
            editor.focusPane(Pane.PRIMARY)
        }
        capture("split-shared-tools")
        click(activity.getString(R.string.split_settings))
        capture("split-interface-settings")
        click(activity.getString(R.string.split_per_pane))
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        capture("split-per-pane-tools")
    }
}
