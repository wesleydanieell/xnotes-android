package com.xnotes.core.tools

import org.junit.Assert.*
import org.junit.Test

class LegacyStandaloneToolbarTest {
    @Test fun newDefaultShowsUpstreamGroupedPensAndSelectionOnly() {
        val note = ToolbarLayout.DEFAULT.sections.flatMap { it.visibleEntries }.map { it.item }
        assertTrue(ToolbarItem.PEN in note && ToolbarItem.SELECT in note)
        for (legacy in listOf(ToolbarItem.DASHED, ToolbarItem.CALLIGRAPHY, ToolbarItem.SPEED, ToolbarItem.TAPER, ToolbarItem.LASSO)) {
            assertFalse(legacy in note)
        }
    }
    @Test fun explicitLegacyToolsRemainVisibleOnRead() {
        val layout = ToolbarLayout.fromRaw(listOf(listOf("dashed" to true, "lasso" to true)))
        val visible = layout.sections.flatMap { it.visibleEntries }.map { it.item }
        assertTrue(ToolbarItem.DASHED in visible)
        assertTrue(ToolbarItem.LASSO in visible)
    }
}
