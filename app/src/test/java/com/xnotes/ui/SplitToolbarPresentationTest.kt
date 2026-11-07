package com.xnotes.ui

import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.*
import com.xnotes.settings.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SplitToolbarPresentationTest {
    private fun workspace(canvas: Boolean) = ToolbarWorkspace(listOf(ToolbarBar("primary", "Main",
        (if (canvas) ToolbarLayout.CANVAS_DEFAULT else ToolbarLayout.DEFAULT).setEntrySize(0, 0, 72),
        geometry = ToolbarGeometry(widthDp = 1000, elementSizeDp = 64, paddingDp = 12, spacingDp = 10))))

    @Test fun narrowNoteAndCanvasPanesAdaptWithoutMutatingTheirSavedProfiles() {
        for (canvas in listOf(false, true)) {
            val source = workspace(canvas)
            val look = ToolbarLook(size = ToolbarSize.COMFORTABLE)
            val small = splitToolbarPresentation(source, look, 300f, 800f, true)
            assertEquals(ToolbarSize.COMPACT, small.look.size)
            assertEquals(36, small.workspace.bars[0].geometry.elementSizeDp)
            assertEquals(36, small.workspace.bars[0].layout.sections[0].entries[0].sizeDp)
            assertEquals(300, small.workspace.bars[0].geometry.widthDp)
            assertEquals(1000, source.bars[0].geometry.widthDp)
            assertEquals(64, source.bars[0].geometry.elementSizeDp)
            assertEquals(72, source.bars[0].layout.sections[0].entries[0].sizeDp)
            assertEquals(source, splitToolbarPresentation(source, look, 1000f, 800f, true).workspace)
            assertEquals(source, splitToolbarPresentation(source, look, 300f, 800f, false).workspace)
        }
    }
    @Test fun stackedPanesAlsoAdaptAndThePreferenceRoundTrips() {
        val source = workspace(false)
        assertEquals(ToolbarSize.COMPACT, splitToolbarPresentation(source, ToolbarLook(), 1200f, 350f, true).look.size)
        for (enabled in listOf(false, true)) {
            val prefs = Preferences(adaptiveSplitToolbars = enabled)
            assertEquals(prefs, Preferences.fromJson(JSONObject(prefs.toJson().toString())))
        }
    }
    @Test fun oversizedManualDockLengthsClampOnlyForRuntimeViewportAdaptation() {
        val docked = workspace(false).let { it.copy(bars = it.bars.map { b -> b.copy(geometry = b.geometry.copy(dockEdge = ToolbarDockEdge.BOTTOM)) }) }
        assertTrue(toolbarDockPlan(docked, ToolbarLook(), 300.dp, 600.dp).rejected.isNotEmpty())
        val adapted = toolbarDockPlan(docked, ToolbarLook(), 300.dp, 600.dp, fitToViewport = true)
        val bar = adapted.placement("primary")!!
        assertTrue(adapted.rejected.isEmpty())
        assertTrue(bar.x >= 0 && bar.x + bar.width <= 300)
        assertEquals(1000, docked.bars[0].geometry.widthDp)
    }
    @Test fun sideRailsDoNotRetainAFullWindowCrossDimensionInASmallPane() {
        val source = workspace(false)
        for (position in ToolbarPosition.entries) {
            val look = ToolbarLook(position = position, floating = false)
            val adapted = splitToolbarPresentation(source, look, 260f, 360f, true)
            val g = adapted.workspace.bars.single().geometry
            assertTrue(g.widthDp!! <= 260)
            assertEquals(1000, source.bars.single().geometry.widthDp)
        }
    }

}
