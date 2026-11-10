package com.xnotes.ui
import com.xnotes.R
import com.xnotes.core.tools.*

class ReviewVideoTest : BaseCapture() {
    fun testToolbarWorkspaceVideo() {
        launch(".prtoolbar")
        instrumentation.runOnMainSync {
            fun layout(vararg items: ToolbarItem)=ToolbarLayout(listOf(ToolbarSection(items.map { ToolbarEntry(it) })))
            val primary=ToolbarBar("primary","Main tools",layout(ToolbarItem.TITLE,ToolbarItem.UNDO,ToolbarItem.REDO,ToolbarItem.PEN,ToolbarItem.HIGHLIGHTER,ToolbarItem.ERASER,ToolbarItem.SELECT,ToolbarItem.LASSO,ToolbarItem.VIEW,ToolbarItem.PAGE_NAV))
            val colors=ToolbarBar("colors","Ink palette",layout(ToolbarItem.COLORS),geometry=ToolbarGeometry(anchor=ToolbarAnchor.BOTTOM_CENTER,floating=true))
            val side=ToolbarBar("side","Quick tools",layout(ToolbarItem.PAN,ToolbarItem.TEXT,ToolbarItem.SHAPE),geometry=ToolbarGeometry(anchor=ToolbarAnchor.CENTER_LEFT,floating=true))
            editor.applyToolbarWorkspaces(editor.toolbarWorkspaces.copy(note=ToolbarWorkspace(listOf(primary,colors,side))))
        }
        startRecording("431")
        Thread.sleep(1800)
        click(activity.getString(R.string.toolbar_view))
        click(activity.getString(R.string.configure_toolbars))
        Thread.sleep(2500)
        click("Elementos")
        Thread.sleep(2500)
        click("Barras")
        click(activity.getString(R.string.toolbar_floating))
        Thread.sleep(2500)
        stopRecording()
    }
}
