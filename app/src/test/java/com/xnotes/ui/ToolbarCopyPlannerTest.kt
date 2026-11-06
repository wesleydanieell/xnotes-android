package com.xnotes.ui

import androidx.compose.ui.unit.dp
import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarContext
import com.xnotes.core.tools.ToolbarDockEdge
import com.xnotes.core.tools.ToolbarEntry
import com.xnotes.core.tools.ToolbarGeometry
import com.xnotes.core.tools.ToolbarItem
import com.xnotes.core.tools.ToolbarLayout
import com.xnotes.core.tools.ToolbarSection
import com.xnotes.core.tools.ToolbarWorkspace
import com.xnotes.core.tools.prepareToolbarTransfer
import com.xnotes.settings.ToolbarLook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolbarCopyPlannerTest {
    private fun bar(id: String, item: ToolbarItem = ToolbarItem.PEN, geometry: ToolbarGeometry = ToolbarGeometry()) =
        ToolbarBar(id, id, ToolbarLayout(listOf(ToolbarSection(listOf(ToolbarEntry(item))))), geometry = geometry)

    @Test fun primaryCopyUsesGlobalDockAndKeepsSourceUntouched() {
        val source = bar("source", geometry = ToolbarGeometry(widthDp = 180, spacingDp = 5))
        val destination = ToolbarWorkspace(listOf(bar("dest")))
        val draft = prepareToolbarTransfer(source, destination, ToolbarContext.CANVAS)
        val placed = planCopiedToolbar(source, true, destination, draft, ToolbarLook(), 1200.dp, 800.dp, 5)
        assertNotNull(placed)
        assertEquals(ToolbarDockEdge.TOP, placed!!.bar.geometry.dockEdge)
        assertFalse(placed.bar.geometry.floating)
        assertEquals(180, placed.bar.geometry.widthDp)
        assertEquals(5, placed.bar.geometry.spacingDp)
        assertEquals(null, source.geometry.dockEdge)
        assertEquals(1, destination.bars.size)
        assertEquals(draft.bar.id, placed.bar.id)
    }

    @Test fun secondaryCopyPreservesItsFloatingAnchor() {
        val geometry = ToolbarGeometry(anchor = ToolbarAnchor.BOTTOM_RIGHT, floating = true, widthDp = 150)
        val source = bar("source", geometry = geometry)
        val destination = ToolbarWorkspace(listOf(bar("dest")))
        val draft = prepareToolbarTransfer(source, destination, ToolbarContext.NOTE)
        val placed = planCopiedToolbar(source, false, destination, draft, ToolbarLook(), 1200.dp, 800.dp, 4)
        assertNotNull(placed)
        assertEquals(geometry, placed!!.bar.geometry)
        assertTrue(placed.omitted.isEmpty())
    }

    @Test fun emptyTransferNeverProducesPlacement() {
        val source = bar("source", ToolbarItem.PAGE_NAV)
        val destination = ToolbarWorkspace(listOf(bar("dest")))
        val draft = prepareToolbarTransfer(source, destination, ToolbarContext.CANVAS)
        assertEquals(0, draft.retainedCount)
        assertNull(planCopiedToolbar(source, true, destination, draft, ToolbarLook(), 1200.dp, 800.dp, 5))
    }
}
