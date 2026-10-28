package com.xnotes.ui

import com.xnotes.core.tools.ToolbarAnchor
import com.xnotes.core.tools.ToolbarBar
import com.xnotes.core.tools.ToolbarGeometry
import com.xnotes.core.tools.ToolbarLayout
import com.xnotes.core.tools.ToolbarScale
import com.xnotes.settings.ToolbarLook
import com.xnotes.settings.ToolbarPosition
import com.xnotes.settings.ToolbarSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ToolbarLookResolverTest {
    private val base = ToolbarLook(
        position = ToolbarPosition.RIGHT,
        size = ToolbarSize.COMFORTABLE,
        floating = true,
    )
    private val bar = ToolbarBar("primary", "Primary", ToolbarLayout.DEFAULT)

    @Test
    fun missingPrimaryKeepsLegacyLook() {
        assertEquals(base, toolbarLookFor(base, null))
    }

    @Test
    fun canvasGeometryDoesNotInheritNoteDockOrSize() {
        val projected = toolbarLookFor(
            base,
            bar.copy(geometry = ToolbarGeometry(
                anchor = ToolbarAnchor.CENTER_LEFT,
                scale = ToolbarScale.COMPACT,
                floating = false,
            )),
        )
        assertEquals(ToolbarPosition.LEFT, projected.position)
        assertEquals(ToolbarSize.COMPACT, projected.size)
        assertFalse(projected.floating)
    }

    @Test
    fun cornerFloatingGeometryUsesTopOrBottomEdgeForPlacement() {
        assertEquals(
            ToolbarPosition.TOP,
            toolbarLookFor(base, bar.copy(geometry = ToolbarGeometry(ToolbarAnchor.TOP_RIGHT, floating = true))).position,
        )
        assertEquals(
            ToolbarPosition.BOTTOM,
            toolbarLookFor(base, bar.copy(geometry = ToolbarGeometry(ToolbarAnchor.BOTTOM_LEFT, floating = true))).position,
        )
    }
}
