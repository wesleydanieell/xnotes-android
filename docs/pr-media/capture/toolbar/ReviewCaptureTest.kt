package com.xnotes.ui
import com.xnotes.R
import com.xnotes.core.tools.*

class ReviewCaptureTest : BaseCapture() {
    fun testToolbarWorkspaceAndEditor() {
        launch(".prtoolbar")
        instrumentation.runOnMainSync {
            fun layout(vararg items: ToolbarItem)=ToolbarLayout(listOf(ToolbarSection(items.map { ToolbarEntry(it) })))
            val primary=ToolbarBar("primary","Main tools",layout(ToolbarItem.TITLE,ToolbarItem.UNDO,ToolbarItem.REDO,ToolbarItem.PEN,ToolbarItem.HIGHLIGHTER,ToolbarItem.ERASER,ToolbarItem.SELECT,ToolbarItem.LASSO,ToolbarItem.VIEW,ToolbarItem.PAGE_NAV))
            val colors=ToolbarBar("colors","Ink palette",layout(ToolbarItem.COLORS),geometry=ToolbarGeometry(anchor=ToolbarAnchor.BOTTOM_CENTER,floating=true))
            val side=ToolbarBar("side","Quick tools",layout(ToolbarItem.PAN,ToolbarItem.TEXT,ToolbarItem.SHAPE),geometry=ToolbarGeometry(anchor=ToolbarAnchor.CENTER_LEFT,floating=true))
            editor.applyToolbarWorkspaces(editor.toolbarWorkspaces.copy(note=ToolbarWorkspace(listOf(primary,colors,side))))
        }
        capture("toolbar-workspace")
        click(activity.getString(R.string.toolbar_view))
        click(activity.getString(R.string.configure_toolbars))
        capture("toolbar-configurator")
        click("Elementos")
        capture("toolbar-elements")
    }
}
